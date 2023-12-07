package com.craftaro.serverjars.builder.utils

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URL

object MojangService {

    val manifest: JsonObject = CachingService.rememberMinutes(key = "mojangManifest", ttl = 15) {
        URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").readText().let {
            JsonParser.parseString(it).asJsonObject
        }
    }

    val latestRelease: String = manifest.getAsJsonObject("latest").get("release").asString
    val latestReleaseManifest: JsonObject = manifest.getAsJsonArray("versions").first { it.asJsonObject.get("id").asString == latestRelease }.asJsonObject

    val latestSnapshot: String = manifest.getAsJsonObject("latest").get("snapshot").asString
    val latestSnapshotManifest: JsonObject = manifest.getAsJsonArray("versions").first { it.asJsonObject.get("id").asString == latestSnapshot }.asJsonObject

    /* Map of versions and respective manifests URIs */
    val versions: Map<String, String> = manifest.getAsJsonArray("versions")
        .associate { it.asJsonObject.get("id").asString to it.asJsonObject.get("url").asString }

    val releaseVersions: Map<String, String> = manifest.getAsJsonArray("versions")
        .filter { it.asJsonObject.get("type").asString == "release" }
        .associate { it.asJsonObject.get("id").asString to it.asJsonObject.get("url").asString }

    fun versionManifest(version: String) =
        CachingService.rememberMinutes(key = "mojangVersionManifest-$version", ttl = 15) {
            URL(versions[version]).readText().let {
                JsonParser.parseString(it).asJsonObject
            }
        }

    fun assetVersion(version: String) = version.split(".").let { "${it[0]}.${it[1]}" }

}