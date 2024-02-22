package com.craftaro.serverjars.builder

import com.craftaro.serverjars.builder.jars.bedrock.PocketMineService
import com.craftaro.serverjars.builder.jars.modded.*
import com.craftaro.serverjars.builder.jars.proxies.BungeeService
import com.craftaro.serverjars.builder.jars.proxies.VelocityService
import com.craftaro.serverjars.builder.jars.proxies.WaterfallService
import com.craftaro.serverjars.builder.jars.servers.*
import com.craftaro.serverjars.builder.jars.vanilla.SnapshotService
import com.craftaro.serverjars.builder.jars.vanilla.VanillaService
import com.craftaro.serverjars.builder.utils.EnvironmentUtils
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options

object App {
    val env = EnvironmentUtils()

    fun services() = listOf(
        // Bedrock
        PocketMineService,

        // Modded
        BannerService,
        CatServerService,
        FabricService,
        ForgeService,
        MagmaService,
        MohistService,

        // Proxies
        BungeeService,
        VelocityService,
        WaterfallService,

        // Servers
        PaperService,
        FoliaService,
        PurpurService,
        SpongeService,
        PufferfishService,

        // Vanilla
        VanillaService,
        SnapshotService,
    )
}

fun main(args: Array<out String>){
    val options = options()

    val parser = DefaultParser()
    val cmd = parser.parse(options, args)

    val services = App.services()

    if(cmd.options.isEmpty() || cmd.hasOption("help")) {
        println("Usage: java -jar ServerJarsBuilder.jar [options]")
        println("Options:")
        options.options.forEach {
            println("\t-${it.opt} --${it.longOpt}\t${it.description}")
        }

        println("\nAvailable categories: ${services.distinctBy { it.type }.joinToString(", "){ it.type }}")
        return
    }

    if(cmd.hasOption("available-types")) {
        val category = cmd.getOptionValue("available-types", "servers")
        println("Available types for $category: ${services.filter { it.type == category }.distinctBy { it.category }.joinToString(", "){ it.category }}")
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

        val excluded = cmd.getOptionValue("exclude", "").split(";")

        services.filter { !excluded.contains(it.baseDirectory) }.forEach {
            Thread {
                it.loadDatabase()
                it.buildAll()
                it.saveDatabase()
            }.start()
        }
        return
    }

    val category = cmd.getOptionValue("category")
    val type = cmd.getOptionValue("type", "all")
    val version = cmd.getOptionValue("version", "all")
    val servicesToProcess = if(type.lowercase() == "all") {
        services.filter { it.type == category }
    } else {
        services.filter { it.type == category && it.category == type }
    }

    if(servicesToProcess.isEmpty()) {
        println("No services found for category $category and type $type")
        return
    }

    servicesToProcess.forEach { service ->
        Thread {
            try {
                println("Processing ${service.baseDirectory}...")
                service.loadDatabase()
                service.buildAll(if(version.contains(";")) {
                    version.split(";").toTypedArray()
                } else {
                    arrayOf(version)
                })
                service.saveDatabase()
            } catch (e: Exception) {
                println("An error occurred while processing ${service.baseDirectory}: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }
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
            .desc("The type of jar to build (vanilla, paper, etc.). Defaults to all")
            .hasArg()
            .optionalArg(true)
            .build()
    )

    addOption(
        Option.builder("v")
            .longOpt("version")
            .desc("The version of the jar to build (1.16.5, 1.17.1, etc.) You can specify multiple versions like this: '1.12;1.13;1.14' (see how it's separated by a semicolon). Defaults to all")
            .hasArg()
            .optionalArg(true)
            .build()
    )

    addOption(
        Option.builder("h")
            .longOpt("help")
            .desc("Prints this help message")
            .optionalArg(true)
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

    addOption(
        Option.builder("e")
            .longOpt("exclude")
            .hasArg()
            .optionalArg(true)
            .desc("Excludes a software build from the process (only works with --all). Example: --exclude servers/paper;servers/purpur")
            .build()
    )
}