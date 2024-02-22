package com.craftaro.serverjars.builder.jars.servers

import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.utils.CachingService
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URL

object PufferfishService: SoftwareBuilder() {
    override val type: String = "servers"
    override val category: String = "pufferfish"

    override fun availableVersions(): List<String> =
        CachingService.rememberMinutes("$baseDirectory/versions", 5) {
            JsonParser.parseString(URL("https://ci.pufferfish.host/api/json").readText()).asJsonObject
        }.getAsJsonArray("jobs")
            .reversed()
            .filter { it.asJsonObject.get("name").asString.let { name -> name.startsWith("Pufferfish-") && !name.lowercase().contains("purpur") } }
            .map { it.asJsonObject.get("name").asString.substringAfter("Pufferfish-") }

    override fun getMeta(version: String): JsonObject = CachingService.rememberMinutes("$baseDirectory/$version/meta", 5) {
        val data = JsonParser.parseString(URL("https://ci.pufferfish.host/job/Pufferfish-$version/lastSuccessfulBuild/api/json").readText()).asJsonObject
        val manifest = (data.getAsJsonArray("actions").filter { it.asJsonObject.has("buildsByBranchName") }.find { it.asJsonObject.get("buildsByBranchName").asJsonObject.has("refs/remotes/origin/ver/$version") } ?: run {
            println("Failed to find manifest for Pufferfish $version")
            JsonObject()
        }).asJsonObject.get("buildsByBranchName").asJsonObject.get("refs/remotes/origin/ver/$version").asJsonObject

        val buildNumber = manifest.get("buildNumber").asInt
        val hash = "sha1:${manifest.get("marked").asJsonObject.get("SHA1").asString}"

        val buildManifest = JsonParser.parseString(URL("https://ci.pufferfish.host/job/Pufferfish-$version/$buildNumber/api/json").readText()).asJsonObject.get("artifacts").asJsonArray.find { it.asJsonObject.has("fileName") && (it.asJsonObject.get("fileName").asString.startsWith("pufferfish") && it.asJsonObject.get("fileName").asString.endsWith(".jar")) }?.asJsonObject ?: run {
            println("Failed to find build manifest for Pufferfish $version")
            JsonObject()
        }

        val relativePath = buildManifest.get("relativePath").asString

        JsonObject().apply {
            addProperty("build", buildNumber)
            addProperty("origin", "https://ci.pufferfish.host/job/Pufferfish-$version/$buildNumber/artifact/$relativePath")
            addProperty("hash", hash)
        }
    }

    override fun getHash(version: String): String? = getMeta(version).let { if(it.has("hash")) it.get("hash").asString else null }

    override fun getDownload(version: String): String? = getMeta(version).let { if(it.has("origin")) it.get("origin").asString else null }

    override fun getStability(version: String): String = "unknown"
}