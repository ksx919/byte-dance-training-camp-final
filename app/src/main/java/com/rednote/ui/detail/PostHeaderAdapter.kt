package com.rednote.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.rednote.data.model.post.PostDetailVO
import com.rednote.databinding.ItemPostHeaderBinding
import java.time.format.DateTimeFormatter

class PostHeaderAdapter : RecyclerView.Adapter<PostHeaderAdapter.HeaderViewHolder>() {

    private var postData: PostDetailVO? = null

    private var thumbWith: Int = 0

    private var thumbHeight: Int = 0

    fun setPostData(data: PostDetailVO, tWidth: Int = 0, tHeight: Int = 0) {
        this.postData = data
        this.thumbWith = tWidth
        this.thumbHeight = tHeight
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val binding = ItemPostHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HeaderViewHolder(binding)
    }

    override fun getItemCount(): Int = if (postData == null) 0 else 1

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        val post = postData ?: return

        with(holder.binding) {
            // 文本信息
            tvTitle.text = post.title
            tvContent.text = post.content

            try {
                val date = java.time.LocalDateTime.parse(post.createdAt)
                val formatter = DateTimeFormatter.ofPattern("MM-dd")
                tvDate.text = "编辑于 ${date.format(formatter)}"
            } catch (e: Exception) {
                tvDate.text = "编辑于 ${post.createdAt}"
            }

            // 图片轮播处理逻辑
            setupImagePager(this, post, holder.imageAdapter)
        }
    }

    private fun setupImagePager(binding: ItemPostHeaderBinding, post: PostDetailVO, adapter: ImagePagerAdapter) {
        val width = post.imgWidth ?: 1
        val height = post.imgHeight ?: 1

        // 限制长图比例
        var ratio = width.toFloat() / height.toFloat()
        if (ratio < 0.75f) ratio = 0.75f

        val params = binding.vpImages.layoutParams as ConstraintLayout.LayoutParams
        params.dimensionRatio = "$ratio"
        binding.vpImages.layoutParams = params

        // 复用传入的 adapter
        if (binding.vpImages.adapter != adapter) {
            binding.vpImages.adapter = adapter
        }

        // 新增：将尺寸传递给ImagePagerAdapter
        adapter.setThumbnailSize(thumbWith, thumbHeight)
        // 设置 postId 并提交数据
        adapter.postId = post.id
        adapter.submitList(post.images ?: emptyList())

        // 计数器逻辑
        val totalCount = post.images?.size ?: 0
        if (totalCount > 1) {
            binding.tvImgIndicator.visibility = View.VISIBLE
            binding.tvImgIndicator.text = "1/$totalCount"
        } else {
            // 如果只有一张图，通常也显示 1/1，或者隐藏
            binding.tvImgIndicator.text = "1/1"
            binding.tvImgIndicator.visibility = View.VISIBLE
        }
    }

    class HeaderViewHolder(val binding: ItemPostHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        val imageAdapter = ImagePagerAdapter()
        
        init {
            // 在 ViewHolder 初始化时注册回调，避免重复注册
            binding.vpImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val total = imageAdapter.currentList.size
                    if (total > 1) {
                        binding.tvImgIndicator.text = "${position + 1}/$total"
                    }
                }
            })
        }
    }
}