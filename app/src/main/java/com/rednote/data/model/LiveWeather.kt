package com.rednote.data.model

data class LiveWeather(
    val province: String,
    val city: String,
    val adcode: String,
    val weather: String,      // 天气现象（晴、多云等）
    val temperature: String,  // 实时气温
    val winddirection: String,
    val windpower: String,
    val humidity: String,
    val reporttime: String
)
