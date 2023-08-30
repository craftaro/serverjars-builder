package com.craftaro.serverjars.builder.services.utils

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.smithy.kotlin.runtime.net.Url
import com.craftaro.serverjars.builder.App
import kotlinx.coroutines.runBlocking
import java.io.File

interface Storage {

    fun read(path: String): String?

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

    override fun read(path: String): String? = try {
        runBlocking {
            s3client.readByteArrayFromS3(path)?.let { String(it) }
        }
    } catch (e: Exception) {
        null
    }

    override fun write(path: String, contents: ByteArray, permission: String, checksum: String?): Any? = try {
        runBlocking {
            s3client.uploadByteArrayToS3(contents, path, acl=permission, checksumSha256 = checksum)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    override fun delete(path: String) {
        try {
            runBlocking {
                s3client.deleteObject(DeleteObjectRequest {
                    bucket = App.env["S3_BUCKET"]
                    key = path
                })
            }
        } catch (e: Exception) {
            println("Failed to delete $path from S3")
        }
    }

    override fun contains(path: String): Boolean =
        read(path) != null
}

class LocalStorage: Storage {

    override fun read(path: String): String? = File(path).let {
        if(it.exists()) it.readText() else null
    }

    override fun write(path: String, contents: ByteArray, permission: String, checksum: String?): Any =
        File(path).apply {
            parentFile.mkdirs()
            if(!exists()) createNewFile()
            writeBytes(contents)
        }

    override fun delete(path: String) {
        File(path).deleteRecursively()
    }

    override fun contains(path: String): Boolean =
        File(path).exists()

}