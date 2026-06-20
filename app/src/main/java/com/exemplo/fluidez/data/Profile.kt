package com.exemplo.fluidez.data

data class Profile(
    val id: String,
    val name: String,
    val animationScale: Float, // 0f = instantâneo, 1f = padrão
    val batterySaver: Boolean
)
