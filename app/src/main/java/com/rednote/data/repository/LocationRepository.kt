package com.rednote.data.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption

class LocationRepository {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var locationClient: AMapLocationClient? = null

    fun startSingleLocation(
        context: Context,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            if (locationClient == null) {
                locationClient = AMapLocationClient(context.applicationContext)
            }
        } catch (e: Exception) {
            postToMain { onError("定位初始化失败") }
            return
        }

        val option = AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            isNeedAddress = true
            isOnceLocation = true
        }

        locationClient?.apply {
            setLocationOption(option)
            setLocationListener { location ->
                if (location != null) {
                    if (location.errorCode == 0) {
                        val adCode = location.adCode
                        if (!adCode.isNullOrEmpty()) {
                            stopLocation()
                            postToMain { onSuccess(adCode) }
                        } else {
                            postToMain { onError("定位成功但无法获取城市信息") }
                        }
                    } else {
                        postToMain { onError("定位失败，请检查权限设置") }
                    }
                } else {
                    postToMain { onError("定位结果为空") }
                }
            }
            startLocation()
        } ?: postToMain { onError("定位客户端不可用") }
    }

    fun stop() {
        locationClient?.stopLocation()
    }

    fun release() {
        locationClient?.onDestroy()
        locationClient = null
    }

    private fun postToMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post { block() }
        }
    }
}