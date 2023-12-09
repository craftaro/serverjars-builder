package com.craftaro.serverjars.builder.services

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class MohistAPIServiceTest {

    @Test
    fun `test mohist has available versions`() {
        val api = MohistAPIService("modded/mohist", "mohist")
        val versions = api.availableVersions()
        assertTrue(versions.isNotEmpty())
    }

    @Test
    fun `test mohist has meta`() {
        val api = MohistAPIService("modded/mohist", "mohist")
        val meta = api.getMeta("1.20.2")
        assertTrue(meta.get("build").asInt >= 122)
    }

    @Test
    fun `test banner has available versions`() {
        val api = MohistAPIService("modded/banner", "banner")
        val versions = api.availableVersions()
        assertTrue(versions.isNotEmpty())
    }

    @Test
    fun `test banner has meta`() {
        val api = MohistAPIService("modded/banner", "banner")
        val meta = api.getMeta("1.20.1")
        assertTrue(meta.get("build").asInt >= 450)
    }
}