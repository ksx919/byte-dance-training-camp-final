package com.rednote.data.api

import com.rednote.data.model.BaseResponse
import com.rednote.data.model.UserInfo
import retrofit2.http.Body
import retrofit2.http.PUT

interface UserApiService {

    @PUT("users/update")
    suspend fun updateUserInfo(@Body user: UserInfo): BaseResponse<Boolean>
}
