package com.github.paolodepetrillo.vkdtandroidtest.vkdt

class VkdtGraph internal constructor() {
    private val nativeGraph: Long = initGraph()

    internal fun close() {
        cleanupGraph(nativeGraph)
    }

    companion object {
        @JvmStatic private external fun initGraph(): Long
        @JvmStatic private external fun cleanupGraph(nativeGraph: Long)
    }
}