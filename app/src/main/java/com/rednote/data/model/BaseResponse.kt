package com.rednote.data.model

data class BaseResponse<T>(
    val code: Int,      // 状态码，比如 200 表示成功
    val msg: String?,   // 提示信息
    val data: T?        // 具体数据
)