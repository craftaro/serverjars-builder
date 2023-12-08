package com.craftaro.serverjars.builder.jars.servers

import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.services.PaperAPIService
import com.google.gson.JsonObject

object FoliaService: SoftwareBuilder() {

    override val category: String = "servers"
    override val type: String = "folia"

    private val api = PaperAPIService(
        baseDirectory = baseDirectory,
        project = "folia",
    )

    override fun availableVersions(): List<String> = api.availableVersions()

    override fun getMeta(version: String): JsonObject = api.getMeta(version)

    override fun getHash(version: String): String? = api.getMeta(version)["hash"]?.asString

    override fun getDownload(version: String): String? = api.getMeta(version)["origin"]?.asString

    override fun getStability(version: String): String = api.getMeta(version).get("stability")?.asString ?: "unknown"


}