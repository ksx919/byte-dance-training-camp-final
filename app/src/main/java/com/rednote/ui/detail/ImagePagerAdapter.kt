package com.rednote.ui.detail

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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

        // 1. 移除共享元素名称
        holder.binding.ivMainImage.transitionName = null

        // 2. 点击查看大图
        holder.binding.ivMainImage.setOnClickListener {
            val intent = Intent(holder.itemView.context, ImagePreviewActivity::class.java)
            val images = ArrayList(currentList)
            intent.putStringArrayListExtra("IMAGE_URLS", images)
            intent.putExtra("INITIAL_POSITION", position)
            holder.itemView.context.startActivity(intent)
        }

        // 3. 加载模糊背景
        Glide.with(holder.itemView)
            .load(url)
            .override(200, 200)
            .centerCrop()
            .into(holder.binding.ivBlurBackground)

        val thumbnailRequest = Glide.with(holder.itemView)
            .load(url)
            .override(thumbWidth,thumbHeight)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)

        // 4. 加载主图
        Glide.with(holder.itemView)
            .load(url)
            .thumbnail(thumbnailRequest)
            .fitCenter()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.binding.ivMainImage)
    }

    fun setThumbnailSize(width: Int, height: Int) {
        this.thumbWidth = width
        this.thumbHeight = height
    }

    class ImageViewHolder(val binding: ItemImagePagerBinding) : RecyclerView.ViewHolder(binding.root)
}