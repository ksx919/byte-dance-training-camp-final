package com.rednote.ui.login

import android.util.Patterns
import com.rednote.data.api.RetrofitClient
import com.rednote.data.model.login.LoginRequest
import com.rednote.ui.base.BaseViewModel
import com.rednote.utils.TokenManager
import com.rednote.utils.UserManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class LoginViewModel : BaseViewModel() {

    private val _email = MutableStateFlow("")

    private val _password = MutableStateFlow("")

    private val _loginEvent = Channel<LoginUiEvent>(Channel.BUFFERED)
    val loginEvent = _loginEvent.receiveAsFlow()

    fun updateEmail(input: String) {
        _email.value = input
    }

    fun updatePassword(input: String) {
        _password.value = input
    }

    fun login() {
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value.trim()

        // 1. 基础校验
        if (currentEmail.isEmpty()) {
            showToast("请输入邮箱")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches()) {
            showToast("邮箱格式不正确")
            return
        }
        if (currentPassword.isEmpty()) {
            showToast("请输入密码")
            return
        }

        // 2. 发起请求 (使用 BaseViewModel 的 launchDataLoad)
        // 它会自动处理 showLoading() -> block() -> hideLoading()
        // 并且会自动 catch 异常并 toast 报错
        launchDataLoad {
            // 1. 准备参数
            val request = LoginRequest(email = currentEmail, password = currentPassword)

            // 2. 发起真实网络请求 (协程挂起)
            val response = RetrofitClient.loginApiService.login(request)

            // 3. 处理业务逻辑
            // 假设后端约定 code == 200 代表成功
            if (response.code == 200 && response.data != null) {

                // 获取 Token 和 UserInfo
                val token = response.data.token
                val userInfo = response.data.userInfo

                // 这里把 token和userInfo 保存到 MMKV 中
                TokenManager.saveToken(token)
                UserManager.saveUser(userInfo)

                // 发送跳转事件
                _loginEvent.send(LoginUiEvent.NavigateToHome)

            } else {
                // 业务失败 (比如密码错误)，显示后端返回的 msg
                showToast("登录失败，请检查账号密码")
            }
        }
    }
}