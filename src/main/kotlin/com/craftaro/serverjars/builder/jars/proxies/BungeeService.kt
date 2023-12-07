package com.craftaro.serverjars.builder.jars.proxies

import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.utils.CachingService
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URL

object BungeeService: SoftwareBuilder() {

    override val category: String = "proxies"

    override val type: String = "bungee"

    private val builds = JsonParser.parseString(URL("https://ci.md-5.net/job/Bungeecord/api/json?tree=allBuilds[number,mavenArtifacts[moduleRecords[pomArtifact[version]]{0}]]").readText()).asJsonObject
        .getAsJsonArray("allBuilds")
        .filter { it.asJsonObject.let { json -> json.has("mavenArtifacts") && json.get("mavenArtifacts") != null && !json.get("mavenArtifacts").isJsonNull } }
        .map { it.asJsonObject }
        .map { json ->
            val number = json.get("number").asInt
            val version = json.getAsJsonObject("mavenArtifacts").getAsJsonArray("moduleRecords").first().asJsonObject.getAsJsonObject("pomArtifact").get("version").asString

            number to version.split("-").first()
        }
        .distinctBy { it.second }
        .toMap()



    override fun availableVersions(): List<String> =
        builds.values.toList()

    override fun getMeta(version: String): JsonObject = CachingService.rememberMinutes("$baseDirectory/$version/meta", 5) {
        val buildNumber = builds.filter { it.value == version }.keys.first()
        val manifest = JsonParser.parseString(URL("https://ci.md-5.net/job/Bungeecord/$buildNumber/api/json?tree=artifacts[fileName,relativePath],mavenArtifacts[moduleRecords[mainArtifact[fileName,md5sum]]]").readText()).asJsonObject
        // Find BungeeCord.jar
        val artifact = manifest.getAsJsonArray("artifacts").find { it.asJsonObject.get("fileName").asString == "BungeeCord.jar" } ?: run {
            println("No BungeeCord.jar artifact found for $version")
            JsonObject()
        }
        // Now find md5 sum in mavenArtifacts using the same filename
        val md5sum = manifest.getAsJsonObject("mavenArtifacts").getAsJsonArray("moduleRecords")
            .filter { it.asJsonObject.has("mainArtifact") && it.asJsonObject.get("mainArtifact") != null && !it.asJsonObject.get("mainArtifact").isJsonNull }
            .map { it.asJsonObject.getAsJsonObject("mainArtifact") }
            .find { it.get("fileName").asString == artifact.asJsonObject.get("fileName").asString }?.get("md5sum")?.asString ?: run {
            println("No md5sum found for $version")
            JsonObject()
        }
        // Download
        val url = "https://ci.md-5.net/job/Bungeecord/$buildNumber/artifact/${artifact.asJsonObject.get("relativePath").asString}"
        val hash = "md5:${md5sum}"

        JsonObject().apply {
            addProperty("buildNumber", buildNumber)
            addProperty("origin", url)
            addProperty("hash", hash)
        }
    }

    override fun getHash(version: String): String? = getMeta(version).let { if (it.has("hash")) it.get("hash").asString else null }

    override fun getDownload(version: String): String? = getMeta(version).let { if (it.has("origin")) it.get("origin").asString else null }

    override fun getStability(version: String): String = "snapshot"
}