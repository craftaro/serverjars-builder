package com.craftaro.serverjars.builder.jars.servers

import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.utils.CachingService
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.FileNotFoundException
import java.net.URL

object SpongeService : SoftwareBuilder() {
    override val type: String = "servers"
    override val category: String = "sponge"

    override fun availableVersions(): List<String> =
        CachingService.rememberMinutes("$baseDirectory/versions", 5) {
            JsonParser.parseString(URL("https://dl-api-new.spongepowered.org/api/v2/groups/org.spongepowered/artifacts/spongevanilla").readText()).asJsonObject
        }.getAsJsonObject("tags")
            .getAsJsonArray("minecraft")
            .map { it.asString }

    override fun getMeta(version: String): JsonObject = CachingService.rememberMinutes("$baseDirectory/$version/meta", 5) {
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
            JsonObject()
        }

        val url = asset.asJsonObject.get("downloadUrl").asString
        val hash = "sha1:${asset.asJsonObject.get("sha1").asString}"

        JsonObject().apply {
            addProperty("spongeVersion", spongeVersion)
            addProperty("spongeApiVersion", versionManifest.getAsJsonObject("tags").get("api").asString)
            addProperty("origin", url)
            addProperty("stability", stability)
            addProperty("hash", hash)
        }
    }

    override fun getHash(version: String): String? = getMeta(version)["hash"]?.asString

    override fun getDownload(version: String): String? = getMeta(version)["origin"]?.asString

    override fun getStability(version: String): String = getMeta(version)["stability"]?.asString ?: "unknown"

}