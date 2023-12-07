package com.craftaro.serverjars.builder.jars.servers

import com.craftaro.serverjars.builder.App
import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.models.SoftwareFile
import com.craftaro.serverjars.builder.utils.CachingService
import com.craftaro.serverjars.builder.utils.Crypto
import com.craftaro.serverjars.builder.utils.Storage
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URL

object PurpurService: SoftwareBuilder() {

    override val category: String = "servers"
    override val type: String = "purpur"

    override fun availableVersions(): List<String> =
        CachingService.rememberMinutes("purpur-versions", 5) {
            JsonParser.parseString(URL("https://api.purpurmc.org/v2/purpur/").readText()).asJsonObject
        }.getAsJsonArray("versions").reversed().map { it.asString }

    override fun build(version: String) {
        val manifest = JsonParser.parseString(URL("https://api.purpurmc.org/v2/purpur/$version/latest").readText()).asJsonObject

        val build = manifest.get("build").asInt
        val download = "https://api.purpurmc.org/v2/purpur/$version/$build/download"
        val hash = "md5:${manifest.get("md5").asString}"
        val stability = "stable"

        if (isInDatabase(version = version, hash = hash)) {
            println("Purpur $version build $build ($stability) already built")
            return
        }

        println("Downloading purpur $version build $build ($stability)...")
        val jar = URL(download).readBytes()
        println("Downloaded purpur $version build $build ($stability)")
        println("Building purpur $version build $build ($stability)...")
        saveToDatabase(SoftwareFile(
            version = version,
            stability = stability,
            hash = hash,
            download = "https://cdn.craftaro.com/$baseDirectory/$version/purpur-$version.jar",
            meta = JsonObject().apply {
                addProperty("build", build)
                addProperty("origin", download)
            }
        ))

        println("Uploading purpur $version build $build ($stability) to Storage...")
        Storage.write(
            path = "$baseDirectory/$version/purpur-$version.jar",
            contents = jar,
            permission = "public-read",
            checksum = Crypto.toString(Crypto.sha256(jar))
        )
    }
}