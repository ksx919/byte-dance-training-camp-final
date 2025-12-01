package com.rednote.data.model.post

/**
 * 本地待发布的帖子模型
 */
data class PendingPost(
    val localId: String,            // 本地生成的唯一ID (UUID)
    var serverId: Long? = null,     // 服务器返回的真实ID
    var status: Int,                // 状态: 0=Pending, 1=Uploading, 2=Success, 3=Failed
    var progress: Int = 0,          // 上传进度 0-100
    var errorMessage: String? = null,
    
    // 帖子内容
    val title: String,
    val content: String,
    val imageUris: List<String>,    // 图片URI列表
    val createTime: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = 0
        const val STATUS_UPLOADING = 1
        const val STATUS_SUCCESS = 2
        const val STATUS_FAILED = 3
    }
}
