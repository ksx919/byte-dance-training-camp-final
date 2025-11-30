package com.rednote.data.model.post

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PostDetailVO(
    // === 帖子基础信息 ===
    val id: Long,
    val title: String,
    val content: String,
    val images: List<String>?, // 图片列表可能为空
    val imgWidth: Int?,        // 宽高可能为空，需处理空安全
    val imgHeight: Int?,

    // 后端返回的是 ISO-8601 字符串，Gson 默认按 String 解析
    val createdAt: String,

    // === 作者信息 ===
    val authorId: Long,
    val authorName: String,
    val authorAvatar: String?,

    // === 交互数据 ===
    // 定义为 var，因为在详情页点赞后需要动态修改这些值并刷新 UI
    var likeCount: Int,
    var commentCount: Int,
    var isLiked: Boolean
) : Parcelable