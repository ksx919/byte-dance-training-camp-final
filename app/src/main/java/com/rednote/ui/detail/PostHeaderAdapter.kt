package com.rednote.ui.detail

import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.rednote.data.model.post.PostDetailVO
import com.rednote.databinding.ItemPostHeaderBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PostHeaderAdapter : RecyclerView.Adapter<PostHeaderAdapter.HeaderViewHolder>() {

    private var postData: PostDetailVO? = null

    private var thumbWidth: Int = 0

    private var thumbHeight: Int = 0

    fun setPostData(data: PostDetailVO, tWidth: Int = 0, tHeight: Int = 0) {
        this.postData = data
        this.thumbWidth = tWidth
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
            
            // 骨架屏逻辑
            if (post.content.isNullOrEmpty()) {
                // 初始状态：显示骨架，隐藏内容
                llContentSkeleton.visibility = View.VISIBLE
                tvContent.visibility = View.GONE
                tvContent.text = ""
            } else {
                // 加载完成：有内容了
                if (tvContent.visibility != View.VISIBLE) {
                    // 如果之前是隐藏的，说明是刚加载出来，执行动画
                    TransitionManager.beginDelayedTransition(root)
                }
                llContentSkeleton.visibility = View.GONE
                tvContent.visibility = View.VISIBLE
                tvContent.text = post.content
            }

            try {
                // 如果时间为空，暂时隐藏或显示默认
                if (post.createdAt.isNullOrEmpty()) {
                     tvDate.text = ""
                } else {
                    val date = LocalDateTime.parse(post.createdAt)
                    val formatter = DateTimeFormatter.ofPattern("MM-dd")
                    tvDate.text = "编辑于 ${date.format(formatter)}"
                }
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

        // 1. 计算父容器 ViewPager 的布局比例
        var ratio = width.toFloat() / height.toFloat()
        if (ratio < 0.75f) ratio = 0.75f // 限制最大长宽比 3:4

        val params = binding.vpImages.layoutParams as ConstraintLayout.LayoutParams
        val newRatioStr = "$ratio"
        if (params.dimensionRatio != newRatioStr) {
            params.dimensionRatio = newRatioStr
            binding.vpImages.layoutParams = params
        }

        // 2. 配置 Adapter 的参数
        adapter.setThumbnailSize(thumbWidth, thumbHeight)
        adapter.postId = post.id

        // 3. 关联 Adapter
        if (binding.vpImages.adapter != adapter) {
            binding.vpImages.adapter = adapter

            // 【核心优化1】设置离屏缓存数量为 1
            // 这会让 ViewPager2 自动创建并渲染左右相邻的页面（即第2张图会被立即渲染成View）
            // 注意：不要设置太大，1 是内存和流畅度的最佳平衡点
            binding.vpImages.offscreenPageLimit = 1
        }

        // 4. 提交数据
        val images = post.images ?: emptyList()
        adapter.submitList(images)

        // 5. 指示器逻辑
        val totalCount = images.size
        if (totalCount > 1) {
            binding.tvImgIndicator.visibility = View.VISIBLE
            binding.tvImgIndicator.text = "1/$totalCount"
        } else {
            binding.tvImgIndicator.text = "1/1"
            binding.tvImgIndicator.visibility = View.VISIBLE
        }

        // 6. 【核心优化2】静默预下载后续所有图片 (解决快速滑动白屏)
        // offscreenPageLimit 只能解决下一张，如果用户滑得很快，后面的图来不及渲染。
        // 这里使用 preload() 将剩余图片提前下载到磁盘缓存，但不创建 View，不占显存。
        if (images.size > 1) {
            binding.root.post {
                try {
                    // 从第2张开始预加载 (drop(1))
                    images.drop(1).forEach { imageUrl ->
                        Glide.with(binding.root.context)
                            .load(imageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL) // 确保缓存策略一致
                            .preload() // 关键：只下载，不显示
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
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