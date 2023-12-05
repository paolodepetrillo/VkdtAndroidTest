package com.github.paolodepetrillo.vkdtandroidtest.vkdt

import android.content.Context
import java.io.File
import java.lang.RuntimeException
import java.util.zip.ZipInputStream

class VkdtBase(context: Context) {
    val root = File(context.filesDir, "vkdtbase")
    private val flagFile = File(root, "_unpacked_")

    init {
        // TODO Keep track of checksum of zip?
        //if (!flagFile.exists()) {
            unpack(context)
        //}
    }

    private fun unpack(context: Context) {
        if (root.exists()) {
            root.deleteRecursively()
        }
        context.assets.open("vkdtbase.zip").use { ins ->
            ZipInputStream(ins).use { zip ->
                generateSequence { zip.nextEntry }
                    .filterNot { it.isDirectory }
                    .forEach { entry ->
                        val dest = File(root, entry.name)
                        dest.parentFile?.let {
                            if (!it.exists()) {
                                if (!it.mkdirs()) {
                                    throw RuntimeException("Failed to create directory")
                                }
                            }
                        }
                        dest.outputStream().buffered().use { bos ->
                            val buf = ByteArray(4096)
                            while (zip.available() == 1) {
                                val size = zip.read(buf)
                                bos.write(buf, 0, size)
                            }
                        }
                    }
            }
        }
        flagFile.outputStream().use {
            it.write(0)
        }
    }
}