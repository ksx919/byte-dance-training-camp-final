package com.rednote.data.model.weather

data class WeatherResponse(
    val status: String,
    val lives: List<LiveWeather>?,
    val forecasts: List<Forecast>?
)