package com.github.paolodepetrillo.vkdtandroidtest

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.DtModuleId
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.VkdtGraph
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.VkdtLib
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(val vkdtLib: VkdtLib): ViewModel() {
    private val _rawFileName = MutableStateFlow<String?>(null)
    val rawFileName: StateFlow<String?> = _rawFileName

    private val _mainBitmap = MutableStateFlow<BitmapWrapper?>(null)
    val mainBitmap: StateFlow<BitmapWrapper?> = _mainBitmap

    private val graph: VkdtGraph = vkdtLib.newGraph()

    fun selectFile(name: String, filesDir: File) {
        Log.i("vkdt", "Processing $name")
        _rawFileName.value = name
        viewModelScope.launch(Dispatchers.IO) {
            val gf = File(filesDir, "vkdtbase/bin/default-darkroom.i-raw")
            val lines = gf.readText(Charsets.UTF_8).lines()
            graph.loadConfigLines(lines)
            graph.setParam(DtModuleId("i-raw", "main"), "filename", name)
            val bmp = graph.doTestExport("")
            _mainBitmap.value = BitmapWrapper(bmp)
        }
    }

    fun setExposure(exposure: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.i("vkdt", "Set exposure $exposure")
            graph.setParam(DtModuleId("colour", "01"), "exposure", 0, exposure)
            val bmp = graph.runGraphIfNeeded()
            _mainBitmap.value = BitmapWrapper(bmp)
        }
    }

    override fun onCleared() {
        super.onCleared()
        vkdtLib.closeGraph(graph)
    }
}