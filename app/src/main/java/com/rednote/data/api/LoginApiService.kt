package com.rednote.data.api

import com.rednote.data.model.BaseResponse
import com.rednote.data.model.login.LoginRequest
import com.rednote.data.model.login.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginApiService {
    @POST("users/login")
    suspend fun login(@Body loginRequest: LoginRequest): BaseResponse<LoginResponse>
}