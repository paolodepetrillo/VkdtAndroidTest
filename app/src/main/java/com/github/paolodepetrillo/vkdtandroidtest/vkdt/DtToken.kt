package com.github.paolodepetrillo.vkdtandroidtest.vkdt

class DtToken(private val s: String) {
    override fun toString(): String {
        return s
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DtToken

        if (s != other.s) return false

        return true
    }

    override fun hashCode(): Int {
        return s.hashCode()
    }

    val token: Long

    init {
        if (s.isEmpty() || s.length > 8) {
            throw RuntimeException("Invalid token")
        }
        var t = 0L
        s.forEachIndexed { index, c ->
            t += c.code.toLong() shl (8 * index)
        }
        token = t
    }
}