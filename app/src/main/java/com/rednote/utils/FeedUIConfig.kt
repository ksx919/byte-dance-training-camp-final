package com.rednote.utils

import android.content.Context
import android.util.TypedValue

object FeedUIConfig {

    var isInitialized = false
        private set

    // 核心尺寸（像素）
    var itemWidth: Int = 0
    var padding: Int = 0
    var avatarSize: Int = 0
    var staticContentHeight: Int = 0

    // 字体大小
    var titleTextSize: Float = 0f

    // 屏幕密度，用于后续的 dp 转 px 计算
    var density: Float = 1f

    fun init(context: Context) {
        if (isInitialized) return

        val res = context.resources
        val metrics = res.displayMetrics
        val screenWidth = metrics.widthPixels

        // 赋值 density
        density = metrics.density

        // 1. 基础转换
        padding = dp2px(8f, metrics)
        val margin = dp2px(2f, metrics)
        avatarSize = dp2px(20f, metrics)

        // 2. 计算卡片宽度
        val listTotalPadding = padding * 2
        itemWidth = (screenWidth - listTotalPadding) / 2 - margin

        // 3. 计算“非动态内容”的总高度
        staticContentHeight = padding + padding + avatarSize + padding

        // 4. 文字配置
        titleTextSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 14f, metrics
        )

        isInitialized = true
    }

    private fun dp2px(dp: Float, metrics: android.util.DisplayMetrics): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics).toInt()
    }
}