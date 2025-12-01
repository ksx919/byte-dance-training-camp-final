package com.rednote.data.api

import com.rednote.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val noAuthHeader = originalRequest.header("No-Auth")
        if (noAuthHeader != null) {
            return chain.proceed(originalRequest.newBuilder().removeHeader("No-Auth").build())
        }

        val token = TokenManager.getToken()

        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            // .addHeader("device-type", "android")
            .build()

        return chain.proceed(newRequest)
    }
}