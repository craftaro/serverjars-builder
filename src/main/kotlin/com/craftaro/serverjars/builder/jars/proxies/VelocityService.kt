package com.craftaro.serverjars.builder.jars.proxies

import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.services.PaperAPIService
import com.google.gson.JsonObject

object VelocityService: SoftwareBuilder() {

    override val type: String = "proxy"
    override val category: String = "velocity"

    private val api = PaperAPIService(
        baseDirectory = baseDirectory,
        project = "velocity",
    )

    override fun availableVersions(): List<String> = api.availableVersions()

    override fun getMeta(version: String): JsonObject = api.getMeta(version)

    override fun getHash(version: String): String? = getMeta(version)["hash"]?.asString

    override fun getDownload(version: String): String? = getMeta(version)["origin"]?.asString

    override fun getStability(version: String): String = getMeta(version)["stability"]?.asString ?: "unknown"
}