package com.craftaro.serverjars.builder.jars.modded

import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.utils.CachingService
import com.craftaro.serverjars.builder.utils.Crypto
import com.craftaro.serverjars.builder.utils.asJson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URL

object FabricService: SoftwareBuilder() {
    override val category: String = "modded"
    override val type: String = "fabric"

    override fun availableVersions(): List<String> = CachingService.rememberMinutes("$baseDirectory/versions", 5) {
        JsonObject().apply {
            val versions = URL("https://meta.fabricmc.net/v2/versions/game").asJson().asJsonArray.filter { it.asJsonObject["stable"]?.asBoolean == true }
            add("versions", JsonArray().apply { versions.forEach { add(it.asJsonObject["version"].asString) } })
        }
    }.getAsJsonArray("versions").map { it.asString }

    private fun getLatestInstaller() = CachingService.rememberMinutes("$baseDirectory/installer", 5) {
        URL("https://meta.fabricmc.net/v2/versions/installer").asJson().asJsonArray.first { it.asJsonObject["stable"]?.asBoolean == true }.asJsonObject
    }

    private fun getLatestLoader() = CachingService.rememberMinutes("$baseDirectory/loader", 5) {
        URL("https://meta.fabricmc.net/v2/versions/loader").asJson().asJsonArray.first { it.asJsonObject["stable"]?.asBoolean == true }.asJsonObject
    }

    override fun getMeta(version: String): JsonObject = JsonObject().apply {
        addProperty("installer", getLatestInstaller()["version"]?.asString)
        addProperty("loader", getLatestLoader()["version"]?.asString)
        addProperty("loader_build", getLatestLoader()["build"]?.asInt)
    }

    override fun getHash(version: String): String? = try {
        val meta = getMeta(version)
        if(!meta.has("loader") || !meta.has("installer")) {
            null
        } else {
            val bytes = URL("https://meta.fabricmc.net/v2/versions/loader/$version/${meta["loader"]?.asString}/${meta["installer"]?.asString}/server/jar").readBytes()
            Crypto.toString(Crypto.sha256(bytes))
        }
    } catch (e: Exception) {
        null
    }

    override fun getDownload(version: String): String {
        val meta = getMeta(version)
        return "https://meta.fabricmc.net/v2/versions/loader/$version/${meta["loader"]?.asString}/${meta["installer"]?.asString}/server/jar"
    }

    override fun getStability(version: String): String = "stable"
}