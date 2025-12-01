package com.rednote.ui.publish

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rednote.databinding.ItemPublishAddBinding
import com.rednote.databinding.ItemPublishImageBinding
import java.io.File

class ImageAdapter(
    private val onAddClick: () -> Unit
) : ListAdapter<Uri, RecyclerView.ViewHolder>(UriDiffCallback) {

    companion object {
        private const val TYPE_IMAGE = 0
        private const val TYPE_ADD = 1
    }

    object UriDiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            // Uri 是唯一的,直接对比
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            // 【性能优化】这里改为 return oldItem == newItem
            // 之前返回 false 会导致只要 List 变动,所有图片(包括没变的)都会闪烁重绘
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_IMAGE) {
            val binding = ItemPublishImageBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            ImageViewHolder(binding)
        } else {
            val binding = ItemPublishAddBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            AddViewHolder(binding, onAddClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ImageViewHolder) {
            holder.bind(getItem(position), position)
        } else if (holder is AddViewHolder) {
            holder.bind()
        }
    }

    override fun getItemCount(): Int = currentList.size + 1

    override fun getItemViewType(position: Int): Int {
        // 最后一个位置始终是 TYPE_ADD
        return if (position == currentList.size) TYPE_ADD else TYPE_IMAGE
    }

    fun updateImages(images: List<Uri>) {
        submitList(images)
    }

    class ImageViewHolder(
        private val binding: ItemPublishImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: Uri, position: Int) {
            // 添加日志
            android.util.Log.d("ImageAdapter", "bind: Loading image at position $position, URI: $uri")
            if (uri.scheme == "file") {
                val file = File(uri.path!!)
                android.util.Log.d("ImageAdapter", "bind: File exists: ${file.exists()}, size: ${file.length()}")
            }
            
            Glide.with(binding.root.context)
                .load(uri)
                .centerCrop()
                .into(binding.ivImage)
            binding.tvSequence.text = (position + 1).toString()
        }
    }

    class AddViewHolder(
        private val binding: ItemPublishAddBinding,
        private val onAddClick: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            // 点击该按钮触发 onAddClick,在 Activity 中打开相册选择器
            binding.root.setOnClickListener { onAddClick() }
        }
    }
}