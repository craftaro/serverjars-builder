package com.craftaro.serverjars.builder.models

import com.google.gson.JsonObject

data class SoftwareFile(
    val version: String,
    val stability: String,
    val hash: String,
    val download: String,
    val meta: JsonObject
) {
    fun toJson(): JsonObject = JsonObject().apply {
        addProperty("version", version)
        addProperty("stability", stability)
        addProperty("hash", hash)
        addProperty("download", download)
        add("meta", meta)
    }
}
