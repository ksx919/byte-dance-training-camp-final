package com.rednote.data.model.comment

data class AddCommentDTO(
    val postId: Long,
    val content: String,
    val parentId: Long? = null,
    val rootParentId: Long? = null,
    val replyToUserId: Long? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null
)
