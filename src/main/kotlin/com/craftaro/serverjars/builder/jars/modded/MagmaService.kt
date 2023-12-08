package com.craftaro.serverjars.builder.jars.modded

import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.utils.CachingService
import com.craftaro.serverjars.builder.utils.Crypto
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URL

object MagmaService: SoftwareBuilder() {
    override val category: String = "modded"
    override val type: String = "magma"

    override fun availableVersions(): List<String> = CachingService.rememberMinutes("$baseDirectory/versions", 5) {
        JsonObject().apply {
            add("versions", JsonParser.parseString(URL("https://api.magmafoundation.org/api/v2/allVersions").readText()).asJsonArray)
        }
    }.getAsJsonArray("versions").map { it.asString }.filter {
        getMeta(it).has("origin")
    }

    override fun getMeta(version: String): JsonObject = CachingService.rememberMinutes("$baseDirectory/$version/meta", 5) {
        try {
            val builds = JsonParser.parseString(URL("https://api.magmafoundation.org/api/v2/$version").readText()).asJsonArray
            if(builds.isEmpty) {
                JsonObject()
            }

            val meta = builds.first { !(it.asJsonObject["archived"]?.asBoolean ?: false) }.asJsonObject

            if(!meta.has("link")) {
                JsonObject()
            }

            JsonObject().apply {
                addProperty("origin", meta["link"].asString)
                addProperty("created_at", meta["created_at"].asString)
            }
        } catch (e: Exception) {
            JsonObject()
        }
    }

    override fun getHash(version: String): String? = getMeta(version)["origin"]?.asString?.let {
        Crypto.toString(Crypto.sha256(URL(it).readBytes()))
    }

    override fun getDownload(version: String): String? = getMeta(version).let {
        if(it.has("origin")) it["origin"].asString else null
    }

    override fun getStability(version: String): String = "unknown"
}