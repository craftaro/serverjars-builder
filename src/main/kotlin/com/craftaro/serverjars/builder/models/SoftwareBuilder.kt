package com.craftaro.serverjars.builder.models

import com.craftaro.serverjars.builder.utils.Crypto
import com.craftaro.serverjars.builder.utils.Storage
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URL
import java.nio.charset.Charset

abstract class SoftwareBuilder {

    abstract val category: String
    abstract val type: String

    val baseDirectory: String
        get() = "$category/$type"

    private val db: MutableList<SoftwareFile> = mutableListOf()

    abstract fun availableVersions(): List<String>

    abstract fun getMeta(version: String): JsonObject

    abstract fun getHash(version: String): String?

    abstract fun getDownload(version: String): String?

    abstract fun getStability(version: String): String

    fun build(version: String) {
        val display = type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        var toPrint = "$display: Building version $version..."
        val hash = getHash(version) ?: run {
            toPrint += "\n\tFailed to find hash for version $version"
            println(toPrint)
            return
        }

        toPrint += "\n\tChecking if version $version is already built with hash '$hash'..."
        if (isInDatabase(version = version, hash = hash)) {
            toPrint += "\n\t$display: version $version already built"
            println(toPrint)
            return
        }

        val download = getDownload(version) ?: run {
            toPrint += "\n\tFailed to find download for version $version"
            println(toPrint)
            return
        }

        toPrint += "\n\tBuilding version $version..."
        saveToDatabase(SoftwareFile(
            version = version,
            stability = getStability(version),
            hash = hash,
            download = "https://cdn.craftaro.com/$baseDirectory/$version/$type-$version.jar",
            meta = getMeta(version)
        ))

        toPrint += "\n\tUploading version $version to Storage..."
        val bytes = URL(download).readBytes()
        Storage.write(
            path = "$baseDirectory/$version/$type-$version.jar",
            contents = bytes,
            permission = "public-read",
            checksum = Crypto.toString(Crypto.sha256(bytes))
        )
        println("\n$toPrint")
    }

    fun buildAll(versions: Array<String> = arrayOf("all")) {
        if(versions.firstOrNull()?.lowercase() == "all") {
            availableVersions().forEach { build(it) }
        } else {
            versions.forEach { build(it) }
        }
    }

    open fun loadDatabase() {
        if(db.isNotEmpty()) return

        JsonParser.parseString(if(Storage.contains("$baseDirectory/meta.json")) (Storage.readString("$baseDirectory/meta.json") ?: "[]") else "[]").asJsonArray.forEach { value ->
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

    open fun isInDatabase(version: String, hash: String): Boolean {
        if(db.isEmpty()) {
            loadDatabase()
        }

        return db.any { it.version == version && it.hash == hash }
    }

    open fun saveToDatabase(file: SoftwareFile) {
        if(isInDatabase(file.version, file.hash)) return

        // Now if in the db there's the same version, replace it, but keep the same index
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

        Storage.write("$baseDirectory/meta.json", data.toString().toByteArray(Charset.defaultCharset()))
        println("Saved database $baseDirectory/meta.json")
    }

}