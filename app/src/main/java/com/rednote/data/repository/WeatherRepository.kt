package com.rednote.data.repository

import com.rednote.data.model.weather.WeatherResponse
import com.rednote.data.api.WeatherClient
import com.rednote.data.api.WeatherApiService

class WeatherRepository {
    private val api = WeatherClient.create(WeatherApiService::class.java)

    suspend fun getWeather(city: String, key: String, extensions: String): WeatherResponse {
        return api.getWeatherInfo(city, key, extensions)
    }
}
