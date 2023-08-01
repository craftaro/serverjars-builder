package com.craftaro.serverjars.builder

import com.craftaro.serverjars.builder.services.servers.PaperService
import com.craftaro.serverjars.builder.services.servers.PufferfishService
import com.craftaro.serverjars.builder.services.servers.PurpurService
import com.craftaro.serverjars.builder.services.servers.SpongeService
import com.craftaro.serverjars.builder.services.utils.CachingService
import com.craftaro.serverjars.builder.services.utils.LocalStorage
import com.craftaro.serverjars.builder.services.utils.S3Storage
import com.craftaro.serverjars.builder.services.utils.Storage
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import java.io.File

object App {
    val env = mutableMapOf<String, String>()
    lateinit var storage: Storage
}

fun services() = listOf(
    PaperService,
    PurpurService,
    SpongeService,
    PufferfishService
)

fun main(args: Array<out String>){
    val options = options()

    val parser = DefaultParser()
    val cmd = parser.parse(options, args)


    var now = System.currentTimeMillis()
    CachingService.init()
    println("Caching service initialized in ${System.currentTimeMillis() - now}ms")

    now = System.currentTimeMillis()
    App.env.putAll(System.getenv())
    File(".env").apply {
        if(exists()) {
            readLines().filter { it.isNotBlank() }.forEach {
                val (key, value) = it.split("=")
                App.env[key] = value
            }
        }
    }
    println("Environment variables initialized in ${System.currentTimeMillis() - now}ms")

    now = System.currentTimeMillis()
    App.storage = if(App.env["STORAGE_TYPE"]?.lowercase() == "s3") S3Storage() else LocalStorage()
    println("Storage initialized in ${System.currentTimeMillis() - now}ms")

    val services = services()

    if(cmd.options.isEmpty() || cmd.hasOption("help")) {
        println("Usage: java -jar ServerJarsBuilder.jar [options]")
        println("Options:")
        options.options.forEach {
            println("\t-${it.opt} --${it.longOpt}\t${it.description}")
        }

        println("\nAvailable categories: ${services.distinctBy { it.category }.joinToString(", "){ it.category }}")
        return
    }

    if(cmd.hasOption("available-types")) {
        val category = cmd.getOptionValue("available-types", "servers")
        println("Available types for $category: ${services.filter { it.category == category }.distinctBy { it.type }.joinToString(", "){ it.type }}")
        return
    }

    if(cmd.hasOption("all")) {
        if(App.env["NO_INPUT"] == null) {
            print("Building all jars... Do you want to continue? (y/n): ")
            if(readlnOrNull()?.lowercase() != "y") {
                println("Aborting...")
                return
            }
        }

        services.forEach {
            Thread {
                it.loadDatabase()
                it.buildAll()
                it.saveDatabase()
            }.start()
        }
        return
    }

    val category = cmd.getOptionValue("category")
    val type = cmd.getOptionValue("type")
    val version = cmd.getOptionValue("version", "all")

    val service = services.firstOrNull { it.category == category && it.type == type } ?: run {
        println("Unknown service $category $type")
        return
    }

    service.loadDatabase()
    if(version == "all") {
        service.buildAll()
    } else if(version.contains(";")) {
        service.buildAll(version.split(";").toTypedArray())
    } else {
        service.build(version)
    }
    service.saveDatabase()
}

fun options() = Options().apply {
    addOption(
        Option.builder("a")
            .longOpt("all")
            .desc("Builds all jars")
            .build()
    )

    addOption(
        Option.builder("c")
            .longOpt("category")
            .desc("The category of the jar to build (servers, vanilla, proxies, modded, etc.)")
            .hasArg()
            .build()
    )

    addOption(
        Option.builder("t")
            .longOpt("type")
            .desc("The type of jar to build (vanilla, paper, etc.)")
            .hasArg()
            .build()
    )

    addOption(
        Option.builder("v")
            .longOpt("version")
            .desc("The version of the jar to build (1.16.5, 1.17.1, etc.) You can specify multiple versions like this: '1.12;1.13;1.14' (see how it's separated by a semicolon)")
            .hasArg()
            .build()
    )

    addOption(
        Option.builder("h")
            .longOpt("help")
            .desc("Prints this help message")
            .build()
    )

    addOption(
        Option.builder("at")
            .longOpt("available-types")
            .hasArg()
            .optionalArg(true)
            .desc("Prints the available types for a given category.")
            .build()
    )
}