package com.craftaro.serverjars.builder.services

import com.craftaro.serverjars.builder.utils.CachingService
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URL
import java.nio.charset.Charset
import java.util.*

class MohistAPIService(
    private val baseDirectory: String,
    private val project: String,
){

    private val display = project.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    fun availableVersions(): List<String> = CachingService.rememberMinutes("$baseDirectory/versions", 5) {
        JsonParser.parseString(URL("https://mohistmc.com/api/v2/projects/$project").readText(Charset.defaultCharset())).asJsonObject
    }.getAsJsonArray("versions").reversed().map { it.asString }

    fun getMeta(version: String): JsonObject = CachingService.rememberMinutes("$baseDirectory/$version/meta", 5) {
        val manifest = JsonParser.parseString(URL("https://mohistmc.com/api/v2/projects/$project/$version/builds").readText()).asJsonObject
        val latestBuild = manifest.getAsJsonArray("builds").maxByOrNull {
            it.asJsonObject.get("createdAt").asLong
        }?.asJsonObject ?: run {
            println("Error: No builds found for $display $version")
            JsonObject()
        }

        latestBuild.remove("number").let {
            if(it?.isJsonNull == false) {
                latestBuild.addProperty("build", it.asInt)
            }
        }

        latestBuild.remove("fileMd5").let {
            if(it?.isJsonNull == false) {
                latestBuild.addProperty("hash", it.asString)
            }
        }

        latestBuild.remove("url").let {
            if(it?.isJsonNull == false) {
                latestBuild.addProperty("origin", it.asString)
            }
        }

        latestBuild.remove("createdAt").let {
            if(it?.isJsonNull == false) {
                latestBuild.addProperty("created_at", it.asLong)
            }
        }

        latestBuild.remove("gitSha")
        latestBuild.remove("originUrl")


        latestBuild
    }
}