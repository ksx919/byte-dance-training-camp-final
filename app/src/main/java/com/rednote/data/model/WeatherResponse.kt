package com.rednote.data.model

data class WeatherResponse(
    val status: String,
    val lives: List<LiveWeather>?,
    val forecasts: List<Forecast>?
)
