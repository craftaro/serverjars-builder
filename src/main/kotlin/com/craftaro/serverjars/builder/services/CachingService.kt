package com.craftaro.serverjars.builder.services

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.time.Instant

object CachingService {

    private val expires = LinkedHashMap<String, Instant>()
    private val cache = LinkedHashMap<String, JsonObject>()

    fun init(){
        // Load from disk into memory
        val contents = File("cache.json").apply {
            if(!exists()) {
                createNewFile()
                writeText(JsonObject().apply {
                    add("cache", JsonObject())
                    add("expires", JsonObject())
                }.toString())
            }
        }.readText()
        val json = JsonParser.parseString(contents).asJsonObject

        json.getAsJsonObject("cache").entrySet().forEach { (key, value) ->
            cache[key] = value.asJsonObject
        }

        json.getAsJsonObject("expires").entrySet().forEach { (key, value) ->
            expires[key] = Instant.parse(value.asString)
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            // Save everything to cache
            val toSave = JsonObject()
            val cacheJson = JsonObject()
            val expiresJson = JsonObject()

            cache.forEach { (key, value) ->
                cacheJson.add(key, value)
            }

            expires.forEach { (key, value) ->
                expiresJson.addProperty(key, value.toString())
            }

            toSave.add("cache", cacheJson)
            toSave.add("expires", expiresJson)

            File("cache.json").writeText(toSave.toString())
        })
    }

    fun remember(key: String, ttl: Instant, value: () -> JsonObject): JsonObject {
        if(Instant.now().isAfter(expires[key] ?: Instant.MIN)) {
            cache.remove(key)
            expires.remove(key)
            expires[key] = ttl
        }

        return cache.computeIfAbsent(key) {
            value()
        }
    }


    fun rememberMillis(key: String, ttl: Long, value: () -> JsonObject): JsonObject {
        return remember(key, Instant.now().plusMillis(ttl), value)
    }

    fun rememberSeconds(key: String, ttl: Int, value: () -> JsonObject): JsonObject {
        return remember(key, Instant.now().plusSeconds(ttl.toLong()), value)
    }

    fun rememberMinutes(key: String, ttl: Int = 1, value: () -> JsonObject): JsonObject {
        return rememberSeconds(key, ttl * 60, value)
    }

    fun rememberHours(key: String, ttl: Int = 1, value: () -> JsonObject): JsonObject {
        return rememberMinutes(key, ttl * 60, value)
    }

    fun rememberDays(key: String, ttl: Int = 1, value: () -> JsonObject): JsonObject {
        return rememberHours(key, ttl * 24, value)
    }

    fun rememberForever(key: String, value: () -> JsonObject): JsonObject {
        return cache.computeIfAbsent(key) {
            value()
        }
    }
}