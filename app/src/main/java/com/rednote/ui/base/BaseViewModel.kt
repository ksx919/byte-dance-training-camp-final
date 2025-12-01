package com.rednote.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    // 1. 控制 Loading 状态 (StateFlow 替代 LiveData)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 2. 发送一次性事件 (如 Toast, 导航)，使用 Channel/SharedFlow
    // Channel 特点：事件被消费一次就没了，适合弹窗
    private val _toastEvent = Channel<String>()
    val toastEvent = _toastEvent.receiveAsFlow()

    // 3. 封装通用的协程调用，自动处理 Loading 和 异常
    // block: 具体的业务逻辑 (比如调用 Repository)
    fun launchDataLoad(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                showLoading()
                block() // 执行具体的业务
            } catch (e: Exception) {
                // 统一异常处理，比如网络错误
                showToast("发生未知错误，请稍后重试")
                e.printStackTrace()
            } finally {
                hideLoading()
            }
        }
    }

    private fun showLoading() {
        _isLoading.value = true
    }

    private fun hideLoading() {
        _isLoading.value = false
    }

    protected fun showToast(message: String) {
        viewModelScope.launch {
            _toastEvent.send(message)
        }
    }
}
