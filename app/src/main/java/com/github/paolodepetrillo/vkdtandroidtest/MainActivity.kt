package com.github.paolodepetrillo.vkdtandroidtest

import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.DtModuleId
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.DtToken
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.VkdtBase
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.VkdtGraph
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.VkdtLib
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    var graph: VkdtGraph? = null

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val multiplePermissionsState = rememberMultiplePermissionsState(permissions)
                if (multiplePermissionsState.allPermissionsGranted) {
                    MainUi()
                } else {
                    RequestPermissions(multiplePermissionsState)
                }
            }
        }
    }

    @Composable
    private fun MainUi() {
        val mainBitmap by viewModel.mainBitmap.collectAsStateWithLifecycle(null)
        var exposure by remember { mutableFloatStateOf(0f) }
        Column {
            Button(onClick = { lifecycleScope.launch {
                loadImage()
            } }) {
                Text("Load Image")
            }
            Text("Exposure = $exposure")
            Slider(
                value = exposure,
                onValueChange = { exposure = it },
                onValueChangeFinished = { viewModel.setExposure(exposure) },
                valueRange = -5f..5f
            )
            mainBitmap?.let {
                Image(it.bitmap.asImageBitmap(), null)
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    private fun RequestPermissions(multiplePermissionsState: MultiplePermissionsState) {
        if (multiplePermissionsState.shouldShowRationale) {
            Column {
                Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                    Text("Request Permissions")
                }
            }

        } else {
            LaunchedEffect(true) {
                multiplePermissionsState.launchMultiplePermissionRequest()
            }
        }
    }

    private suspend fun loadImage() {
        withContext(Dispatchers.IO) {
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DISPLAY_NAME
            )
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
            val query = contentResolver.query(
                collection,
                projection,
                null, arrayOf(),
                sortOrder
            )
            query?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    if (name.endsWith(".dng")) {
                        val data = cursor.getString(dataColumn)
                        viewModel.selectFile(data, filesDir)
                        break
                    }
                }
            }
        }
    }

    /*
    fun processRaw(rawFileName: String) {
        val vkdtBase = VkdtBase(this)
        val vkdtLib = VkdtLib(vkdtBase)
        graph = vkdtLib.newGraph()
        val gf = File(filesDir, "vkdtbase/bin/default-darkroom.i-raw")
        val lines = gf.readText(Charsets.UTF_8).lines()
        graph!!.loadConfigLines(lines)
        graph!!.setParam(DtModuleId("i-raw", "main"), "filename", rawFileName)
        graph!!.doTestExport(File(filesDir, "out.jpg").absolutePath)
    }
     */

    companion object {
        val permissions = listOf(android.Manifest.permission.READ_MEDIA_IMAGES)
    }
}
