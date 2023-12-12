package com.github.paolodepetrillo.vkdtandroidtest.vkdt

data class PixelSize(
    val wd: Int,
    val ht: Int
) {
    override fun toString(): String {
        return "${wd}x${ht}"
    }
}
