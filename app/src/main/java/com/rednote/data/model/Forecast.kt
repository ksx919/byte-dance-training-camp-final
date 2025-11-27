package com.rednote.data.model

data class Forecast(
    val city: String,
    val adcode: String,
    val casts: List<Cast>
)
