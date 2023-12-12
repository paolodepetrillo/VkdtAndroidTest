package com.github.paolodepetrillo.vkdtandroidtest

import android.graphics.Bitmap
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.lifecycleScope
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.DtModuleId
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.DtToken
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.VkdtBase
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.VkdtGraph
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.VkdtLib
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class MainActivity : ComponentActivity() {
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
        var bmp by remember { mutableStateOf<Bitmap?>(null) }
        Column {
            Button(onClick = { lifecycleScope.launch {
                loadImage()
                bmp = graph?.getBitmap(DtToken("main"))
            } }) {
                Text("Load Image")
            }
            bmp?.let {
                Image(it.asImageBitmap(), null)
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
                        processRaw(data)
                        break
                    }
                }
            }
        }
    }

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

    companion object {
        val permissions = listOf(android.Manifest.permission.READ_MEDIA_IMAGES)
    }
}
