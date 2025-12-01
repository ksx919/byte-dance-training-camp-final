package com.rednote.data.model.comment

import android.net.Uri

data class CommentVO(
    val id: Long,
    val postId: Long,
    val userId: Long,
    val nickname: String,
    val avatarUrl: String,
    val content: String,
    var likeCount: Int,
    var isLiked: Boolean,
    val createdAt: String,
    val imageUrl: String?,
    val imageWidth: Int?,
    val imageHeight: Int?,
    val topReply: CommentVO?,
    val replyCount: Int?,
    val targetUserNickname: String?
) {
    var childReplies: MutableList<CommentVO>? = null
    var nextCursor: String? = null
    var hasMoreReplies: Boolean = false
    
    @Transient var totalHeight: Int = 0
    @Transient var isLoadingReplies: Boolean = false
    
    // Optimistic UI fields
    @Transient var status: Int = STATUS_NORMAL
    @Transient var localImageUri: Uri? = null
    
    // Context for retry
    @Transient var tempParentId: Long? = null
    @Transient var tempRootId: Long? = null
    @Transient var tempReplyToUserId: Long? = null

    companion object {
        const val STATUS_NORMAL = 0
        const val STATUS_SENDING = 1
        const val STATUS_FAILED = 2
    }
}
