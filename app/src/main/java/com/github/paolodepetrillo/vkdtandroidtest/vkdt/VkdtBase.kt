package com.github.paolodepetrillo.vkdtandroidtest.vkdt

import android.content.Context
import com.github.paolodepetrillo.vkdtandroidtest.BuildConfig
import java.io.File
import java.lang.RuntimeException
import java.util.zip.ZipInputStream

class VkdtBase(context: Context) {
    private val root = File(context.filesDir, "vkdtbase")
    private val timestampFile = File(root, "vkdtbase_timestamp")

    val rootPath by lazy {
        if (timestampFile.exists()) {
            timestampFile.inputStream().use {
                val timestamp = it.reader().readText()
                if (timestamp != BuildConfig.BUILD_TIME) {
                    unpack(context)
                }
            }
        } else {
            unpack(context)
        }
        root.absolutePath
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
        timestampFile.outputStream().use { fos ->
            fos.writer().use {
                it.write(BuildConfig.BUILD_TIME)
            }
        }
    }
}