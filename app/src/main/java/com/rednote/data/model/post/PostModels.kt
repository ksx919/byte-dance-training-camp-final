package com.rednote.data.model.post

import android.text.StaticLayout
import com.google.gson.annotations.SerializedName

data class PostInfo(
    val id: Long,
    val title: String,
    @SerializedName("nickname") val author: String,
    val avatarUrl: String?,
    val likeCount: Int,
    @SerializedName("image") val imageUrl: String?, // 这是服务器返回的网络地址
    val width: Int = 0,
    val height: Int = 0,
    val isLiked: Boolean = false,

    // --- 【新增】本地状态字段 ---
    var content: String? = null,
    var status: Int = STATUS_NORMAL,      // 状态：0=正常, 1=上传中, 2=失败
    var localImageUri: String? = null,    // 本地图片的 Uri，上传完成前显示这个
    var failMessage: String? = null,       // 如果失败，记录原因
    val localId: String? = null // 【新增】本地唯一ID，用于 DiffUtil 防止重排
) {
    companion object {
        const val STATUS_NORMAL = 0
        const val STATUS_UPLOADING = 1
        const val STATUS_FAILED = 2
    }

    // --- UI 缓存字段 (全 Transient) ---
    @Transient var titleLayout: StaticLayout? = null
    @Transient var titleHeight: Int = 0
    @Transient var authorLayout: StaticLayout? = null
    @Transient var likeLayout: StaticLayout? = null
    @Transient var totalHeight: Int = 0
}

data class CursorResult<T>(
    val list: List<T>,
    val nextCursor: String?,
    val hasMore: Boolean
)