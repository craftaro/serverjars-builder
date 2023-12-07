package com.craftaro.serverjars.builder

import com.craftaro.serverjars.builder.utils.Storage
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

internal class AllServicesTest {

    @Test
    fun `test all services`() {
        val services = App.services()
        for(service in services) {
            val versions = service.availableVersions()
            assertTrue(versions.isNotEmpty(), "Service ${service.baseDirectory} has no versions")

            // Build latest version
            val latestVersion = versions.first()
            service.build(latestVersion)

            assertTrue(Storage.contains("${service.baseDirectory}/$latestVersion/${service.type}-$latestVersion.jar"), "Service ${service.baseDirectory} failed to build latest version")
            service.saveDatabase()
        }
    }
}