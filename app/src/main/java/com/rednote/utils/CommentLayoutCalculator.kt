package com.rednote.utils

import android.content.res.Resources
import android.graphics.Paint
import android.text.StaticLayout
import android.text.Layout
import android.text.TextPaint
import android.util.TypedValue
import com.rednote.data.model.comment.CommentVO

object CommentLayoutCalculator {

    fun calculate(comment: CommentVO): Int {
        if (!CommentUIConfig.isInitialized) return 0

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        var height = CommentUIConfig.paddingTop

        // 1. Username Height (13sp)
        val usernameSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13f, Resources.getSystem().displayMetrics)
        textPaint.textSize = usernameSize
        
        // Use StaticLayout for accurate height including font padding
        val usernameLayout = StaticLayout.Builder.obtain(
            comment.nickname,
            0,
            comment.nickname.length,
            textPaint,
            CommentUIConfig.contentAvailableWidth // Username has same width constraint roughly
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(true)
            .build()
        height += usernameLayout.height

        // 2. Content Height (15sp)
        height += CommentUIConfig.contentMarginTop
        if (comment.content.isNotEmpty()) {
            textPaint.textSize = CommentUIConfig.contentTextSize
            val textLayout = StaticLayout.Builder.obtain(
                comment.content,
                0,
                comment.content.length,
                textPaint,
                CommentUIConfig.contentAvailableWidth
            )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(CommentUIConfig.contentLineSpacingExtra, 1.0f)
                .setIncludePad(true)
                .build()
            
            height += textLayout.height
        }

        // 3. Image Height
        if (!comment.imageUrl.isNullOrEmpty()) {
            height += CommentUIConfig.dp2px(8f) // Margin Top
            
            if (comment.imageWidth != null && comment.imageHeight != null && comment.imageWidth > 0 && comment.imageHeight > 0) {
                val targetWidth = CommentUIConfig.dp2px(160f)
                val ratio = comment.imageHeight.toFloat() / comment.imageWidth.toFloat()
                val targetHeight = (targetWidth * ratio).toInt()
                height += targetHeight
            } else {
                height += CommentUIConfig.dp2px(200f) 
            }
        }

        // 4. Info (Date/Reply) Height (11sp)
        height += CommentUIConfig.dp2px(8f) // Margin Top
        
        val infoSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11f, Resources.getSystem().displayMetrics)
        textPaint.textSize = infoSize
        
        // Use placeholder text for info height if needed, or just measure 1 line
        val infoText = "2023-01-01 回复" 
        val infoLayout = StaticLayout.Builder.obtain(
            infoText,
            0,
            infoText.length,
            textPaint,
            CommentUIConfig.contentAvailableWidth
        )
            .setIncludePad(true)
            .build()
        height += infoLayout.height

        // 5. Reply Container Height
        val repliesToShow = if (comment.childReplies?.isNotEmpty() == true) {
            comment.childReplies!!
        } else if (comment.topReply != null) {
            listOf(comment.topReply)
        } else {
            emptyList()
        }
        
        if (repliesToShow.isNotEmpty() || (comment.replyCount ?: 0) > 0) {
            height += CommentUIConfig.replyContainerMarginTop
            
            for (reply in repliesToShow) {
                height += calculateReplyItemHeight(reply)
            }
            
            val totalCount = comment.replyCount ?: 0
            val currentCount = if (comment.childReplies != null) comment.childReplies!!.size else (if (comment.topReply != null) 1 else 0)
            
            if (totalCount > currentCount || (comment.hasMoreReplies)) {
                 height += CommentUIConfig.dp2px(34f) // Expand button (increased to 34dp)
            }
        }

        // 6. Divider & Bottom Padding
        height += CommentUIConfig.dp2px(12f) // Margin Top for divider
        height += CommentUIConfig.dp2px(1f)  // Divider height
        height += CommentUIConfig.paddingBottom
        
        // Add a buffer to prevent overlap
        height += CommentUIConfig.dp2px(12f)

        return height
    }
    
    private fun calculateReplyItemHeight(reply: CommentVO): Int {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        
        var h = CommentUIConfig.dp2px(4f) // Top padding
        
        // Username (12sp)
        val usernameSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, Resources.getSystem().displayMetrics)
        textPaint.textSize = usernameSize
        
        val usernameLayout = StaticLayout.Builder.obtain(
            reply.nickname,
            0,
            reply.nickname.length,
            textPaint,
            CommentUIConfig.replyContentAvailableWidth
        )
            .setIncludePad(true)
            .build()
        h += usernameLayout.height
        
        // Content (14sp)
        h += CommentUIConfig.dp2px(2f) // Margin Top
        
        val contentSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, Resources.getSystem().displayMetrics)
        textPaint.textSize = contentSize
        
        val replyTextWidth = CommentUIConfig.replyContentAvailableWidth

        val contentText = if (!reply.targetUserNickname.isNullOrEmpty()) {
            "回复 ${reply.targetUserNickname}：${reply.content}"
        } else {
            reply.content
        }
        
        val textLayout = StaticLayout.Builder.obtain(
            contentText,
            0,
            contentText.length,
            textPaint,
            replyTextWidth
        )
            .setLineSpacing(CommentUIConfig.contentLineSpacingExtra, 1.0f)
            .setIncludePad(true)
            .build()
        
        h += textLayout.height
        
        // Image Height
        if (!reply.imageUrl.isNullOrEmpty()) {
            h += CommentUIConfig.dp2px(8f) // Margin Top
            
            if (reply.imageWidth != null && reply.imageHeight != null && reply.imageWidth > 0 && reply.imageHeight > 0) {
                val targetWidth = CommentUIConfig.dp2px(160f) // Max width for reply image
                val ratio = reply.imageHeight.toFloat() / reply.imageWidth.toFloat()
                val targetHeight = (targetWidth * ratio).toInt()
                h += targetHeight
            } else {
                h += CommentUIConfig.dp2px(160f) // Default square-ish
            }
        }
        
        // Info (11sp)
        h += CommentUIConfig.dp2px(4f) // Margin Top
        val infoSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11f, Resources.getSystem().displayMetrics)
        textPaint.textSize = infoSize
        
        val infoText = "01-01 回复"
        val infoLayout = StaticLayout.Builder.obtain(
            infoText,
            0,
            infoText.length,
            textPaint,
            replyTextWidth
        )
            .setIncludePad(true)
            .build()
        h += infoLayout.height
        
        h += CommentUIConfig.dp2px(4f) // Bottom padding
        
        return h
    }
}
