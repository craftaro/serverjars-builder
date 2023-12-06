package com.craftaro.serverjars.builder.services.utils

import java.io.File

class EnvironmentUtils: HashMap<String, String>() {

    init {
        putAll(System.getenv())
        val dotEnv = File(".env")
        if(dotEnv.exists()) {
            dotEnv.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.forEach {
                val (key, value) = it.split("=")
                put(key, value)
            }
        }
    }
}