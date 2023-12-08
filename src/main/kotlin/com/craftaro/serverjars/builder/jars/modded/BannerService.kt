package com.craftaro.serverjars.builder.jars.modded

import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.services.MohistAPIService
import com.google.gson.JsonObject

object BannerService: SoftwareBuilder() {

    override val category: String = "modded"
    override val type: String = "banner"

    private val api = MohistAPIService(
        baseDirectory = baseDirectory,
        project = type
    )

    override fun availableVersions(): List<String> = api.availableVersions()

    override fun getMeta(version: String): JsonObject = api.getMeta(version)

    override fun getHash(version: String): String? = getMeta(version)["hash"]?.asString

    override fun getDownload(version: String): String? = getMeta(version)["origin"]?.asString

    override fun getStability(version: String): String = "unknown"
}