package com.craftaro.serverjars.builder.services.utils

import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.net.url.Url
import com.craftaro.serverjars.builder.App
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class S3StorageTest {

    private val env = EnvironmentUtils()

    private val s3Client = S3Client {
        region = env["S3_REGION"]
        credentialsProvider = CraftaroS3CredentialsProvider()
        endpointUrl = Url.parse(App.env["S3_ENDPOINT"] ?: "")
    }

    @Test
    @Order(1)
    fun `write test file to serverjars folder`()  {
        val response = runBlocking {
            s3Client.uploadObjectBytes("test".toByteArray(), "test.txt")
            s3Client.objectExists("test.txt")
        }

        assertTrue(response)
    }

    @Test
    @Order(2)
    fun `test file exists in serverjars folder`() {
        val exists = runBlocking {
            s3Client.objectExists("test.txt")
        }

        val doesNotExist = runBlocking {
            s3Client.objectExists("test2.txt")
        }

        assertTrue(exists)
        assertFalse(doesNotExist)
    }

    @Test
    @Order(3)
    fun `read test file from serverjars folder`() {
        val response = runBlocking {
            s3Client.readObjectBytes("test.txt")?.let { String(it) }
        }

        val exists = runBlocking {
            s3Client.objectExists("test.txt")
        }

        assertNotNull(response)
        assertEquals("test", response)
        assertTrue(exists)
    }

    @Test
    @Order(4)
    fun `delete test file from serverjars folder`() {
        val response = runBlocking {
            s3Client.deleteObject("test.txt")
            s3Client.objectExists("test.txt")
        }

        assertFalse(response)
    }
}