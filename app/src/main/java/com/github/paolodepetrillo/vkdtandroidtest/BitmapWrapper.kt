package com.github.paolodepetrillo.vkdtandroidtest

import android.graphics.Bitmap
import java.time.Instant

data class BitmapWrapper(val bitmap: Bitmap, val ts: Long) {
    constructor(bitmap: Bitmap) : this(bitmap, Instant.now().toEpochMilli())
}