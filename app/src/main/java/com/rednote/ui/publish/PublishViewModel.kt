package com.rednote.ui.publish

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.rednote.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 发布页面ViewModel
 * 职责：管理发布页面的状态（图片列表、标题、内容等）
 */
class PublishViewModel : BaseViewModel() {

    // 图片列表状态
    private val _imageList = MutableStateFlow<List<Uri>>(emptyList())
    val imageList: StateFlow<List<Uri>> = _imageList.asStateFlow()

    // 标题状态
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    // 内容状态
    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    /**
     * 添加图片
     */
    fun addImages(uris: List<Uri>) {
        val currentList = _imageList.value.toMutableList()
        currentList.addAll(uris)
        _imageList.value = currentList
    }

    /**
     * 添加单张图片
     */
    fun addImage(uri: Uri) {
        val currentList = _imageList.value.toMutableList()
        currentList.add(uri)
        _imageList.value = currentList
    }

    /**
     * 移除图片
     */
    fun removeImage(position: Int) {
        val currentList = _imageList.value.toMutableList()
        if (position in currentList.indices) {
            currentList.removeAt(position)
            _imageList.value = currentList
        }
    }

    /**
     * 清空所有图片
     */
    fun clearImages() {
        _imageList.value = emptyList()
    }

    /**
     * 更新标题
     */
    fun updateTitle(newTitle: String) {
        _title.value = newTitle
    }

    /**
     * 更新内容
     */
    fun updateContent(newContent: String) {
        _content.value = newContent
    }

    /**
     * 获取当前图片数量
     */
    fun getImageCount(): Int = _imageList.value.size

    /**
     * 显示Toast消息（公共方法，供View层调用）
     */
    fun showToastMessage(message: String) {
        showToast(message)
    }

    /**
     * 发布内容（可以在这里添加网络请求等业务逻辑）
     */
    fun publish() {
        viewModelScope.launch {
            // TODO: 实现发布逻辑
            // 例如：调用Repository上传图片和内容
            showToast("发布功能待实现")
        }
    }
}

