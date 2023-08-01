package com.craftaro.serverjars.builder.services.servers

import com.craftaro.serverjars.builder.App
import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.models.SoftwareFile
import com.craftaro.serverjars.builder.services.utils.CachingService
import com.craftaro.serverjars.builder.services.utils.Crypto
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URL

object PufferfishService: SoftwareBuilder() {
    override val category: String = "servers"
    override val type: String = "pufferfish"

    override fun availableVersions(): List<String> =
        CachingService.rememberMinutes("$baseDirectory/versions", 5) {
            JsonParser.parseString(URL("https://ci.pufferfish.host/api/json").readText()).asJsonObject
        }.getAsJsonArray("jobs")
            .reversed()
            .filter { it.asJsonObject.get("name").asString.let { name -> name.startsWith("Pufferfish-") && !name.lowercase().contains("purpur") } }
            .map { it.asJsonObject.get("name").asString.substringAfter("Pufferfish-") }

    override fun build(version: String) {
        val data = JsonParser.parseString(URL("https://ci.pufferfish.host/job/Pufferfish-$version/lastSuccessfulBuild/api/json").readText()).asJsonObject
        val manifest = (data.getAsJsonArray("actions").filter { it.asJsonObject.has("buildsByBranchName") }.find { it.asJsonObject.get("buildsByBranchName").asJsonObject.has("refs/remotes/origin/ver/$version") } ?: run {
            println("Failed to find manifest for Pufferfish $version")
            return
        }).asJsonObject.get("buildsByBranchName").asJsonObject.get("refs/remotes/origin/ver/$version").asJsonObject

        val buildNumber = manifest.get("buildNumber").asInt
        val hash = "sha1:${manifest.get("marked").asJsonObject.get("SHA1").asString}"
        if(isInDatabase(version = version, hash = hash)) {
            println("Pufferfish $version build $buildNumber already built")
            return
        }

        val relativePath = data.getAsJsonArray("artifacts").first().asJsonObject.get("relativePath").asString
        val download = "https://ci.pufferfish.host/job/Pufferfish-$version/$buildNumber/artifact/$relativePath"

        println("Building Pufferfish $version build $buildNumber...")
        saveToDatabase(SoftwareFile(
            version = version,
            stability = "stable",
            hash = hash,
            download = "https://cdn.craftaro.com/$baseDirectory/$version/pufferfish-$version.jar",
            meta = JsonObject().apply {
                addProperty("build", buildNumber)
                addProperty("origin", download)
            }
        ))

        println("Uploading Pufferfish $version build $buildNumber to Storage...")
        val bytes = URL(download).readBytes()
        App.storage.write(
            path = "$baseDirectory/$version/pufferfish-$version.jar",
            contents = bytes,
            permission = "public-read",
            checksum = Crypto.toString(Crypto.sha256(bytes))
        )
    }
}