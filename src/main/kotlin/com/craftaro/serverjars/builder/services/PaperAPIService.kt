package com.craftaro.serverjars.builder.services

import com.craftaro.serverjars.builder.utils.CachingService
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URL
import java.nio.charset.Charset
import java.util.*

class PaperAPIService(
    private val baseDirectory: String,
    private val project: String,
){

    private val display = project.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    fun availableVersions(): List<String> = CachingService.rememberMinutes("$baseDirectory/versions", 5) {
        val data = URL("https://api.papermc.io/v2/projects/$project/").readText(Charset.defaultCharset())
        JsonParser.parseString(data).asJsonObject
    }.getAsJsonArray("versions").reversed().map { it.asString }

    fun getMeta(version: String): JsonObject = CachingService.rememberMinutes("$baseDirectory/$version/meta", 5) {
        val manifest = JsonParser.parseString(URL("https://api.papermc.io/v2/projects/$project/versions/$version/builds").readText()).asJsonObject
        val latestBuild = manifest.getAsJsonArray("builds").last().asJsonObject ?: run {
            println("Error: No builds found for $display $version")
            JsonObject()
        }

        val build = latestBuild.get("build").asInt
        val stability = latestBuild.get("channel").asString.let { if(it == "default") "stable" else it }
        val download = "https://api.papermc.io/v2/projects/$project/versions/${version}/builds/${latestBuild.get("build").asInt}/downloads/${latestBuild.get("downloads").asJsonObject.get("application").asJsonObject.get("name").asString}"
        val hash = "sha256:${latestBuild.get("downloads").asJsonObject.get("application").asJsonObject.get("sha256").asString}"

        JsonObject().apply {
            addProperty("build", build)
            addProperty("stability", stability)
            addProperty("origin", download)
            addProperty("hash", hash)
        }
    }

}