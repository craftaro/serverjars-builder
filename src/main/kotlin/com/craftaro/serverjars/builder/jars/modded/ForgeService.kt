package com.craftaro.serverjars.builder.jars.modded

import com.craftaro.serverjars.builder.models.SoftwareBuilder
import com.craftaro.serverjars.builder.utils.CachingService
import com.craftaro.serverjars.builder.utils.Crypto
import com.google.gson.JsonObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URL

object ForgeService: SoftwareBuilder() {
    override val category: String = "modded"
    override val type: String = "forge"

    override fun availableVersions(): List<String> = Jsoup.parse(CachingService.rememberStringMinutes("$baseDirectory/versions", 5) {
        URL("https://files.minecraftforge.net/net/minecraftforge/forge/").readText()
    }).body()
        .select("li[class*=li-version-list] > ul[class*=nav-collapsible] > li > a")
        .map { it.attr("href") }
        .map { it.substringAfter("https://files.minecraftforge.net/net/minecraftforge/forge/index_").substringBefore(".html") }

    override fun getMeta(version: String): JsonObject {
        val data = CachingService.rememberStringMinutes("$baseDirectory/meta/$version", 5) {
            URL("https://files.minecraftforge.net/net/minecraftforge/forge/index_$version.html").readText()
        }
        val doc = Jsoup.parse(data).body()
        val downloads = doc.select("[class*=promos-content] > [class*=downloads] > [class*=download]")
        val download = if(downloads.size > 1 && doc.text().contains("Download Recommended")) { // We have a recommended jar!
            downloads[2]
        } else {
            downloads[1]
        }


        val versions = download.select("div[class*=title] > small").text().split(" - ").map { it.trim() }
        return JsonObject().apply {
            addProperty("forge_version", versions[1])
            addProperty("created_at", download.select("div[class*=title] > div[title]").attr("title"))
            addProperty("origin", download.select("div[class*=links] > div[class*=link-boosted] > a").attr("href").substringAfter("?").split("&").first { param -> param.startsWith("url=") }.substringAfter("url="))
            addProperty("stability", if(downloads.size > 1 && doc.text().contains("Download Recommended")) "stable" else "snapshot")
        }
    }

    override fun getHash(version: String): String? = getMeta(version)["origin"]?.asString?.let { origin ->
        Crypto.toString(Crypto.sha256(URL(origin).readBytes()))
    }

    override fun getDownload(version: String): String? = getMeta(version)["origin"]?.asString

    override fun getStability(version: String): String = getMeta(version)["stability"]?.asString ?: "unknown"
}