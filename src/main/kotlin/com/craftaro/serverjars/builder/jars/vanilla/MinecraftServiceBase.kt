package com.craftaro.serverjars.builder.jars.vanilla

import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.services.MojangService
import com.google.gson.JsonObject

abstract class MinecraftServiceBase: SoftwareBuilder() {

    open val releaseOnly: Boolean = true

    override val type: String = "vanilla"

    override fun availableVersions(): List<String> = if(releaseOnly) {
        MojangService.releaseVersions
    } else {
        MojangService.versions
    }.keys.toList()

    override fun getMeta(version: String): JsonObject = JsonObject().apply {
        val manifest = MojangService.versionManifest(version)
        val serverManifest = manifest["downloads"].asJsonObject["server"].asJsonObject
        addProperty("hash", "sha1:${serverManifest["sha1"].asString}")
        addProperty("origin", serverManifest["url"].asString)
        addProperty("stability", manifest["type"].asString)
    }

    override fun getHash(version: String): String? = getMeta(version)["hash"]?.asString

    override fun getDownload(version: String): String? = getMeta(version)["origin"]?.asString

    override fun getStability(version: String): String = getMeta(version)["stability"].asString
}