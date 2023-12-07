package com.craftaro.serverjars.builder.jars.servers

import com.craftaro.serverjars.builder.utils.Storage
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertContains

internal class PaperServiceTest {

    @Test
    fun `availableVersions() returns a list of versions`() {
        val versions = PaperService.availableVersions()
        assertTrue(versions.isNotEmpty())
    }

    @Test
    fun `availableVersions() contains 1_20`() {
        val versions = PaperService.availableVersions()
        assertContains(versions, "1.20")
    }

    @Test
    fun `build() builds 1_20`() {
        PaperService.build("1.20")
        assertTrue(Storage.contains("${PaperService.baseDirectory}/1.20/paper-1.20.jar"))
    }

    @Test
    fun `build() latest`() {
        val version = PaperService.availableVersions().first()
        PaperService.build(version)
        assertTrue(Storage.contains("${PaperService.baseDirectory}/$version/paper-$version.jar"))
    }

}