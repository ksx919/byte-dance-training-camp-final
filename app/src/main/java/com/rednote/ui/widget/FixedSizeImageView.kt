package com.rednote.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * 性能优化专用 ImageView。
 * 当我们确定 View 的大小已经被父布局或 LayoutParams 固定死时，
 * 设置 Drawable 不应触发 requestLayout。
 */
class FixedSizeImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    override fun requestLayout() {
        // 【核心黑科技】
        // 如果当前 View 已经经历过 layout 并且大小确定，
        // 我们就拦截 requestLayout，防止它向父 View 传播，造成连锁反应。
        // 除非显式调用 forceLayout()，否则这里直接吞掉。
        if (!isLayoutRequested) {
            super.requestLayout()
        }
    }
}