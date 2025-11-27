package com.rednote.ui.publish

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class PublishViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // --- 状态 (State) ---
    private val _imageList = MutableStateFlow<List<Uri>>(emptyList())
    val imageList: StateFlow<List<Uri>> = _imageList.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    // --- 一次性事件 (Events) ---
    // 使用 Channel 处理"动作"，保证事件只被消费一次
    private val _viewEvent = Channel<PublishViewEvent>(Channel.BUFFERED)
    val viewEvent = _viewEvent.receiveAsFlow()

    // --- 常量 ---
    private val MAX_IMAGE_COUNT = 9
    private val KEY_PENDING_CAMERA_URI = "pending_camera_uri"

    /**
     * 用户点击：请求打开相册
     */
    fun onPickImageClick() {
        val currentCount = _imageList.value.size
        if (currentCount >= MAX_IMAGE_COUNT) {
            sendToast("最多只能选择${MAX_IMAGE_COUNT}张图片")
            return
        }
        // 告诉 View：去打开相册，还可以选 (9 - current) 张
        val remaining = MAX_IMAGE_COUNT - currentCount
        viewModelScope.launch {
            _viewEvent.send(PublishViewEvent.OpenGallery(remaining))
        }
    }

    /**
     * 用户点击：请求打开相机
     */
    fun onTakePhotoClick() {
        if (_imageList.value.size >= MAX_IMAGE_COUNT) {
            sendToast("最多只能选择${MAX_IMAGE_COUNT}张图片")
            return
        }
        // 告诉 View：去准备打开相机（需要 View 创建文件）
        viewModelScope.launch {
            _viewEvent.send(PublishViewEvent.PrepareCamera)
        }
    }

    /**
     * View层创建好文件后，将 URI 存入 ViewModel (使用 SavedStateHandle 防丢失)
     */
    fun setPendingCameraUri(uri: Uri) {
        savedStateHandle[KEY_PENDING_CAMERA_URI] = uri
    }

    /**
     * 相机回调结果
     */
    fun onCameraResult(success: Boolean) {
        val uri = savedStateHandle.get<Uri>(KEY_PENDING_CAMERA_URI)
        if (success && uri != null) {
            addImage(uri)
        }
        // 无论成功失败，最好清理一下（也可保留以防万一，视需求而定）
        // savedStateHandle.remove<Uri>(KEY_PENDING_CAMERA_URI)
    }

    /**
     * 从相册选择器接收图片列表（已经过用户筛选，直接使用）
     * ImageSelectorActivity 返回的是用户最终确定的完整列表
     */
    fun tryAddImages(uris: List<Uri>) {
        // ImageSelectorActivity 返回的是用户最终确定的完整列表
        // 包括之前已选的和本次新选的，已经处理过重复问题
        // 允许空列表（用户取消了所有选择）
        if (uris.size > MAX_IMAGE_COUNT) {
            // 如果超过最大数量，截取前面的
            _imageList.value = uris.take(MAX_IMAGE_COUNT)
            sendToast("最多只能选择${MAX_IMAGE_COUNT}张图片")
        } else {
            _imageList.value = uris
        }
    }

    // 私有辅助方法：直接添加单张（不校验，内部使用）
    private fun addImage(uri: Uri) {
        val currentList = _imageList.value.toMutableList()
        currentList.add(uri)
        _imageList.value = currentList
    }

    fun updateTitle(newTitle: String) {
        _title.value = newTitle
    }

    fun updateContent(newContent: String) {
        _content.value = newContent
    }

    fun publish() {
        // todo 发布逻辑...
    }

    private fun sendToast(msg: String) {
        viewModelScope.launch {
            _viewEvent.send(PublishViewEvent.ShowToast(msg))
        }
    }
}