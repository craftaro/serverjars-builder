package com.craftaro.serverjars.builder.jars.bedrock

import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.utils.CachingService
import com.craftaro.serverjars.builder.utils.Crypto
import com.craftaro.serverjars.builder.utils.asJson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.net.URL

object PocketMineService: SoftwareBuilder() {
    override val type: String = "bedrock"
    override val category: String = "pocketmine"

    // We don't want to exceed GitHub limits, so we'll only be constructing the latest version
    override fun availableVersions(): List<String> = listOf(URL("https://api.github.com/repos/pmmp/PocketMine-MP/releases/latest").asJson().asJsonObject["tag_name"].asString)

    override fun getMeta(version: String): JsonObject = CachingService.rememberMinutes(key = "pocketmineMeta-$version", ttl = 15) {
        val data = URL(if(version == "latest") "https://api.github.com/repos/pmmp/PocketMine-MP/releases/latest" else "https://api.github.com/repos/pmmp/PocketMine-MP/releases/tags/$version").asJson().asJsonObject
        // Get the 'build_info.json' file to get the build data
        val buildInfo = data.getAsJsonArray("assets").find { it.asJsonObject["name"].asString == "build_info.json" }?.asJsonObject?.get("browser_download_url")?.asString?.let {
            URL(it).asJson().asJsonObject
        } ?: run {
            return@rememberMinutes JsonObject()
        }

        JsonObject().apply {
            addProperty("build", buildInfo.get("build").asNumber)
            addProperty("minecraft_version", buildInfo.get("mcpe_version").asString)
            addProperty("pocketmine_version", buildInfo.get("base_version").asString)
            addProperty("stability", buildInfo.get("channel").asString)
            addProperty("origin", buildInfo.get("download_url").asString)
            add("requirements", JsonArray().apply {
                JsonObject().apply {
                    addProperty("name", "PHP")
                    addProperty("version", buildInfo.get("php_version").asString)
                    addProperty("download", buildInfo.get("php_download_url").asString)
                }
            })
        }
    }

    override fun getHash(version: String): String? = try {
        getMeta(version)["origin"]?.asString?.let {
            Crypto.toString(Crypto.sha256(URL(it).readBytes()))
        }
    } catch (_: Exception) {
        null
    }

    override fun getDownload(version: String): String? = getMeta(version)["origin"]?.asString

    override fun getStability(version: String): String = getMeta(version)["stability"].asString

}