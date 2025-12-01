package com.rednote.data.model

data class Draft(
    val id: Int = 1,
    val title: String,
    val content: String,
    val imagesJson: String,
    val updatedAt: Long
)
