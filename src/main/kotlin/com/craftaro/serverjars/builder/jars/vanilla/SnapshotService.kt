package com.craftaro.serverjars.builder.jars.vanilla

object SnapshotService: MinecraftServiceBase() {

    override val releaseOnly: Boolean = false

    override val category: String = "snapshot"
}