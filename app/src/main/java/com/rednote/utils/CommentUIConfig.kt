package com.rednote.utils

import android.content.Context
import android.text.TextPaint
import android.util.TypedValue

object CommentUIConfig {

    var isInitialized = false
        private set

    // Dimensions (px)
    var screenWidth: Int = 0
    var avatarSize: Int = 0
    var avatarMarginStart: Int = 0
    var contentMarginTop: Int = 0
    var contentMarginEnd: Int = 0
    var likeContainerWidth: Int = 0
    var paddingStart: Int = 0
    var paddingEnd: Int = 0
    var paddingTop: Int = 0
    var paddingBottom: Int = 0
    
    var contentTextSize: Float = 0f
    var contentLineSpacingExtra: Float = 0f
    
    var replyContainerMarginTop: Int = 0
    
    // Calculated widths
    var contentAvailableWidth: Int = 0
    var replyContentAvailableWidth: Int = 0

    // Paint for text measurement
    val textPaint = TextPaint()

    var density: Float = 1f

    fun init(context: Context) {
        if (isInitialized) return

        val res = context.resources
        val metrics = res.displayMetrics
        screenWidth = metrics.widthPixels
        density = metrics.density

        // 1. Basic Dimensions from item_comment.xml
        paddingStart = dp2px(16f)
        paddingEnd = dp2px(16f)
        paddingTop = dp2px(12f)
        paddingBottom = dp2px(4f)
        
        avatarSize = dp2px(36f)
        avatarMarginStart = dp2px(10f) // Margin between avatar and username/content
        
        contentMarginTop = dp2px(4f)
        contentMarginEnd = dp2px(16f)
        
        likeContainerWidth = dp2px(30f) // Approximate width for like icon + count
        
        replyContainerMarginTop = dp2px(8f)

        // 2. Text Configuration
        contentTextSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 15f, metrics
        )
        contentLineSpacingExtra = dp2px(2f).toFloat()
        
        textPaint.textSize = contentTextSize
        textPaint.isAntiAlias = true

        // 3. Calculate Available Width for Content
        val contentStart = paddingStart + avatarSize + avatarMarginStart
        val contentEnd = paddingEnd + likeContainerWidth + contentMarginEnd
        
        contentAvailableWidth = screenWidth - contentStart - contentEnd
        
        // 4. Calculate Available Width for Reply Content
        // ll_reply_container width = Screen - contentStart - paddingEnd
        val replyContainerWidth = screenWidth - contentStart - paddingEnd
        
        // item_comment_reply.xml:
        // Content Start = 24dp (avatar) + 8dp (margin) = 32dp
        // Content End = 16dp (margin) + LikeContainer (approx 30dp)
        val replyContentStart = dp2px(32f)
        val replyContentEnd = dp2px(16f) + dp2px(30f)
        
        replyContentAvailableWidth = replyContainerWidth - replyContentStart - replyContentEnd

        isInitialized = true
    }

    fun dp2px(dp: Float): Int {
        return (dp * density + 0.5f).toInt()
    }
}
