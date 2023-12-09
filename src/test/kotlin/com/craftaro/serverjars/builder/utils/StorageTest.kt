package com.craftaro.serverjars.builder.utils

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class StorageTest {

    @Test
    fun `write test file to serverjars folder`()  {
        val uuid = UUID.randomUUID()
        Storage.write("$uuid.txt", "test".toByteArray())

        assertTrue(Storage.contains("$uuid.txt"))
        Storage.delete("$uuid.txt")
    }

    @Test
    fun `test file exists in serverjars folder`() {
        val uuid = UUID.randomUUID()
        Storage.write("$uuid.txt", "test".toByteArray())
        assertTrue(Storage.contains("$uuid.txt"))
        assertFalse(Storage.contains("test2.txt"))
        Storage.delete("$uuid.txt")
        assertFalse(Storage.contains("$uuid.txt"))
    }

    @Test
    @Order(3)
    fun `read test file from serverjars folder`() {
        val uuid = UUID.randomUUID()
        Storage.write("$uuid.txt", "test".toByteArray())
        val data = Storage.read("$uuid.txt")

        assertNotNull(data)
        assertEquals("test", String(data))
        Storage.delete("$uuid.txt")
    }

    @Test
    fun `delete test file from serverjars folder`() {
        val uuid = UUID.randomUUID()
        Storage.write("$uuid.txt", "test".toByteArray())
        assertTrue(Storage.contains("$uuid.txt"))
        Storage.delete("$uuid.txt")
        assertFalse(Storage.contains("$uuid.txt"))
    }

    @Test
    fun `test upload to non-existing folder`() {
        val uuid = UUID.randomUUID()
        Storage.write("$uuid/$uuid.txt", "test".toByteArray())
        assertTrue(Storage.contains("$uuid/$uuid.txt"))
        Storage.delete("$uuid/$uuid.txt")
        Storage.delete("$uuid/")
    }
}