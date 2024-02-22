package com.craftaro.serverjars.builder.utils

import java.io.File

class EnvironmentUtils: HashMap<String, String>() {

    init {
        putAll(System.getenv())
        val dotEnv = File(".env")
        if(dotEnv.exists()) {
            println("Loading from '${dotEnv.absolutePath}'...")
            dotEnv.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.forEach {
                val (key, value) = it.split("=")
                put(key, value)
                println("Loaded $key from '.env' with value '${if(key.contains("_KEY")) value.replace(Regex("."), "*") else value}'")
            }
        }
    }
}