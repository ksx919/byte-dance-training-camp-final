package com.rednote.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withRotation

/**
 * 仿小红书风格的极简加载 View
 * 效果：两个红色圆点围绕中心旋转
 */
class RedNoteLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 主色调：小红书红
    private val colorRed = "#FF2442".toColorInt()
    // 浅色调：可以用半透明红，或者稍微浅一点的粉
    private val colorPink = "#FF899E".toColorInt()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // 动画控制
    private var rotationAngle = 0f
    private var animator: ValueAnimator? = null

    // 尺寸配置 (dp)
    private var dotRadius = 0f // 圆点半径
    private var orbitRadius = 0f // 旋转轨道半径

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 根据 View 的大小动态计算圆点大小
        // 假设 View 大小是 30dp，那么轨道半径就是 8dp，圆点半径 3dp
        val size = w.coerceAtMost(h).toFloat()
        orbitRadius = size * 0.25f
        dotRadius = size * 0.12f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f

        // 保存画布状态
        canvas.withRotation(rotationAngle, cx, cy) {

            // 旋转画布
            // 画左边的圆点 (深红)
            paint.color = colorRed
            drawCircle(cx - orbitRadius, cy, dotRadius, paint)

            // 画右边的圆点 (浅红/粉)
            paint.color = colorPink
            drawCircle(cx + orbitRadius, cy, dotRadius, paint)

            // 恢复画布
        }
    }

    private fun startAnim() {
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 800 // 旋转一圈 800ms，快速且丝滑
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator() // 匀速旋转
                addUpdateListener {
                    rotationAngle = it.animatedValue as Float
                    invalidate() // 触发重绘
                }
            }
        }
        if (animator?.isRunning == false) {
            animator?.start()
        }
    }

    private fun stopAnim() {
        animator?.cancel()
    }

    // View 只有在显示时才动画，节省性能
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnim()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnim()
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == VISIBLE) {
            startAnim()
        } else {
            stopAnim()
        }
    }
}