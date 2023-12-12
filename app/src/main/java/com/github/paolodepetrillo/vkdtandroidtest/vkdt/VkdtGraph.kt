package com.github.paolodepetrillo.vkdtandroidtest.vkdt

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.util.Log

class VkdtGraph internal constructor() {
    private val nativeGraph: Long = initGraph()
    private val bitmaps: MutableMap<DtToken, Bitmap> = mutableMapOf()
    private var pendingRunFlags: Long = 0

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
        val runFlags = LongArray(1)
        val err = setParamString(
            nativeGraph,
            module.name.token,
            module.instance.token,
            DtToken(param).token,
            value,
            runFlags
        )
        if (err != 0) {
            throw RuntimeException("Error $err setting param string for $module")
        }
        pendingRunFlags = pendingRunFlags or runFlags[0]
    }

    fun setParam(module: DtModuleId, param: String, index: Int, value: Float) {
        val runFlags = LongArray(1)
        val err = setParamFloat(
            nativeGraph,
            module.name.token,
            module.instance.token,
            DtToken(param).token,
            index,
            value,
            runFlags
        )
        pendingRunFlags = pendingRunFlags or runFlags[0]
    }

    fun doTestExport(outPath: String): Bitmap {
        //testExport(nativeGraph, outPath);
        replaceDisplayWithCback(nativeGraph)
        val ps = getOutputSize()
        val mainBitmap = getBitmapForOutput(DtToken("main"), ps)
        runGraph(nativeGraph, RUN_ALL, mainBitmap)
        return mainBitmap
        //val ps = getOutputSize()
        //getBitmapForOutput(DtToken("main"), ps)
        //runGraph(nativeGraph, RUN_ALL, mainBitmap);
    }

    fun runGraphIfNeeded(): Bitmap {
        pendingRunFlags.let {
            Log.i("vkdt", "Run graph with pending flags $pendingRunFlags")
            if (it > 0) {
                val flags = it or RUN_DOWNLOAD_SINK or RUN_WAIT_DONE or RUN_ALL
                Log.i("vkdt", "Run graph with flags $flags")
                val ps = getOutputSize()
                val mainBitmap = getBitmapForOutput(DtToken("main"), ps)
                runGraph(nativeGraph, flags, mainBitmap)
                pendingRunFlags = 0
                return mainBitmap
            } else {
                return getBitmap(DtToken("main")) ?: getBitmapForOutput(DtToken("main"), PixelSize(100, 100))
            }
        }
    }

    private fun getOutputSize(): PixelSize {
        val wh = IntArray(2)
        val err = runGraphForRoi(nativeGraph, wh)
        return PixelSize(wh[0], wh[1])
    }

    private fun getBitmapForOutput(inst: DtToken, size: PixelSize): Bitmap {
        bitmaps[inst]?.let {
            if (it.width == size.wd && it.height == size.ht) {
                return it
            }
        }

        val colorSpace = ColorSpace.Rgb(
            "Rec2020 Linear",
            (ColorSpace.get(ColorSpace.Named.BT2020) as ColorSpace.Rgb).primaries,
            ColorSpace.ILLUMINANT_D65,
            1.0)
        val bitmap = Bitmap.createBitmap(size.wd, size.ht, Bitmap.Config.RGBA_F16, true, colorSpace)
        bitmaps[inst] = bitmap
        return bitmap
    }

    fun getBitmap(inst: DtToken): Bitmap? {
        return bitmaps[inst]
    }

    companion object {
        const val RUN_ALL = 0xffffffffL
        const val RUN_DOWNLOAD_SINK = 0x20L
        const val RUN_WAIT_DONE = 0x40L

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
            value: String,
            runFlags: LongArray
        ): Int
        @JvmStatic
        private external fun setParamFloat(
            nativeGraph: Long,
            name: Long,
            inst: Long,
            param: Long,
            index: Int,
            newValue: Float,
            runFlags: LongArray
        ): Int
        @JvmStatic
        private external fun testExport(nativeGraph: Long, outPath: String)
        @JvmStatic
        private external fun replaceDisplayWithCback(nativeGraph: Long): Int
        @JvmStatic
        private external fun runGraph(nativeGraph: Long, runFlags: Long, mainBitmap: Bitmap?): Int
        @JvmStatic
        private external fun runGraphForRoi(nativeGraph: Long, size: IntArray): Int
    }
}