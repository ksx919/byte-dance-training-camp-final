package com.rednote.data.model.weather

data class Forecast(
    val city: String,
    val adcode: String,
    val casts: List<Cast>
)