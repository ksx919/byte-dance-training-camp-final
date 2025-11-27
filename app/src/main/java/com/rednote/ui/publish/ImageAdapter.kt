package com.rednote.ui.publish

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rednote.databinding.ItemPublishImageBinding

class ImageAdapter(
    private val onImageDelete: (Int) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    private var imageList: List<Uri> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemPublishImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding, onImageDelete)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageList[position], position)
    }

    override fun getItemCount(): Int = imageList.size

    /**
     * 更新图片列表（由ViewModel的状态驱动）
     */
    fun updateImages(images: List<Uri>) {
        imageList = images
        notifyDataSetChanged()
    }

    inner class ImageViewHolder(
        private val binding: ItemPublishImageBinding,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri, position: Int) {
            // 加载图片（这里使用简单的设置，实际项目中应该使用图片加载库如Glide）
            binding.ivImage.setImageURI(uri)
            
            // 添加删除按钮点击事件
            binding.btnDelete.setOnClickListener {
                onDelete(position)
            }
        }
    }
}

