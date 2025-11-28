package com.rednote.data.api

import com.rednote.data.model.BaseResponse
import com.rednote.data.model.UserInfo
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface UserApiService {

    @PUT("users/update")
    suspend fun updateUserInfo(@Body user: UserInfo): BaseResponse<Boolean>

    @Multipart
    @POST("users/upload-avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): BaseResponse<String>
}
