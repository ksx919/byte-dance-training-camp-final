package com.rednote.utils

import com.google.gson.Gson
import com.rednote.data.model.UserInfo
import com.tencent.mmkv.MMKV

object UserManager {

    private val kv = MMKV.defaultMMKV()
    private val gson = Gson()
    private const val KEY_USER_INFO = "user_info"

    // --- 1. 内存缓存 (Memory Cache) ---
    // App 运行期间，直接从这个变量拿数据，速度最快
    @Volatile
    private var _cachedUser: UserInfo? = null

    /**
     * 获取用户信息
     * 逻辑：先看内存有没有 -> 没有就去 MMKV 找 -> 还没有就是没登录
     */
    @Synchronized
    fun getUser(): UserInfo? {
        // 1. 命中内存缓存
        if (_cachedUser != null) {
            return _cachedUser
        }

        // 2. 读取磁盘 (JSON String)
        val json = kv.decodeString(KEY_USER_INFO)
        if (!json.isNullOrEmpty()) {
            try {
                // 3. 反序列化为对象，并回填内存缓存
                _cachedUser = gson.fromJson(json, UserInfo::class.java)
                return _cachedUser
            } catch (e: Exception) {
                // 异常处理：比如数据结构变了导致解析失败，干脆清空
                clearUser()
            }
        }
        return null
    }

    /**
     * 保存用户信息 (登录成功后调用)
     */
    @Synchronized
    fun saveUser(user: UserInfo) {
        // 1. 更新内存
        _cachedUser = user
        // 2. 序列化并存入磁盘
        val json = gson.toJson(user)
        kv.encode(KEY_USER_INFO, json)
    }

    /**
     * 退出登录
     */
    @Synchronized
    fun logout() {
        // 清空内存和磁盘
        _cachedUser = null
        kv.removeValueForKey(KEY_USER_INFO)
        // 别忘了把 Token 也清了
        TokenManager.clearToken()
    }

    /**
     * 内部私有方法：仅清除用户信息（内存+磁盘）
     */
    @Synchronized
    private fun clearUser() {
        _cachedUser = null
        kv.removeValueForKey(KEY_USER_INFO)
    }

    /**
     * 快速判断是否登录
     */
    fun isLogin(): Boolean {
        return getUser() != null
    }

    // 获取用户ID的便捷方法
    fun getUserId(): Long? {
        return getUser()?.id
    }
}