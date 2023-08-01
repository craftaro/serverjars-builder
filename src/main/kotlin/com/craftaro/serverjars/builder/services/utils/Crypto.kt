package com.craftaro.serverjars.builder.services.utils

import java.security.MessageDigest

object Crypto {

    private val sha256 = MessageDigest.getInstance("SHA-256")

    fun sha256(bytes: ByteArray): ByteArray =
        sha256.digest(bytes)


    fun toString(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }
}