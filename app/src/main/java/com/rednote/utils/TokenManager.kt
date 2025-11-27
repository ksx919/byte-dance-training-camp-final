package com.rednote.utils

import com.tencent.mmkv.MMKV

object TokenManager {

    // 获取默认的 MMKV 实例 (可增加加密密钥)
    private val kv = MMKV.defaultMMKV()

    private const val KEY_TOKEN = "jwt_token"

    /**
     * 保存 Token
     */
    fun saveToken(token: String) {
        kv.encode(KEY_TOKEN, token)
    }

    /**
     * 获取 Token
     * @return 如果没有，返回 null
     */
    fun getToken(): String? {
        return kv.decodeString(KEY_TOKEN, null)
    }

    /**
     * 是否已登录
     */
    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    /**
     * 退出登录（清除 Token）
     */
    fun clearToken() {
        kv.removeValueForKey(KEY_TOKEN)
    }
}