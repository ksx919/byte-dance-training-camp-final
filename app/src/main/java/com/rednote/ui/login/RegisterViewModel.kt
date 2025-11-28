package com.rednote.ui.login

import android.util.Patterns
import com.rednote.data.api.RetrofitClient
import com.rednote.data.model.login.RegisterRequest
import com.rednote.ui.base.BaseViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class RegisterViewModel : BaseViewModel() {

    private val _email = MutableStateFlow("")
    private val _password = MutableStateFlow("")
    private val _nickname = MutableStateFlow("")

    private val _registerEvent = Channel<RegisterUiEvent>(Channel.BUFFERED)
    val registerEvent = _registerEvent.receiveAsFlow()

    fun updateEmail(input: String) {
        _email.value = input
    }

    fun updatePassword(input: String) {
        _password.value = input
    }

    fun updateNickname(input: String) {
        _nickname.value = input
    }

    fun register() {
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value.trim()
        val currentNickname = _nickname.value.trim()

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
        if (currentNickname.isEmpty()) {
            showToast("请输入昵称")
            return
        }

        launchDataLoad {
            val request = RegisterRequest(
                email = currentEmail,
                password = currentPassword,
                nickname = currentNickname
            )
            val response = RetrofitClient.loginApiService.register(request)

            if (response.code == 200) {
                showToast("注册成功")
                _registerEvent.send(RegisterUiEvent.RegisterSuccess)
            } else {
                showToast(response.msg ?: "注册失败")
            }
        }
    }
}