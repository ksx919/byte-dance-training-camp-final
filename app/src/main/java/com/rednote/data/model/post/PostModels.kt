package com.rednote.data.model.post

import android.text.StaticLayout
import com.google.gson.annotations.SerializedName

data class PostInfo(
    val id: Long,
    val title: String,
    @SerializedName("nickname") val author: String,
    val avatarUrl: String?,
    val likeCount: Int,
    @SerializedName("image") val imageUrl: String?,
    val width: Int = 0,
    val height: Int = 0
) {
    // --- UI 缓存字段 (全 Transient) ---
    @Transient var titleLayout: StaticLayout? = null
    @Transient var titleHeight: Int = 0

    // 【新增】作者名和点赞数的预计算 Layout
    @Transient var authorLayout: StaticLayout? = null
    @Transient var likeLayout: StaticLayout? = null

    @Transient var totalHeight: Int = 0
}

data class CursorResult<T>(
    val list: List<T>,
    val nextCursor: Long?,
    val hasMore: Boolean
)