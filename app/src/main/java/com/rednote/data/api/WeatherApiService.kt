package com.rednote.data.api

import com.rednote.data.model.weather.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("v3/weather/weatherInfo")
    suspend fun getWeatherInfo(
        @Query("city") city: String,
        @Query("key") key: String,
        @Query("extensions") extensions: String
    ): WeatherResponse
}
