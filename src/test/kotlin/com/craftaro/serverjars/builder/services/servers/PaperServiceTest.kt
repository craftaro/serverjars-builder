package com.craftaro.serverjars.builder.services.servers

import com.craftaro.serverjars.builder.App
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
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
        App.storage.contains("${PaperService.baseDirectory}/1.20/paper-1.20.jar")
    }

    @Test
    fun `build() latest`() {
        val version = PaperService.availableVersions().first()
        PaperService.build(version)
        App.storage.contains("${PaperService.baseDirectory}/$version/paper-$version.jar")
    }

    companion object {
        @JvmStatic
        @AfterAll
        fun cleanup() {
            arrayOf("serverjars/").map { File(it) }.filter{ it.exists() }.forEach { FileUtils.forceDelete(it) }
        }
    }
}