package com.github.paolodepetrillo.vkdtandroidtest.vkdt

data class DtModuleId(
    val name: DtToken,
    val instance: DtToken
) {
    constructor(name: String, instance: String) : this(DtToken(name), DtToken(instance))
}