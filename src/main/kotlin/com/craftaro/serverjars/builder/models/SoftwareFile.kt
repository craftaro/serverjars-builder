package com.craftaro.serverjars.builder.models

import com.google.gson.JsonObject

data class SoftwareFile(
    val version: String,
    val stability: String,
    val hash: String,
    val download: String,
    val meta: JsonObject,
    val built: Long,
    val size: SoftwareFileSize
) {
    fun toJson(): JsonObject = JsonObject().apply {
        addProperty("version", version)
        addProperty("stability", stability)
        addProperty("hash", hash)
        addProperty("download", download)
        addProperty("built", built)
        add("size", size.toJson())
        add("meta", meta)
    }
}

data class SoftwareFileSize(
    val bytes: Int,
    val display: String
) {

    fun toJson(): JsonObject = JsonObject().apply {
        addProperty("bytes", bytes)
        addProperty("display", display)
    }

}
