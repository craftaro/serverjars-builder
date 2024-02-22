package com.craftaro.serverjars.builder.jars.modded

import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.utils.CachingService
import com.craftaro.serverjars.builder.utils.Crypto
import com.craftaro.serverjars.builder.utils.asJson
import com.google.gson.JsonObject
import java.net.URL

object CatServerService: SoftwareBuilder() {

    override val type: String = "modded"
    override val category: String = "catserver"

    override fun availableVersions(): List<String> = URL("https://jenkins.rbqcloud.cn:30011/api/json").asJson()
        .asJsonObject["jobs"]
        .asJsonArray
        .filter { it.asJsonObject["name"].asString.startsWith("CatServer") }
        .map { it.asJsonObject["name"].asString.split("-")[1] }
        .reversed()

    override fun getMeta(version: String): JsonObject = CachingService.rememberMinutes("catserver-$version", 5) {
        val data = URL("https://jenkins.rbqcloud.cn:30011/job/CatServer-$version/lastSuccessfulBuild/api/json").asJson().asJsonObject
        JsonObject().apply {
            addProperty("build_number", data["number"].asInt)
            addProperty("origin", "https://jenkins.rbqcloud.cn:30011/job/CatServer-$version/lastSuccessfulBuild/artifact/${data["artifacts"].asJsonArray.first().asJsonObject["relativePath"].asString}")
        }
    }

    override fun getHash(version: String): String? = getMeta(version)["origin"]?.asString?.let {
        try {
            Crypto.toString(Crypto.sha256(URL(it).readBytes()))
        } catch (_: Exception) {
            null
        }
    }

    override fun getDownload(version: String): String? = getMeta(version)["origin"]?.asString

    override fun getStability(version: String): String = "stable"
}