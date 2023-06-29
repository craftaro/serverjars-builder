package com.craftaro.serverjars.builder

import com.craftaro.serverjars.builder.servers.PaperService
import com.craftaro.serverjars.builder.services.CachingService
import com.craftaro.serverjars.builder.services.MojangService
import okhttp3.OkHttpClient
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import java.io.File

object App {

    val okHttpclient = OkHttpClient.Builder()
        .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
        .followRedirects(true)
        .build()

    val env = mutableMapOf<String, String>()

}

fun main(args: Array<out String>){
    val options = Options()
    options.addOption(
        Option.builder("c")
            .longOpt("category")
            .desc("The category of the jar to build (servers, vanilla, proxies, modded, etc.)")
            .hasArg()
            .build()
    )

    options.addOption(
        Option.builder("t")
            .longOpt("type")
            .desc("The type of jar to build (vanilla, paper, etc.)")
            .hasArg()
            .build()
    )

    options.addOption(
        Option.builder("v")
            .longOpt("version")
            .desc("The version of the jar to build (1.16.5, 1.17.1, etc.)")
            .hasArg()
            .build()
    )

    options.addOption(
        Option.builder("h")
            .longOpt("help")
            .desc("Prints this help message")
            .build()
    )


    val parser = DefaultParser()
    val cmd = parser.parse(options, args)

    if(cmd.options.isEmpty() || cmd.hasOption("help")) {
        println("Usage: java -jar ServerJarsBuilder.jar [options]")
        println("Options:")
        options.options.forEach {
            println("\t-${it.opt} --${it.longOpt}\t${it.description}")
        }
        return
    }


    var now = System.currentTimeMillis()
    println("Initializing caching service...")
    CachingService.init()
    println("Caching service initialized in ${System.currentTimeMillis() - now}ms")

    now = System.currentTimeMillis()
    println("Initializing Environment variables...")
    App.env.putAll(System.getenv())
    File(".env").readLines().filter { it.isNotBlank() }.forEach {
        val (key, value) = it.split("=")
        App.env[key] = value
    }
    println("Environment variables initialized in ${System.currentTimeMillis() - now}ms")

    println("Loading with secret ${(App.env["CDN_SECRET"] ?: "").toCharArray().map { '*' }.joinToString("")}")

    val category = cmd.getOptionValue("category")
    val type = cmd.getOptionValue("type")
    val version = cmd.getOptionValue("version")

    when(category) {
        "servers" -> {
            when(type) {
                "paper" -> {
                    if(version.contains(";")) {
                        version.split(";").forEach { v ->
                            PaperService.build(minecraftVersion = v)
                        }
                    } else if(version == "all") {
                        MojangService.releaseVersions.forEach { v ->
                            PaperService.build(minecraftVersion = v.key)
                        }
                    } else {
                        PaperService.build(minecraftVersion = version)
                    }
                }
                else -> {
                    println("Unknown server type $type")
                }
            }
        }
        "vanilla" -> {
            when(type) {
                "vanilla" -> {
                    println("Building vanilla client jar for version $version")
                }
                "snapshot" -> {
                    println("Building vanilla snapshot client jar for version $version")
                }
                else -> {
                    println("Unknown vanilla type $type")
                }
            }
        }
        else -> {
            println("Unknown category $category")
        }
    }
}