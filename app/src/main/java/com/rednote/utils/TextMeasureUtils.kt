package com.rednote.utils

import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.toColorInt
import java.lang.ThreadLocal

object TextMeasureUtils {

    // 1. 标题画笔 (ThreadLocal 防止不同线程同时修改同一实例)
    private val titlePaintTL = ThreadLocal.withInitial {
        TextPaint().apply {
            isAntiAlias = true
            color = Color.BLACK
            // typeface = Typeface.DEFAULT_BOLD // 如需粗体请取消注释
        }
    }

    // 2. 作者名画笔
    private val authorPaintTL = ThreadLocal.withInitial {
        TextPaint().apply {
            isAntiAlias = true
            textSize = 11f.spToPx // 11sp
            color = "#333333".toColorInt() // 深灰色
        }
    }

    // 3. 点赞数画笔
    private val likePaintTL = ThreadLocal.withInitial {
        TextPaint().apply {
            isAntiAlias = true
            textSize = 11f.spToPx // 11sp
            color = Color.GRAY
        }
    }

    private fun obtainTitlePaint(): TextPaint = titlePaintTL.get()!!
    private fun obtainAuthorPaint(): TextPaint = authorPaintTL.get()!!
    private fun obtainLikePaint(): TextPaint = likePaintTL.get()!!

    /**
     * 预计算标题 Layout (双行，末尾省略)
     * @param width Item 的总宽度 (FeedUIConfig.itemWidth)
     */
    fun preCalculateTitle(text: String, width: Int, textSize: Float): Pair<StaticLayout?, Int> {
        if (text.isEmpty()) return Pair(null, 0)

        val titlePaint = obtainTitlePaint()
        // 动态更新字号，防止配置运行时变更
        titlePaint.textSize = textSize

        // 【关键】减去左右 Padding，否则文字会画出圆角边界
        val contentWidth = width - (FeedUIConfig.padding * 2)

        val layout = createStaticLayout(
            text = text,
            paint = titlePaint,
            width = contentWidth,
            maxLines = 2,
            ellipsize = TextUtils.TruncateAt.END
        )

        // 【保留你的优化】：高度 + 2px 缓冲，防止 baseline 对齐导致切底
        val height = layout.height + 2

        return Pair(layout, height)
    }

    /**
     * 预计算作者名 Layout (单行，末尾省略)
     * @param maxWidth 作者名允许的最大宽度 (通常是 Item宽 - Padding - 头像 - 预留点赞区)
     */
    fun preCalculateAuthor(text: String, maxWidth: Int): StaticLayout? {
        if (text.isEmpty()) return null

        return createStaticLayout(
            text = text,
            paint = obtainAuthorPaint(),
            width = maxWidth,
            maxLines = 1,
            ellipsize = TextUtils.TruncateAt.END
        )
    }

    /**
     * 预计算点赞数 Layout (单行，不省略)
     * 数字通常较短，宽度按实际文字宽度计算即可
     */
    fun preCalculateLike(text: String): StaticLayout? {
        if (text.isEmpty()) return null

        val likePaint = obtainLikePaint()
        // 计算文字实际需要的宽度，并稍微给一点余量 (+2) 防止边缘裁剪
        val desiredWidth = likePaint.measureText(text).toInt() + 2

        return createStaticLayout(
            text = text,
            paint = likePaint,
            width = desiredWidth,
            maxLines = 1,
            ellipsize = null // 数字通常不做省略处理
        )
    }

    /**
     * 内部通用方法：兼容不同 Android 版本的 StaticLayout 创建逻辑
     */
    private fun createStaticLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        maxLines: Int,
        ellipsize: TextUtils.TruncateAt?
    ): StaticLayout {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(true) // 建议设为 true 以包含字体本身的 padding
                .setEllipsize(ellipsize)
                .setMaxLines(maxLines)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text, 0, text.length, paint, width, Layout.Alignment.ALIGN_NORMAL,
                1.0f, 0.0f, true, ellipsize, width
            )
        }
    }

    // 扩展属性：SP 转 PX (使用系统级 Resources，无需 Context)
    private val Float.spToPx: Float // sp to px
        get() = this * Resources.getSystem().displayMetrics.density
}