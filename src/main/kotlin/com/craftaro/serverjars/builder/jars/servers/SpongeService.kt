package com.craftaro.serverjars.builder.jars.servers

import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.models.SoftwareFile
import com.craftaro.serverjars.builder.utils.CachingService
import com.craftaro.serverjars.builder.utils.Crypto
import com.craftaro.serverjars.builder.utils.Storage
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.FileNotFoundException
import java.net.URL

object SpongeService : SoftwareBuilder() {
    override val category: String = "servers"
    override val type: String = "sponge"

    override fun availableVersions(): List<String> =
        CachingService.rememberMinutes("$baseDirectory/versions", 5) {
            JsonParser.parseString(URL("https://dl-api-new.spongepowered.org/api/v2/groups/org.spongepowered/artifacts/spongevanilla").readText()).asJsonObject
        }.getAsJsonObject("tags")
            .getAsJsonArray("minecraft")
            .map { it.asString }

    override fun build(version: String) {
        val latestManifest = JsonParser.parseString(
            try {
                URL("https://dl-api-new.spongepowered.org/api/v2/groups/org.spongepowered/artifacts/spongevanilla/versions?offset=0&limit=1&recommended=true&tags=,minecraft:$version")
                    .readText()
            } catch (e: FileNotFoundException) {
                URL("https://dl-api-new.spongepowered.org/api/v2/groups/org.spongepowered/artifacts/spongevanilla/versions?offset=0&limit=1&tags=,minecraft:$version")
                    .readText()
            }
        ).asJsonObject // Try to find a recommended one or just the latest one
        val spongeVersion = latestManifest.getAsJsonObject("artifacts").keySet().first()
        val versionManifest =
            JsonParser.parseString(URL("https://dl-api-new.spongepowered.org/api/v2/groups/org.spongepowered/artifacts/spongevanilla/versions/$spongeVersion").readText()).asJsonObject
        val stability = if (versionManifest.get("recommended").asBoolean) "stable" else "snapshot"
        val asset = versionManifest.getAsJsonArray("assets").find {
            (it.asJsonObject.get("classifier").asString == "universal" || it.asJsonObject.get("classifier").asString == "") && it.asJsonObject.get(
                "extension"
            ).asString == "jar"
        } ?: run {
            println("No jar asset found for $version ($spongeVersion)")
            return
        }

        val url = asset.asJsonObject.get("downloadUrl").asString
        val hash = "sha1:${asset.asJsonObject.get("sha1").asString}"

        if (isInDatabase(version = version, hash = hash)) {
            println("Sponge $version ($stability) already built")
            return
        }

        val apiVersion = versionManifest.getAsJsonObject("tags").get("api").asString

        println(
            "Building Sponge $version build ${
                spongeVersion.substringAfter("$version-").substringAfter("$apiVersion-")
            } ($stability)..."
        )
        saveToDatabase(SoftwareFile(
            version = version,
            stability = stability,
            hash = hash,
            download = "https://cdn.craftaro.com/$baseDirectory/$version/sponge-$version.jar",
            meta = JsonObject().apply {
                addProperty("spongeVersion", spongeVersion)
                addProperty("spongeApiVersion", apiVersion)
                addProperty("origin", url)
            }
        ))

        println(
            "Uploading Sponge $version build ${
                spongeVersion.substringAfter("$version-").substringAfter("$apiVersion-")
            } ($stability) to Storage..."
        )
        val bytes = URL(url).readBytes()
        Storage.write(
            path = "$baseDirectory/$version/sponge-$version.jar",
            contents = bytes,
            permission = "public-read",
            checksum = Crypto.toString(Crypto.sha256(bytes))
        )
    }

}