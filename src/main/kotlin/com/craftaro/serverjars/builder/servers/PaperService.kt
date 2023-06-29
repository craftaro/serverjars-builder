package com.craftaro.serverjars.builder.servers

import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.net.Url
import com.craftaro.serverjars.builder.App
import com.craftaro.serverjars.builder.services.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import java.net.URL

data class Paper(val version: String, val stability: String, val hash: String, val download: String, val meta: JsonObject)

object PaperService {

    private val s3client = S3Client {
        region = App.env["S3_REGION"]
        credentialsProvider = CraftaroS3CredentialsProvider()
        endpointUrl = Url.parse(App.env["S3_ENDPOINT"] ?: "")
    }

    private val db = mutableListOf<Paper>()

    fun build(minecraftVersion: String) {
        if(db.isEmpty()) {
            val data = runBlocking { s3client.readByteArrayFromS3("serverjars/servers/paper/meta.json") } ?: return
            JsonParser.parseString(String(data)).asJsonArray.reversed().forEach { value ->
                value.asJsonObject.apply {
                    db.add(Paper(
                        version = get("version").asString,
                        stability = get("stability").asString,
                        hash = get("hash").asString,
                        download = get("download").asString,
                        meta = get("meta").asJsonObject
                    ))
                }
            }
        }
        println("Building paper $minecraftVersion...")
        val asset = MojangService.assetVersion(version = minecraftVersion)
        val manifest = versionManifest(asset = asset)
        if(manifest.has("error")){
            println("Error: ${manifest.get("error").asString}")
            return
        }

        val versions = mutableListOf<JsonObject>()
        val builds = manifest.getAsJsonArray("builds")
        if(builds.isEmpty){
            println("Error: No builds found for $minecraftVersion")
            return
        }

        manifest.getAsJsonArray("versions").forEach { v ->
            val latestBuild = builds.filter { it.asJsonObject.get("version").asString == v.asString }.maxByOrNull { it.asJsonObject.get("build").asInt }?.asJsonObject
            if(latestBuild != null) {
                versions.add(JsonObject().apply {
                    addProperty("version", v.asString)
                    addProperty("build", latestBuild.get("build").asInt)
                    addProperty("stability", latestBuild.get("channel").asString)
                    addProperty("download", "https://api.papermc.io/v2/projects/paper/versions/${v.asString}/builds/${latestBuild.get("build").asInt}/downloads/${latestBuild.get("downloads").asJsonObject.get("application").asJsonObject.get("name").asString}")
                    addProperty("hash", latestBuild.get("downloads").asJsonObject.get("application").asJsonObject.get("sha256").asString)
                })
            }
        }

        versions.forEach { versionManifest ->
            val version = versionManifest.get("version").asString
            val build = versionManifest.get("build").asInt
            val stability = versionManifest.get("stability").asString
            val download = versionManifest.get("download").asString
            val hash = versionManifest.get("hash").asString

            val index = db.indexOfFirst { it.version == version }
            if(index != -1 && db[index].hash == hash){
                println("Paper $version build $build ($stability) already built")
                return@forEach
            }

            println("Downloading paper $version build $build ($stability)...")
            val jar = URL(download).readBytes()
            println("Downloaded paper $version build $build ($stability)")

            println("Building paper $version build $build ($stability)...")
            val paper = Paper(version = version, stability = stability, hash = hash, download = "https://cdn.craftaro.com/serverjars/servers/paper/$version/paper-$version.jar", meta = JsonObject().apply {
                addProperty("build", build)
                addProperty("paperDownload", download)
            })
            db.add(paper)

            println("Uploading paper $version build $build ($stability) to DigitalOcean...")

            runBlocking {
                s3client.uploadByteArrayToS3(
                    data = jar,
                    path = "serverjars/servers/paper/$version/paper-$version.jar",
                    checksumSha256 = hash,
                    acl = "public-read"
                )
            }

            println("Built paper $version build $build ($stability)")
        }

        saveDatabase()
    }

    private fun saveDatabase() {
        val json = JsonArray()
        db.reversed().forEach {
            json.add(JsonObject().apply {
                addProperty("download", it.download)
                addProperty("version", it.version)
                addProperty("stability", it.stability)
                addProperty("hash", it.hash)
                add("meta", it.meta)
            })
        }

        runBlocking { s3client.uploadByteArrayToS3(data = json.toString().toByteArray(), path = "serverjars/servers/paper/meta.json", acl = "public-read") }

        println("Saved paper database")
    }


    private fun versionManifest(asset: String): JsonObject = CachingService.rememberMinutes("paper-manifest-$asset", 5) {
        JsonParser.parseString(URL("https://api.papermc.io/v2/projects/paper/version_group/$asset/builds").readText()).asJsonObject
    }
}