package com.rednote.data.model.weather

data class Cast(
    val date: String,         // 日期 "2023-11-25"
    val week: String,         // 星期 "6"
    val dayweather: String,   // 白天天气 "晴"
    val nightweather: String, // 晚上天气
    val daytemp: String,      // 白天温度
    val nighttemp: String,    // 晚上温度
    val daywind: String,
    val daypower: String
)