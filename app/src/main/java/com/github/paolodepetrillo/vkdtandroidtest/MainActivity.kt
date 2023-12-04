package com.github.paolodepetrillo.vkdtandroidtest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.github.paolodepetrillo.vkdtandroidtest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        val v = VkdtBase(this)
        binding.sampleText.text = stringFromJNI(v.root.absolutePath)

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