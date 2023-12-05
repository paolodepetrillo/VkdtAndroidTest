package com.github.paolodepetrillo.vkdtandroidtest

import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.VkdtBase
import com.github.paolodepetrillo.vkdtandroidtest.vkdt.VkdtLib
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vkdtBase = VkdtBase(this)
        val vkdtLib = VkdtLib(vkdtBase)
        val graph = vkdtLib.newGraph()
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
        Column {
            Button(onClick = {lifecycleScope.launch {loadImage()}}) {
                Text("Load Image")
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
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameColumn)
                }
            }
        }
    }

    companion object {
        val permissions = listOf(android.Manifest.permission.READ_MEDIA_IMAGES)
    }
}
