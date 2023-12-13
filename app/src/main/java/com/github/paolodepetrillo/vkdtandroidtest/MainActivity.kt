package com.github.paolodepetrillo.vkdtandroidtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.getImageList()

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
        val openSelectDialog = remember { mutableStateOf(false) }
        Column {
            Button(onClick = { lifecycleScope.launch {
                openSelectDialog.value = true
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
        if (openSelectDialog.value) {
            SelectImageDialog(onDismiss = { openSelectDialog.value = false })
        }
    }

    @Composable
    private fun SelectImageDialog(onDismiss: () -> Unit) {
        val selected: MutableState<ImageFileInfo?> = remember { mutableStateOf(null) }
        val onItemClick = { item: ImageFileInfo -> selected.value = item }
        Dialog(onDismissRequest = onDismiss) {
            Card(Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp),
                shape = RoundedCornerShape(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = viewModel.imageList.value, key = { it.dataFile }) {
                            Text(it.name, modifier = Modifier
                                .clickable { onItemClick(it) }
                                .background(if (selected.value == it) MaterialTheme.colorScheme.secondary else Color.Transparent)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = { onDismiss() }) {
                            Text("Cancel")
                        }
                        Button(
                            enabled = selected.value != null,
                            onClick = {
                                selected.value?.let {
                                    viewModel.loadImage(it)
                                    onDismiss()
                                }
                            }) {
                            Text("Ok")
                        }
                    }
                }
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

    companion object {
        val permissions = listOf(android.Manifest.permission.READ_MEDIA_IMAGES)
    }
}
