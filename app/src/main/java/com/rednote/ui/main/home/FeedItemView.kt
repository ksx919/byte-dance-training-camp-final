package com.rednote.ui.main.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
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

    var onLikeClick: ((Long, Boolean) -> Unit)? = null  // 点赞点击回调 (postId, isLiked)

    companion object {
        private var cachedPlaceholder: ColorDrawable? = null
        private var cachedHeartDrawable: Drawable? = null
        private var cachedHeartFilledDrawable: Drawable? = null

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

        // 缓存爱心图标（空心）
        if (cachedHeartDrawable == null) {
            cachedHeartDrawable = ContextCompat.getDrawable(context.applicationContext, R.drawable.ic_favorite)?.mutate()
            // 放大按钮，从12dp改为20dp
            val size = (20 * resources.displayMetrics.density).toInt()
            cachedHeartDrawable?.setBounds(0, 0, size, size)
        }

        // 缓存爱心图标（填充红心）
        if (cachedHeartFilledDrawable == null) {
            cachedHeartFilledDrawable = ContextCompat.getDrawable(context.applicationContext, R.drawable.ic_favorite_filled)?.mutate()
            cachedHeartFilledDrawable?.setTint(Color.parseColor("#FF2442"))
            val size = (20 * resources.displayMetrics.density).toInt()
            cachedHeartFilledDrawable?.setBounds(0, 0, size, size)
        }

        setBackgroundColor(Color.WHITE)
        clipToOutline = true
        outlineProvider = ROUND_RECT_PROVIDER

        // 【关键】允许 ViewGroup 调用 onDraw/dispatchDraw
        setWillNotDraw(false)
    }

    fun bind(item: PostInfo) {
        this.currentItem = item

        // 【核心修复】优先使用本地图片路径（用于本地发布的帖子）
        // 如果 localImageUri 有值，说明是刚发布的，使用它显示本地图片
        val imageToLoad = item.localImageUri ?: item.imageUrl

        // 1. 加载封面
        if (lastCoverUrl != imageToLoad) {
            lastCoverUrl = imageToLoad
            if (!imageToLoad.isNullOrEmpty()) {
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

                // 【优化】使用 OSS 缩略图参数
                val thumbnailUrl = getThumbnailUrl(imageToLoad, targetW)

                Glide.with(context)
                    .load(thumbnailUrl) // Glide 自动处理 File Uri 或 Network URL
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
                // 头像也可以优化，固定请求小图 (例如 100px)
                val avatarUrl = getThumbnailUrl(item.avatarUrl, 100)
                
                Glide.with(context)
                    .load(avatarUrl)
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

        // 【可选】上传中状态视觉反馈
        if (item.status == PostInfo.STATUS_UPLOADING) {
            ivCover.alpha = 0.8f
        } else {
            ivCover.alpha = 1.0f
        }

        // 触发重绘以显示文字
        invalidate()
    }

    // 辅助方法：生成 OSS 缩略图 URL
    private fun getThumbnailUrl(originalUrl: String, width: Int): String {
        // 简单判断是否是阿里云 OSS 链接 (根据实际情况调整域名判断)
        // 排除本地文件路径 (content:// 或 file://)
        if (originalUrl.startsWith("content://") || originalUrl.startsWith("file://") || !originalUrl.startsWith("http")) {
            return originalUrl
        }
        
        if (originalUrl.contains("aliyuncs.com")) {
            // 如果已经包含了处理参数，就不再添加
            if (originalUrl.contains("x-oss-process")) {
                return originalUrl
            }
            val separator = if (originalUrl.contains("?")) "&" else "?"
            val newUrl = "$originalUrl${separator}x-oss-process=image/resize,w_$width"
            android.util.Log.d("FeedImageOpt", "Optimized: $newUrl")
            return newUrl
        }
        return originalUrl
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val item = currentItem

        // 1. 直接获取 ViewModel 算好的总高度
        // 如果 item 为空，兜底使用 0
        val totalH = item?.totalHeight ?: 0

        // 2. 倒推封面高度
        // 封面高度 = 总高度 - (标题高度 + 底部固定区域高度)
        // 这样可以确保：封面 + 文字 + 间距 = 严格等于 totalHeight
        val otherContentH = (item?.titleHeight ?: 0) + FeedUIConfig.staticContentHeight
        val finalCoverH = (totalH - otherContentH).coerceAtLeast(0)

        // 3. 测量封面
        // 使用精确计算出的高度
        ivCover.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(finalCoverH, MeasureSpec.EXACTLY)
        )

        // 4. 测量头像
        val avatarSpec = MeasureSpec.makeMeasureSpec(FeedUIConfig.avatarSize, MeasureSpec.EXACTLY)
        ivAvatar.measure(avatarSpec, avatarSpec)

        // 5. 设置最终尺寸
        // 必须严格等于 item.totalHeight，否则 Adapter 里设置的 layoutParams.height 就白费了
        setMeasuredDimension(width, totalH)
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
            // 根据点赞状态选择图标
            val heartDrawable = if (item.isLiked) cachedHeartFilledDrawable else cachedHeartDrawable
            val heartW = heartDrawable?.bounds?.width() ?: 0
            val gap = 4.dp // 图标和数字间距

            val totalW = heartW + gap + layout.width
            val startX = width - padding - totalW

            // 绘制爱心
            heartDrawable?.let { drawable ->
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

        // 4. 绘制状态标签 (上传中/失败)
        if (item.status != PostInfo.STATUS_NORMAL) {
            val statusText = if (item.status == PostInfo.STATUS_FAILED) "发布失败" else "发布中..."
            val bgColor = if (item.status == PostInfo.STATUS_FAILED) 0xCCFF4444.toInt() else 0xCC000000.toInt() // 红/黑半透明
            
            // 简单的文字绘制逻辑
            val paint = Paint().apply {
                color = Color.WHITE
                textSize = 12 * resources.displayMetrics.density
                isAntiAlias = true
            }
            val bgPaint = Paint().apply {
                color = bgColor
                isAntiAlias = true
                style = Paint.Style.FILL
            }

            val textW = paint.measureText(statusText)
            val textH = paint.descent() - paint.ascent()
            val paddingH = 6.dp
            val paddingV = 4.dp
            
            val badgeW = textW + paddingH * 2
            val badgeH = textH + paddingV * 2
            
            // 绘制在右上角
            val left = width - badgeW - 8.dp
            val top = 8.dp.toFloat()
            val right = width - 8.dp.toFloat()
            val bottom = top + badgeH
            
            canvas.drawRoundRect(left, top, right, bottom, 4.dp.toFloat(), 4.dp.toFloat(), bgPaint)
            canvas.drawText(statusText, left + paddingH, top + paddingV - paint.ascent(), paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val item = currentItem ?: return super.onTouchEvent(event)

        if (event.action == MotionEvent.ACTION_UP) {
            // 计算点赞区域
            val padding = FeedUIConfig.padding
            val coverH = ivCover.measuredHeight
            val bottomAreaTop = coverH + padding + item.titleHeight + padding
            val centerY = bottomAreaTop + FeedUIConfig.avatarSize / 2f

            item.likeLayout?.let { layout ->
                val heartDrawable = if (item.isLiked) cachedHeartFilledDrawable else cachedHeartDrawable
                val heartW = heartDrawable?.bounds?.width() ?: 0
                val heartH = heartDrawable?.bounds?.height() ?: 0
                val gap = 4.dp

                val totalW = heartW + gap + layout.width
                val startX = width - padding - totalW

                // 扩大点击区域，添加额外的padding
                val clickPadding = 20.dp
                val likeLeft = startX - clickPadding
                val likeRight = width - padding + clickPadding
                val likeTop = centerY - heartH / 2f - clickPadding
                val likeBottom = centerY + heartH / 2f + clickPadding

                if (event.x >= likeLeft && event.x <= likeRight &&
                    event.y >= likeTop && event.y <= likeBottom) {
                    // 点击了点赞区域
                    onLikeClick?.invoke(item.id, !item.isLiked)
                    return true
                }
            }
        }

        return super.onTouchEvent(event)
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density + 0.5f).toInt()
}