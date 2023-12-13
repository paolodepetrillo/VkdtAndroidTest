package com.github.paolodepetrillo.vkdtandroidtest

import android.app.Application
import android.provider.MediaStore
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
class MainViewModel @Inject constructor(
    private val application: Application,
    val vkdtLib: VkdtLib
) : ViewModel() {
    private val _rawFileName = MutableStateFlow<String?>(null)
    val rawFileName: StateFlow<String?> = _rawFileName

    private val _mainBitmap = MutableStateFlow<BitmapWrapper?>(null)
    val mainBitmap: StateFlow<BitmapWrapper?> = _mainBitmap

    private val _imageList = MutableStateFlow<List<ImageFileInfo>>(listOf())
    val imageList: StateFlow<List<ImageFileInfo>> = _imageList

    private val graph: VkdtGraph = vkdtLib.newGraph()

    fun getImageList() {
        viewModelScope.launch(Dispatchers.IO) {
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DISPLAY_NAME
            )
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
            val query = application.contentResolver.query(
                collection,
                projection,
                null, arrayOf(),
                sortOrder
            )
            query?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val list = mutableListOf<ImageFileInfo>()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    if (name.endsWith(".dng")) {
                        val data = cursor.getString(dataColumn)
                        list.add(ImageFileInfo(name, data))
                        if (list.size >= 100) {
                            break
                        }
                    }
                }
                _imageList.value = list
            }
        }
    }

    fun loadImage(image: ImageFileInfo) {
        val name = image.dataFile
        Log.i("vkdt", "Processing $name")
        _rawFileName.value = name
        viewModelScope.launch(Dispatchers.IO) {
            val gf = File(application.filesDir, "vkdtbase/bin/default-darkroom.i-raw")
            val lines = gf.readText(Charsets.UTF_8).lines()
            graph.loadConfigLines(lines)
            graph.setParam(DtModuleId("i-raw", "main"), "filename", name)
            val bmp = graph.runGraphIfNeeded()
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