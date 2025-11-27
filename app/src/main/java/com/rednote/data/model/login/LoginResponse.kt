package com.rednote.data.model.login

import com.google.gson.annotations.SerializedName
import com.rednote.data.model.UserInfo

data class LoginResponse (
    @SerializedName("userInfoVO")
    val userInfo: UserInfo,
    val token: String
)