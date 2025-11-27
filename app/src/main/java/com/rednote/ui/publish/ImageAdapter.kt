package com.rednote.ui.publish

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rednote.databinding.ItemPublishAddBinding
import com.rednote.databinding.ItemPublishImageBinding

class ImageAdapter(
    private val onAddClick: () -> Unit
) : ListAdapter<Uri, RecyclerView.ViewHolder>(UriDiffCallback) {

    companion object {
        private const val TYPE_IMAGE = 0
        private const val TYPE_ADD = 1
    }

    object UriDiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            // Uri 本身就是唯一的，可以直接比
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            // 即使 URI 相同，由于位置可能改变（序号会变），
            // 我们需要强制重新绑定来更新序号
            return false
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
            // 使用 getItem(position) 获取数据
            holder.bind(getItem(position), position)
        } else if (holder is AddViewHolder) {
            holder.bind()
        }
    }

    // 这里的数量是：真实图片数 + 1 个加号
    override fun getItemCount(): Int = currentList.size + 1

    override fun getItemViewType(position: Int): Int {
        // 如果位置小于图片数量，则是图片；否则是最后一个加号
        return if (position < currentList.size) TYPE_IMAGE else TYPE_ADD
    }

    fun updateImages(images: List<Uri>) {
        // submitList 是 ListAdapter 自带的方法
        // 它会自动在后台计算差异，然后平滑刷新 UI
        submitList(images)
    }

    class ImageViewHolder(
        private val binding: ItemPublishImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: Uri, position: Int) {
            binding.ivImage.setImageURI(uri)
            binding.tvSequence.text = (position + 1).toString()
        }
    }

    class AddViewHolder(
        private val binding: ItemPublishAddBinding,
        private val onAddClick: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.root.setOnClickListener { onAddClick() }
        }
    }
}