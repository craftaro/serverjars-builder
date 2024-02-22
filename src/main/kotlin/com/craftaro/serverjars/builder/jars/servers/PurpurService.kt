package com.craftaro.serverjars.builder.jars.servers

import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.utils.CachingService
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URL

object PurpurService: SoftwareBuilder() {

    override val type: String = "servers"
    override val category: String = "purpur"

    override fun availableVersions(): List<String> =
        CachingService.rememberMinutes("purpur-versions", 5) {
            JsonParser.parseString(URL("https://api.purpurmc.org/v2/purpur/").readText()).asJsonObject
        }.getAsJsonArray("versions").reversed().map { it.asString }

    override fun getMeta(version: String): JsonObject = CachingService.rememberMinutes("$baseDirectory/$version/meta", 5) {
        val manifest = JsonParser.parseString(URL("https://api.purpurmc.org/v2/purpur/$version/latest").readText()).asJsonObject

        val build = manifest.get("build").asInt
        val download = "https://api.purpurmc.org/v2/purpur/$version/$build/download"
        val hash = "md5:${manifest.get("md5").asString}"
        val stability = "stable"

        JsonObject().apply {
            addProperty("build", build)
            addProperty("origin", download)
            addProperty("hash", hash)
            addProperty("stability", stability)
        }
    }

    override fun getHash(version: String): String? = getMeta(version).let { if (it.has("hash")) it.get("hash").asString else null }

    override fun getDownload(version: String): String? = getMeta(version).let { if (it.has("origin")) it.get("origin").asString else null }

    override fun getStability(version: String): String = getMeta(version).let { if (it.has("stability")) it.get("stability").asString else "unknown" }

}