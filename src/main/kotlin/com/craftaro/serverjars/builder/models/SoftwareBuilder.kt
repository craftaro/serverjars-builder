package com.craftaro.serverjars.builder.models

import com.craftaro.serverjars.builder.App
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import java.nio.charset.Charset

abstract class SoftwareBuilder {

    abstract val category: String
    abstract val type: String

    val baseDirectory: String
        get() = "serverjars/$category/$type"

    val db: MutableList<SoftwareFile> = mutableListOf()

    abstract fun availableVersions(): List<String>

    abstract fun build(version: String)

    fun buildAll(versions: Array<String> = arrayOf("all")) {
        if(versions.firstOrNull()?.lowercase() == "all") {
            availableVersions().forEach { build(it) }
        } else {
            versions.forEach { build(it) }
        }
    }

    open fun loadDatabase() {
        if(db.isNotEmpty()) return

        JsonParser.parseString(App.storage.read("$baseDirectory/meta.json") ?: "[]").asJsonArray.forEach { value ->
            value.asJsonObject.apply {
                db.add(
                    SoftwareFile(
                        version = get("version").asString,
                        stability = get("stability").asString,
                        hash = get("hash").asString,
                        download = get("download").asString,
                        meta = get("meta").asJsonObject
                    )
                )
            }
        }
    }

    open fun isInDatabase(version: String, hash: String): Boolean =
        db.any { it.version == version && it.hash == hash }

    open fun saveToDatabase(file: SoftwareFile) {
        if(isInDatabase(file.version, file.hash)) return

        // Now if in the db there's the same version replace it, but keep the same index
        val index = db.indexOfFirst { it.version == file.version }

        if(index != -1) {
            db[index] = file
        } else {
            db.add(file)
        }
    }

    open fun saveDatabase() {
        val data = JsonArray()
        db.forEach { value ->
            data.add(value.toJson())
        }

        App.storage.write("$baseDirectory/meta.json", data.toString().toByteArray(Charset.defaultCharset()))
    }

}