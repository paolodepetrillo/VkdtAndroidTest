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

    fun setParam(module: DtModuleId, param: String, value: String) {
        val err = setParamString(
            nativeGraph,
            module.name.token,
            module.instance.token,
            DtToken(param).token,
            value
        );
        if (err != 0) {
            throw RuntimeException("Error $err setting param string for $module")
        }
    }

    fun doTestExport(outPath: String) {
        testExport(nativeGraph, outPath);
    }

    companion object {
        @JvmStatic
        private external fun initGraph(): Long
        @JvmStatic
        private external fun cleanupGraph(nativeGraph: Long)
        @JvmStatic
        private external fun loadConfigLines(nativeGraph: Long, lines: Array<String>): Int
        @JvmStatic
        private external fun setParamString(
            nativeGraph: Long,
            name: Long,
            inst: Long,
            param: Long,
            value: String
        ): Int
        @JvmStatic
        private external fun testExport(nativeGraph: Long, outPath: String)
    }
}