package com.rednote.ui.publish

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.rednote.utils.DraftManager
import com.rednote.utils.PostUploadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PublishViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    // --- 状态 (State) ---
    private val _imageList = MutableStateFlow<List<Uri>>(emptyList())
    val imageList: StateFlow<List<Uri>> = _imageList.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    // --- 一次性事件 (Events) ---
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
    }

    /**
     * 从相册选择器接收图片列表
     */
    fun tryAddImages(uris: List<Uri>) {
        if (uris.size > MAX_IMAGE_COUNT) {
            _imageList.value = uris.take(MAX_IMAGE_COUNT)
            sendToast("最多只能选择${MAX_IMAGE_COUNT}张图片")
        } else {
            _imageList.value = uris
        }
    }

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
        val currentTitle = _title.value
        val currentContent = _content.value
        val currentImages = _imageList.value

        if (currentTitle.isBlank()) {
            sendToast("请输入标题")
            return
        }
        if (currentImages.isEmpty()) {
            sendToast("请至少选择一张图片")
            return
        }

        // 移交任务给 Manager (使用 Application Context)
        PostUploadManager.publish(getApplication(), currentTitle, currentContent, currentImages)

        // 发布成功，彻底清除草稿
        clearDraft()

        sendToast("正在后台发布...")
        viewModelScope.launch {
            _viewEvent.send(PublishViewEvent.Finish)
        }
    }

    private fun sendToast(msg: String) {
        viewModelScope.launch {
            _viewEvent.send(PublishViewEvent.ShowToast(msg))
        }
    }

    // --- 草稿箱逻辑 (核心修复区域) ---

    // 辅助判断：是否有未保存的更改（用于返回拦截）
    fun hasUnsavedChanges(): Boolean {
        return _title.value.isNotBlank() || _content.value.isNotBlank() || _imageList.value.isNotEmpty()
    }

    /**
     * 如果当前页面是空的 -> 删除草稿（解决退出后草稿还在的问题）
     * 如果当前页面有内容 -> 保存草稿（解决图片不保存的问题）
     */
    fun saveDraftOrClear() {
        viewModelScope.launch {
            // 使用 IO + NonCancellable，
            // 确保在 Activity 关闭、进程即将被杀时，IO 操作能抢救多少是多少
            withContext(Dispatchers.IO + NonCancellable) {
                if (!hasUnsavedChanges()) {
                    // 如果没内容，删草稿
                    DraftManager.clearDraft()
                } else {
                    // 如果有内容，存草稿
                    val currentTitle = _title.value
                    val currentContent = _content.value
                    // 必须把 List 拷贝一份传过去，防止多线程并发修改问题
                    val currentImages = ArrayList(_imageList.value)

                    DraftManager.saveDraft(currentTitle, currentContent, currentImages)
                }
            }
        }
    }

    /**
     * 单纯的保存草稿 (供 Dialog 确认保存时调用)
     */
    fun saveDraft() {
        if (hasUnsavedChanges()) {
            viewModelScope.launch {
                withContext(Dispatchers.IO + NonCancellable) {
                    val currentImages = ArrayList(_imageList.value)
                    DraftManager.saveDraft(_title.value, _content.value, currentImages)
                }
            }
        }
    }

    fun loadDraft() {
        viewModelScope.launch(Dispatchers.IO) {
            val draft = DraftManager.getDraft()
            if (draft != null) {
                val imageUris = DraftManager.parseImages(draft.imagesJson)
                android.util.Log.d("PublishViewModel", "loadDraft: Found draft with ${imageUris.size} images")
                withContext(Dispatchers.Main) {
                    _title.value = draft.title
                    _content.value = draft.content
                    _imageList.value = imageUris
                    android.util.Log.d("PublishViewModel", "loadDraft: Updated UI with ${imageUris.size} images")
                }
            } else {
                android.util.Log.d("PublishViewModel", "loadDraft: No draft found")
            }
        }
    }

    /**
     * 彻底清除草稿
     */
    fun clearDraft() {
        viewModelScope.launch {
            withContext(Dispatchers.IO + NonCancellable) {
                DraftManager.clearDraft()
            }
        }
    }
}