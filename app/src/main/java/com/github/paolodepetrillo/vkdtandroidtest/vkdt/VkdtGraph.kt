package com.github.paolodepetrillo.vkdtandroidtest.vkdt

import android.graphics.Bitmap
import android.graphics.ColorSpace

class VkdtGraph internal constructor() {
    private val nativeGraph: Long = initGraph()
    private val bitmaps: MutableMap<DtToken, Bitmap> = mutableMapOf()

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
        )
        if (err != 0) {
            throw RuntimeException("Error $err setting param string for $module")
        }
    }

    fun doTestExport(outPath: String) {
        //testExport(nativeGraph, outPath);
        replaceDisplayWithCback(nativeGraph)
        runGraph(nativeGraph, null)

        val ps = getOutputSize()
        val mainBitmap = getBitmapForOutput(DtToken("main"), ps)
        runGraph(nativeGraph, mainBitmap);
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
        @JvmStatic
        private external fun replaceDisplayWithCback(nativeGraph: Long): Int
        @JvmStatic
        private external fun runGraph(nativeGraph: Long, mainBitmap: Bitmap?): Int
        @JvmStatic
        private external fun runGraphForRoi(nativeGraph: Long, size: IntArray): Int
    }
}