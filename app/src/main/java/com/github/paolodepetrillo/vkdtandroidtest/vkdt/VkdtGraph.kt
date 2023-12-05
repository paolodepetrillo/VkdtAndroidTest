package com.github.paolodepetrillo.vkdtandroidtest.vkdt

class VkdtGraph internal constructor() {
    private val nativeGraph: Long = initGraph()

    internal fun close() {
        cleanupGraph(nativeGraph)
    }

    fun loadConfigLines(lines: List<String>) {
        val err = loadConfigLines(nativeGraph, lines.toTypedArray())
        if (err != 0) {
            throw RuntimeException("Error loading config graph")
        }
    }

    companion object {
        @JvmStatic private external fun initGraph(): Long
        @JvmStatic private external fun cleanupGraph(nativeGraph: Long)
        @JvmStatic private external fun loadConfigLines(nativeGraph: Long, lines: Array<String>): Int
    }
}