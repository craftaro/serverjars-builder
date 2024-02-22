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
            if(!service.isDiscontinued()) {
                assertTrue(versions.isNotEmpty(), "Service ${service.baseDirectory} has no versions")
            }

            // Build latest version
            val latestVersion = versions.firstOrNull() ?: continue
            service.build(latestVersion)

            if(!service.isDiscontinued()) {
                assertTrue(Storage.contains("${service.baseDirectory}/$latestVersion/${service.category}-$latestVersion.jar"), "Service ${service.baseDirectory} failed to build latest version")
            }
            service.saveDatabase()
        }
    }
}