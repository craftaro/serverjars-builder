package com.craftaro.serverjars.builder.jars.modded

import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.services.MohistAPIService
import com.google.gson.JsonObject

object MohistService: SoftwareBuilder() {

    override val type: String = "modded"
    override val category: String = "mohist"

    private val api = MohistAPIService(
        baseDirectory = baseDirectory,
        project = category
    )

    override fun availableVersions(): List<String> = api.availableVersions()

    override fun getMeta(version: String): JsonObject = api.getMeta(version)

    override fun getHash(version: String): String? = getMeta(version)["hash"]?.asString

    override fun getDownload(version: String): String? = getMeta(version)["origin"]?.asString

    override fun getStability(version: String): String = "unknown"
}