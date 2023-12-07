package com.craftaro.serverjars.builder.jars.servers

import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.models.SoftwareFile
import com.craftaro.serverjars.builder.utils.CachingService
import com.craftaro.serverjars.builder.utils.Storage
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URL
import java.nio.charset.Charset

object PaperService : SoftwareBuilder() {

    override val category: String = "servers"
    override val type: String = "paper"

    override fun availableVersions(): List<String> =
        CachingService.rememberMinutes("$baseDirectory/versions", 5) {
            val data = URL("https://api.papermc.io/v2/projects/paper/").readText(Charset.defaultCharset())
            JsonParser.parseString(data).asJsonObject
        }.getAsJsonArray("versions").reversed().map { it.asString }

    override fun build(version: String) {
        val manifest = JsonParser.parseString(URL("https://api.papermc.io/v2/projects/paper/versions/$version/builds").readText()).asJsonObject
        val latestBuild = manifest.getAsJsonArray("builds").last().asJsonObject ?: run {
            println("Error: No builds found for $version")
            return
        }

        val build = latestBuild.get("build").asInt
        val stability = latestBuild.get("channel").asString.let { if(it == "default") "stable" else it }
        val download = "https://api.papermc.io/v2/projects/paper/versions/${version}/builds/${latestBuild.get("build").asInt}/downloads/${latestBuild.get("downloads").asJsonObject.get("application").asJsonObject.get("name").asString}"
        val hash = "sha256:${latestBuild.get("downloads").asJsonObject.get("application").asJsonObject.get("sha256").asString}"

        if (isInDatabase(version = version, hash = hash)) {
            println("Paper $version build $build ($stability) already built")
            return
        }

        println("Building paper $version build $build ($stability)...")
        saveToDatabase(SoftwareFile(
            version = version,
            stability = stability,
            hash = hash,
            download = "https://cdn.craftaro.com/$baseDirectory/$version/paper-$version.jar",
            meta = JsonObject().apply {
                addProperty("build", build)
                addProperty("origin", download)
            }
        ))

        println("Uploading paper $version build $build ($stability) to Storage...")
        Storage.write(
            path = "$baseDirectory/$version/paper-$version.jar",
            contents = URL(download).readBytes(),
            permission = "public-read",
            checksum = hash.substringAfter("sha256:"),
        )
    }
}