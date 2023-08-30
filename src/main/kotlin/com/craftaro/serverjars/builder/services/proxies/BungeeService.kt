package com.craftaro.serverjars.builder.services.proxies

import com.craftaro.serverjars.builder.App
import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.models.SoftwareFile
import com.craftaro.serverjars.builder.services.utils.Crypto
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URL

object BungeeService: SoftwareBuilder() {

    override val category: String = "proxies"

    override val type: String = "bungee"

    private val builds = JsonParser.parseString(URL("https://ci.md-5.net/job/Bungeecord/api/json?tree=allBuilds[number,mavenArtifacts[moduleRecords[pomArtifact[version]]{0}]]").readText()).asJsonObject
        .getAsJsonArray("allBuilds")
        .filter { it.asJsonObject.let { json -> json.has("mavenArtifacts") && json.get("mavenArtifacts") != null && !json.get("mavenArtifacts").isJsonNull } }
        .map { it.asJsonObject }
        .map { json ->
            val number = json.get("number").asInt
            val version = json.getAsJsonObject("mavenArtifacts").getAsJsonArray("moduleRecords").first().asJsonObject.getAsJsonObject("pomArtifact").get("version").asString

            number to version.split("-").first()
        }
        .distinctBy { it.second }
        .toMap()



    override fun availableVersions(): List<String> =
        builds.values.toList()

    override fun build(version: String) {
        val buildNumber = builds.filter { it.value == version }.keys.first()
        val manifest = JsonParser.parseString(URL("https://ci.md-5.net/job/Bungeecord/$buildNumber/api/json?tree=artifacts[fileName,relativePath],mavenArtifacts[moduleRecords[mainArtifact[fileName,md5sum]]]").readText()).asJsonObject
        // Find BungeeCord.jar
        val artifact = manifest.getAsJsonArray("artifacts").find { it.asJsonObject.get("fileName").asString == "BungeeCord.jar" } ?: run {
            println("No BungeeCord.jar artifact found for $version")
            return
        }
        // Now find md5 sum in mavenArtifacts using the same filename
        val md5sum = manifest.getAsJsonObject("mavenArtifacts").getAsJsonArray("moduleRecords")
            .filter { it.asJsonObject.has("mainArtifact") && it.asJsonObject.get("mainArtifact") != null && !it.asJsonObject.get("mainArtifact").isJsonNull }
            .map { it.asJsonObject.getAsJsonObject("mainArtifact") }
            .find { it.get("fileName").asString == artifact.asJsonObject.get("fileName").asString }?.get("md5sum")?.asString ?: run {
                println("No md5sum found for $version")
                return
            }
        // Download
        val url = "https://ci.md-5.net/job/Bungeecord/$buildNumber/artifact/${artifact.asJsonObject.get("relativePath").asString}"
        val hash = "md5:${md5sum}"

        if(isInDatabase(version, hash)) {
            println("Skipping $version, already in database")
            return
        }

        println("Building BungeeCord $version build $buildNumber (snapshot)...")

        saveToDatabase(SoftwareFile(
            version = version,
            stability = "snapshot",
            hash = hash,
            download = "https://cdn.craftaro.com/$baseDirectory/$version/bungee-$version.jar",
            meta = JsonObject().apply {
                addProperty("buildNumber", buildNumber)
                addProperty("origin", url)
            }
        ))

        println("Uploading Bungee $version build $buildNumber (snapshot) to Storage...")
        val bytes = URL(url).readBytes()
        App.storage.write(
            path = "$baseDirectory/$version/bungee-$version.jar",
            contents = bytes,
            permission = "public-read",
            checksum = Crypto.toString(Crypto.sha256(bytes))
        )
    }
}