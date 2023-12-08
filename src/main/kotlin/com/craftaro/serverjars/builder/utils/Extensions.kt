package com.craftaro.serverjars.builder.utils

import com.google.gson.JsonParser
import java.net.URL

fun URL.asJson() = JsonParser.parseString(readText())