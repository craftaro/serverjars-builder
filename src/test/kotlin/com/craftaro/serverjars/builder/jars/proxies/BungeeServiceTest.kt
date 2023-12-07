package com.craftaro.serverjars.builder.jars.proxies

import com.craftaro.serverjars.builder.utils.Storage
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class BungeeServiceTest {

    @Test
    fun `Test availableVersions() returns a list of versions`() {
        val versions = BungeeService.availableVersions()
        assertTrue(versions.isNotEmpty())
    }

    @Test
    fun `Test 1_20 is present in availableVersions()`() {
        val versions = BungeeService.availableVersions()
        assertTrue(versions.contains("1.20"))
    }

    @Test
    fun `Test build() 1_19`() {
        BungeeService.build("1.19")
        assertTrue(Storage.contains("${BungeeService.baseDirectory}/1.19/bungee-1.19.jar"))
    }

}