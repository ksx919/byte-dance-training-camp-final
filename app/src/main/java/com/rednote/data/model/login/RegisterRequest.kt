package com.rednote.data.model.login

data class RegisterRequest(
    val email: String,
    val password: String,
    val nickname: String
)
