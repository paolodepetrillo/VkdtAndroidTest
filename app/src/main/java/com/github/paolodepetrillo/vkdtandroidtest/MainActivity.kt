package com.github.paolodepetrillo.vkdtandroidtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vkdtBase = VkdtBase(this)
        val sampleText = stringFromJNI(vkdtBase.root.absolutePath)
        setContent {
            MaterialTheme {
                Text(sampleText)
            }
        }
    }

    /**
     * A native method that is implemented by the 'vkdtandroidtest' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(basePath: String): String

    companion object {
        // Used to load the 'vkdtandroidtest' library on application startup.
        init {
            System.loadLibrary("vkdtandroidtest")
        }
    }
}