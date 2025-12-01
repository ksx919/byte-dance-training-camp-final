package com.rednote.data.api

import com.rednote.data.model.BaseResponse
import com.rednote.data.model.login.LoginRequest
import com.rednote.data.model.login.LoginResponse
import com.rednote.data.model.login.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface LoginApiService {
    @Headers("No-Auth: true")
    @POST("users/login")
    suspend fun login(@Body loginRequest: LoginRequest): BaseResponse<LoginResponse>

    @Headers("No-Auth: true")
    @POST("users/register")
    suspend fun register(@Body registerRequest: RegisterRequest): BaseResponse<Boolean>
}