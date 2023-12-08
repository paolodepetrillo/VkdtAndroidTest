package com.github.paolodepetrillo.vkdtandroidtest.vkdt

@JvmInline
value class DtToken(val token: Long) {
    constructor(s: String) : this(fromString(s))

    override fun toString(): String {
        var v = token
        val s = StringBuilder(8)
        while (v > 0) {
            s.append((v % 256).toInt().toChar())
            v /= 256
        }
        return s.toString()
    }

    companion object {
        private fun fromString(s: String): Long {
            if (s.isEmpty() || s.length > 8) {
                throw RuntimeException("Invalid token")
            }
            var t = 0L
            s.forEachIndexed { index, c ->
                t += c.code.toLong() shl (8 * index)
            }
            return t
        }
    }
}