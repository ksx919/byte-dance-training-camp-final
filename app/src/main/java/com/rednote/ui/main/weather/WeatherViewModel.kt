package com.rednote.ui.main.weather

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rednote.BuildConfig
import com.rednote.data.model.weather.Cast
import com.rednote.data.model.weather.LiveWeather
import com.rednote.data.model.weather.WeatherResponse
import com.rednote.data.repository.WeatherRepository
import com.rednote.ui.base.BaseViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class WeatherViewModel : BaseViewModel() {
    // 实况天气数据
    val liveWeatherData = MutableLiveData<LiveWeather?>()

    // 预测列表数据
    val forecastData = MutableLiveData<List<Cast>?>()

    // 城市名称
    val cityName = MutableLiveData<String>()

    // 错误信息 (使用父类的 showToast 方法)
    val errorMsg = MutableLiveData<String>()

    // 上次更新时间戳（用于缓存控制）
    var lastUpdateTime: Long = 0L

    private val repository = WeatherRepository()
    private val webApiKey = BuildConfig.AMAP_WEB_API_KEY

    // 是否正在加载中
    private var isFetching = false

    // 检查是否需要更新（10分钟内不重复请求）
    fun shouldUpdate(): Boolean {
        val currentTime = System.currentTimeMillis()
        val tenMinutesInMillis = 10 * 60 * 1000L
        return (currentTime - lastUpdateTime) > tenMinutesInMillis || liveWeatherData.value == null
    }

    // 标记已更新
    fun markUpdated() {
        lastUpdateTime = System.currentTimeMillis()
    }

    fun fetchWeather(adCode: String, forceRefresh: Boolean = false) {
        if (isFetching) return // 防止并发请求

        // 如果不是强制刷新，且不需要更新，则跳过
        if (!forceRefresh && !shouldUpdate()) return

        isFetching = true
        viewModelScope.launch {
            try {
                val liveDeferred = async {
                    repository.getWeather(adCode, webApiKey, "base")
                }
                val forecastDeferred = async {
                    repository.getWeather(adCode, webApiKey, "all")
                }

                handleLiveResponse(liveDeferred.await())
                handleForecastResponse(forecastDeferred.await())
            } catch (_: Exception) {
                postError("天气数据获取失败，请稍后重试")
            } finally {
                isFetching = false
            }
        }
    }

    private fun handleLiveResponse(response: WeatherResponse) {
        if (response.status == "1") {
            val live = response.lives?.firstOrNull()
            if (live != null) {
                liveWeatherData.postValue(live)
                markUpdated()
            } else {
                postError("暂无实况天气数据")
            }
        } else {
            postError("实况天气加载失败，请重试")
        }
    }

    private fun handleForecastResponse(response: WeatherResponse) {
        if (response.status == "1") {
            val casts = response.forecasts?.firstOrNull()?.casts
            if (!casts.isNullOrEmpty()) {
                forecastData.postValue(casts)
            } else {
                postError("暂无天气预报数据")
            }
        } else {
            postError("天气预报加载失败，请重试")
        }
    }

    private fun postError(message: String) {
        errorMsg.postValue(message)
        showToast(message)
    }
}
