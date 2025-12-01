package com.rednote.data.model

data class Draft(
    val id: Int = 1, // Single draft for now, fixed ID
    val title: String,
    val content: String,
    val imagesJson: String, // Store list of URIs as JSON string
    val updatedAt: Long
)
