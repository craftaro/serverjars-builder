package com.craftaro.serverjars.builder.utils

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.smithy.kotlin.runtime.net.url.Url
import com.craftaro.serverjars.builder.App
import kotlinx.coroutines.runBlocking
import java.io.File

interface Storage {

    companion object {
        val storage = if(App.env["STORAGE_TYPE"]?.lowercase() == "s3") S3Storage() else LocalStorage()

        fun read(path: String): ByteArray? = storage.read(path)

        fun readString(path: String): String? = storage.read(path)?.let { String(it) }

        fun write(path: String, contents: ByteArray, permission: String = "public-read", checksum: String? = null): Any? =
            storage.write(path, contents, permission, checksum)

        fun delete(path: String) = storage.delete(path)

        fun contains(path: String): Boolean = storage.contains(path)
    }

    fun readString(path: String): String?

    fun read(path: String): ByteArray?

    fun write(path: String, contents: ByteArray, permission: String = "public-read", checksum: String? = null): Any?

    fun delete(path: String)

    fun contains(path: String): Boolean

}

class S3Storage: Storage {

    companion object {
        private val s3client = S3Client {
            region = App.env["S3_REGION"]
            credentialsProvider = CraftaroS3CredentialsProvider()
            endpointUrl = Url.parse(App.env["S3_ENDPOINT"] ?: "")
        }
    }

    init {
        println("Loading S3Storage with secret ${(App.env["CDN_SECRET"] ?: "").toCharArray().map { '*' }.joinToString("")}")
    }

    override fun read(path: String): ByteArray? = try {
        runBlocking {
            s3client.readObjectBytes(path)
        }
    } catch (e: Exception) {
        null
    }

    override fun readString(path: String): String? = read(path)?.let { String(it) }

    override fun write(path: String, contents: ByteArray, permission: String, checksum: String?): Any? = try {
        runBlocking {
            s3client.uploadObjectBytes(contents, path, acl=permission, checksumSha256 = checksum)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    override fun delete(path: String) {
        try {
            runBlocking {
                s3client.deleteObject(path)
            }
        } catch (e: Exception) {
            println("Failed to delete $path from S3")
        }
    }

    override fun contains(path: String): Boolean = runBlocking {
        s3client.objectExists(path)
    }
}

class LocalStorage: Storage {

    override fun read(path: String): ByteArray? = File(if(App.env["SERVERJARS_FOLDER"] != null) "${App.env["SERVERJARS_FOLDER"]}/$path" else path).let {
        if(it.exists()) it.readBytes() else null
    }

    override fun readString(path: String): String? = read(path)?.let { String(it) }

    override fun write(path: String, contents: ByteArray, permission: String, checksum: String?): Any =
        File(if(App.env["SERVERJARS_FOLDER"] != null) "${App.env["SERVERJARS_FOLDER"]}/$path" else path).apply {
            parentFile.mkdirs()
            if(!exists()) createNewFile()
            writeBytes(contents)
        }

    override fun delete(path: String) {
        File(if(App.env["SERVERJARS_FOLDER"] != null) "${App.env["SERVERJARS_FOLDER"]}/$path" else path).deleteRecursively()
    }

    override fun contains(path: String): Boolean =
        File(if(App.env["SERVERJARS_FOLDER"] != null) "${App.env["SERVERJARS_FOLDER"]}/$path" else path).exists()

}