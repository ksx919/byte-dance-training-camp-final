package com.rednote.ui.detail

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.rednote.databinding.ItemImagePagerBinding

class ImagePagerAdapter : ListAdapter<String, ImagePagerAdapter.ImageViewHolder>(DiffCallback) {

    var postId: Long = -1L
    private var thumbWidth: Int = 0
    private var thumbHeight: Int = 0

    companion object DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImagePagerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val url = getItem(position)

        // 1. 移除共享元素过渡名 (保持之前的逻辑)
        holder.binding.ivMainImage.transitionName = null

        // 2. 点击事件 (保持不变)
        holder.binding.ivMainImage.setOnClickListener {
            val intent = Intent(holder.itemView.context, ImagePreviewActivity::class.java)
            val images = ArrayList(currentList)
            intent.putStringArrayListExtra("IMAGE_URLS", images)
            intent.putExtra("INITIAL_POSITION", position)
            holder.itemView.context.startActivity(intent)
        }

        // 3. 加载主图
        // 构建缩略图请求 (命中列表页缓存)
        // 关键：必须使用与列表页完全一致的 URL（包含 OSS 参数）才能命中缓存
        val thumbnailUrl = getThumbnailUrl(url, thumbWidth)
        Log.e("ThumbRequest", "thumbWidth: $thumbWidth, thumbHeight: $thumbHeight, url: $thumbnailUrl")

        val thumbRequest = Glide.with(holder.itemView)
            .load(thumbnailUrl)
            .override(thumbWidth, thumbHeight)
            .centerCrop()
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.ALL)

        Glide.with(holder.itemView)
            .load(url)
            .thumbnail(thumbRequest) // 先显示低清图
            .centerCrop()
            .priority(Priority.HIGH)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.binding.ivMainImage)
    }

    fun setThumbnailSize(width: Int, height: Int) {
        this.thumbWidth = width
        this.thumbHeight = height
    }

    // 辅助方法：生成 OSS 缩略图 URL (与 FeedItemView 保持一致以命中缓存)
    private fun getThumbnailUrl(originalUrl: String, width: Int): String {
        if (originalUrl.startsWith("content://") || originalUrl.startsWith("file://") || !originalUrl.startsWith("http")) {
            return originalUrl
        }

        if (originalUrl.contains("aliyuncs.com")) {
            if (originalUrl.contains("x-oss-process")) {
                return originalUrl
            }
            val separator = if (originalUrl.contains("?")) "&" else "?"
            return "$originalUrl${separator}x-oss-process=image/resize,w_$width"
        }
        return originalUrl
    }

    class ImageViewHolder(val binding: ItemImagePagerBinding) : RecyclerView.ViewHolder(binding.root)
}