package com.craftaro.serverjars.builder.models

import com.craftaro.serverjars.builder.utils.Crypto
import com.craftaro.serverjars.builder.utils.Storage
import com.craftaro.serverjars.builder.App
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URL
import java.nio.charset.Charset

abstract class SoftwareBuilder {

    abstract val type: String
    abstract val category: String

    val baseDirectory: String
        get() = "$type/$category"

    private val db: MutableList<SoftwareFile> = mutableListOf()

    open fun isDiscontinued(): Boolean = false

    abstract fun availableVersions(): List<String>

    abstract fun getMeta(version: String): JsonObject

    abstract fun getHash(version: String): String?

    abstract fun getDownload(version: String): String?

    abstract fun getStability(version: String): String

    fun build(version: String) {
        val display = category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
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
        val meta = getMeta(version)
        if(meta.isEmpty) {
            toPrint += "\n\tFailed to find meta for version $version"
            println(toPrint)
            return
        }

        toPrint += "\n\tUploading version $version to Storage..."
        val bytes = URL(download).readBytes()
        Storage.write(
            path = "$baseDirectory/$version/$category-$version.jar",
            contents = bytes,
            permission = "public-read",
            checksum = Crypto.toString(Crypto.sha256(bytes))
        )

        saveToDatabase(SoftwareFile(
            version = version,
            stability = getStability(version),
            hash = hash,
            download = "https://cdn.craftaro.com/${App.env["SERVERJARS_FOLDER"]}/$baseDirectory/$version/$category-$version.jar",
            built = System.currentTimeMillis(),
            size = SoftwareFileSize(
                bytes = bytes.size,
                // Display as MiB
                display = "${"%.2f".format(bytes.size / 1024.0 / 1024.0)} MiB"
            ),
            meta = meta,
        ))

        println("\n$toPrint")
    }

    fun buildAll(versions: Array<String> = arrayOf("all")) = try {
        if(versions.firstOrNull()?.lowercase() == "all") {
            availableVersions().forEach { build(it) }
        } else {
            versions.forEach { build(it) }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    open fun loadDatabase() {
        if(db.isNotEmpty()) return

        JsonParser.parseString(if(Storage.contains("$baseDirectory/meta.json")) (Storage.readString("$baseDirectory/meta.json") ?: "[]") else "[]").asJsonArray.forEach { value ->
            value.asJsonObject.apply {
                db.add(
                    SoftwareFile(
                        version = this["version"].asString,
                        stability = this["stability"].asString,
                        hash = this["hash"].asString,
                        download = this["download"].asString,
                        built = this["built"]?.isJsonNull?.takeIf { !it }?.let { this["built"].asLong } ?: System.currentTimeMillis(),
                        size = SoftwareFileSize(
                            bytes = this["size"]?.asJsonObject?.get("bytes")?.asInt ?: 0,
                            display = this["size"]?.asJsonObject?.get("display")?.asString ?: "0 B"
                        ),
                        meta = this["meta"].asJsonObject
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
        if(isInDatabase(file.version, file.hash)) {
            // Now check for a hash of the contents
            val inDatabaseHash = Crypto.sha256(db.firstOrNull { it.version == file.version && it.hash == file.hash }?.toJson().toString().toByteArray())
            val newHash = Crypto.sha256(file.toJson().toString().toByteArray())
            if (Crypto.secureEquals(inDatabaseHash, newHash)) {
                return
            }
        }

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

        if(Storage.contains("$type/meta.json")) {
            val typeData = JsonParser.parseString(Storage.readString("$type/meta.json") ?: "[]").asJsonArray
            if(typeData.none { it.asString == category }) {
                typeData.add(category)
                Storage.write("$type/meta.json", typeData.toString().toByteArray(Charset.defaultCharset()))
            }
        } else {
            Storage.write("$type/meta.json", JsonArray().apply { add(category) }.toString().toByteArray(Charset.defaultCharset()))
        }
    }

}