package com.craftaro.serverjars.builder.services

import com.google.gson.JsonParser
import java.net.URL

object MojangService {

    val manifest = CachingService.rememberMinutes(key = "mojangManifest", ttl = 15) {
        URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").readText().let {
            JsonParser.parseString(it).asJsonObject
        }
    }

    val latestRelease = manifest.getAsJsonObject("latest").get("release").asString
    val latestReleaseManifest = manifest.getAsJsonArray("versions").first { it.asJsonObject.get("id").asString == latestRelease }.asJsonObject

    val latestSnapshot = manifest.getAsJsonObject("latest").get("snapshot").asString
    val latestSnapshotManifest = manifest.getAsJsonArray("versions").first { it.asJsonObject.get("id").asString == latestSnapshot }.asJsonObject

    /* Map of versions and respective manifests URIs */
    val versions = manifest.getAsJsonArray("versions")
        .associate { it.asJsonObject.get("id").asString to it.asJsonObject.get("url").asString }

    val releaseVersions = manifest.getAsJsonArray("versions")
        .filter { it.asJsonObject.get("type").asString == "release" }
        .associate { it.asJsonObject.get("id").asString to it.asJsonObject.get("url").asString }

    fun versionManifest(version: String) = CachingService.rememberMinutes(key = "mojangVersionManifest-$version", ttl = 15) {
        URL(versions[version]).readText().let {
            JsonParser.parseString(it).asJsonObject
        }
    }

    fun assetVersion(version: String) = version.split(".").let { "${it[0]}.${it[1]}" }

}