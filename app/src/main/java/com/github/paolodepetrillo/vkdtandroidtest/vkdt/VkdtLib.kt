package com.github.paolodepetrillo.vkdtandroidtest.vkdt

class VkdtLib(vkdtBase: VkdtBase) {
    private val graphs: MutableSet<VkdtGraph> = mutableSetOf()

    init {
        initVkdtLib(vkdtBase.root.absolutePath).let {
            if (it != 0) {
                throw RuntimeException("Failed to initialize vkdt lib")
            }
        }
    }

    fun close() {
        graphs.forEach {
            it.close()
        }
        closeVkdtLib()
    }

    fun newGraph(): VkdtGraph {
        val graph = VkdtGraph()
        graphs.add(graph)
        return graph
    }

    fun closeGraph(graph: VkdtGraph) {
        graph.close()
        graphs.remove(graph)
    }

    companion object {
        @JvmStatic private external fun initVkdtLib(basePath: String): Int
        @JvmStatic private external fun closeVkdtLib()
        init {
            System.loadLibrary("vkdtandroidtest")
        }
    }
}