package com.rednote.data.model

data class UserInfo(
    val id: Long,
    val email: String,
    val nickname: String,
    val avatarUrl: String,
    val bio: String
)