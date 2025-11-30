package com.rednote.ui.main.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.rednote.R
import com.rednote.data.model.post.PostInfo
import com.rednote.utils.FeedUIConfig
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import androidx.core.graphics.toColorInt
import androidx.core.graphics.drawable.toDrawable
import com.rednote.ui.widget.FixedSizeImageView

class FeedItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    // 只保留图片 View，文字全部画上去
    val ivCover = FixedSizeImageView(context)
    val ivAvatar = FixedSizeImageView(context)

    private var currentItem: PostInfo? = null
    private var lastCoverUrl: String? = null
    private var lastAvatarUrl: String? = null

    companion object {
        private var cachedPlaceholder: ColorDrawable? = null
        private var cachedHeartDrawable: Drawable? = null

        // 全局复用的圆角裁剪器
        private val ROUND_RECT_PROVIDER = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                // 假设 FeedUIConfig.padding / 2f 是圆角半径
                outline.setRoundRect(0, 0, view.width, view.height, FeedUIConfig.padding / 2f)
            }
        }
    }

    init {
        // 1. 封面
        ivCover.scaleType = ImageView.ScaleType.CENTER_CROP
        // 优化：不添加 TextView，减少子 View 数量
        addView(ivCover)

        // 2. 头像
        ivAvatar.scaleType = ImageView.ScaleType.CENTER_CROP
        addView(ivAvatar)

        // 缓存占位图
        if (cachedPlaceholder == null) {
            cachedPlaceholder = "#F5F5F5".toColorInt().toDrawable()
        }
        ivCover.setImageDrawable(cachedPlaceholder)

        // 缓存爱心图标
        if (cachedHeartDrawable == null) {
            cachedHeartDrawable = ContextCompat.getDrawable(context.applicationContext, R.drawable.ic_favorite)?.mutate()
            // 预设大小，例如 12dp
            val size = (12 * resources.displayMetrics.density).toInt()
            cachedHeartDrawable?.setBounds(0, 0, size, size)
        }

        setBackgroundColor(Color.WHITE)
        clipToOutline = true
        outlineProvider = ROUND_RECT_PROVIDER

        // 【关键】允许 ViewGroup 调用 onDraw/dispatchDraw
        setWillNotDraw(false)
    }

    fun bind(item: PostInfo) {
        this.currentItem = item

        // 1. 加载封面
        if (lastCoverUrl != item.imageUrl) {
            lastCoverUrl = item.imageUrl
            if (!item.imageUrl.isNullOrEmpty()) {
                // 计算目标宽高，优化 Glide 加载内存
                val targetW = FeedUIConfig.itemWidth
                var targetH = targetW
                if (item.width > 0 && item.height > 0) {
                    val ratio = item.height.toFloat() / item.width.toFloat()
                    // 限制最大比例，与 onMeasure 保持一致
                    val maxRatio = 1.33f
                    val finalRatio = if (ratio > maxRatio) maxRatio else ratio
                    targetH = (targetW * finalRatio).toInt()
                }

                Glide.with(context)
                    .load(item.imageUrl)
                    .override(targetW, targetH) // 【核心优化】指定加载尺寸
                    .placeholder(cachedPlaceholder)
                    .transition(DrawableTransitionOptions.withCrossFade()) // 【优化】渐变动画
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivCover)
            } else {
                Glide.with(context).clear(ivCover)
                ivCover.setImageDrawable(cachedPlaceholder)
            }
        }

        // 2. 加载头像
        if (lastAvatarUrl != item.avatarUrl) {
            lastAvatarUrl = item.avatarUrl
            if (!item.avatarUrl.isNullOrEmpty()) {
                val size = FeedUIConfig.avatarSize
                Glide.with(context)
                    .load(item.avatarUrl)
                    .override(size, size) // 【核心优化】指定加载尺寸
                    .placeholder(cachedPlaceholder)
                    .transition(DrawableTransitionOptions.withCrossFade()) // 【优化】渐变动画
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivAvatar)
            } else {
                Glide.with(context).clear(ivAvatar)
                ivAvatar.setImageDrawable(cachedPlaceholder)
            }
        }

        // 触发重绘以显示文字
        invalidate()
    }

    // 【优化】O(1) 测量
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        
        // 1. 计算封面高度，限制最大宽高比 (例如 4:3 = 1.33)
        // 防止长图过长，在 Feed 流中占据太多屏幕
        val maxRatio = 1.33f // 4:3
        val rawCoverH = currentItem?.totalHeight?.minus((currentItem?.titleHeight ?: 0) + FeedUIConfig.staticContentHeight) ?: 0
        
        // 计算实际封面高度：如果原图比例超过 maxRatio，则截断
        val item = currentItem
        var finalCoverH = rawCoverH
        
        if (item != null && item.width > 0 && item.height > 0) {
             val ratio = item.height.toFloat() / item.width.toFloat()
             if (ratio > maxRatio) {
                 finalCoverH = (width * maxRatio).toInt()
             }
        }
        
        // 重新计算总高度
        val finalTotalH = finalCoverH + (item?.titleHeight ?: 0) + FeedUIConfig.staticContentHeight

        // 测量封面
        ivCover.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(finalCoverH.coerceAtLeast(0), MeasureSpec.EXACTLY)
        )

        // 测量头像
        val avatarSpec = MeasureSpec.makeMeasureSpec(FeedUIConfig.avatarSize, MeasureSpec.EXACTLY)
        ivAvatar.measure(avatarSpec, avatarSpec)

        setMeasuredDimension(width, finalTotalH)
    }

    // 极简 Layout，不涉及文字排版
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val padding = FeedUIConfig.padding

        // 封面铺满顶部
        val coverH = ivCover.measuredHeight
        ivCover.layout(0, 0, width, coverH)

        // 头像位于底部左侧
        val titleH = currentItem?.titleHeight ?: 0
        val bottomAreaTop = coverH + padding + titleH + padding

        ivAvatar.layout(
            padding,
            bottomAreaTop,
            padding + ivAvatar.measuredWidth,
            bottomAreaTop + ivAvatar.measuredHeight
        )
    }

    // 统一绘制文字，无 View 消耗
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas) // 绘制 ivCover 和 ivAvatar

        val item = currentItem ?: return
        val padding = FeedUIConfig.padding
        val coverH = ivCover.measuredHeight

        // 1. 绘制标题
        item.titleLayout?.let {
            canvas.withTranslation(padding.toFloat(), (coverH + padding).toFloat()) {
                // 标题位于封面下方 + padding
                it.draw(this)
            }
        }

        // 底部区域基准线 (头像垂直居中线)
        val bottomAreaTop = coverH + padding + item.titleHeight + padding
        val centerY = bottomAreaTop + FeedUIConfig.avatarSize / 2f

        // 2. 绘制作者名
        item.authorLayout?.let {
            canvas.withSave {
                val authorLeft = padding + FeedUIConfig.avatarSize + padding
                val authorTop = centerY - it.height / 2f
                translate(authorLeft.toFloat(), authorTop)
                it.draw(this)
            }
        }

        // 3. 绘制点赞 (图标 + 数字)
        item.likeLayout?.let { layout ->
            val heartW = cachedHeartDrawable?.bounds?.width() ?: 0
            val gap = 4.dp // 图标和数字间距

            val totalW = heartW + gap + layout.width
            val startX = width - padding - totalW

            // 绘制爱心
            cachedHeartDrawable?.let { drawable ->
                canvas.withSave {
                    val iconTop = centerY - drawable.bounds.height() / 2f
                    translate(startX.toFloat(), iconTop)
                    drawable.draw(this)
                }
            }

            // 绘制数字
            canvas.withSave {
                val textLeft = startX + heartW + gap
                val textTop = centerY - layout.height / 2f
                translate(textLeft.toFloat(), textTop)
                layout.draw(this)
            }
        }
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density + 0.5f).toInt()
}