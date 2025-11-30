package com.rednote.ui.detail

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.rednote.R
import com.rednote.data.api.RetrofitClient
import com.rednote.data.model.post.PostDetailVO
import com.rednote.databinding.ActivityPostDetailBinding
import com.rednote.utils.FeedUIConfig
import kotlinx.coroutines.launch

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding
    private val headerAdapter = PostHeaderAdapter()

    // 当前页面持有的数据对象
    private var currentPost: PostDetailVO? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 移除共享元素动画逻辑
        // supportPostponeEnterTransition()

        initView()
        
        // 1. 优先从 Intent 读取预加载数据，实现秒开
        val preTitle = intent.getStringExtra("POST_TITLE")
        val preImage = intent.getStringExtra("POST_IMAGE")
        val preWidth = intent.getIntExtra("POST_WIDTH", 0)
        val preHeight = intent.getIntExtra("POST_HEIGHT", 0)
        val preAuthor = intent.getStringExtra("POST_AUTHOR")
        val preAvatar = intent.getStringExtra("POST_AVATAR")
        val preLikes = intent.getIntExtra("POST_LIKES", 0)
        
        if (!preTitle.isNullOrEmpty()) {
            val tempVO = PostDetailVO(
                id = intent.getLongExtra("POST_ID", -1L),
                title = preTitle,
                content = "",
                images = if (preImage != null) listOf(preImage) else emptyList(),
                imgWidth = preWidth,
                imgHeight = preHeight,
                createdAt = "", // 时间暂时为空
                authorId = 0L,
                authorName = preAuthor ?: "",
                authorAvatar = preAvatar,
                likeCount = preLikes,
                commentCount = 0,
                isLiked = false
            )
            updateUI(tempVO)
        }
        
        loadData()
    }

    private fun initView() {
        // 设置 RecyclerView
        val concatAdapter = ConcatAdapter(headerAdapter)
        // 后续接入评论区：concatAdapter.addAdapter(commentAdapter)

        binding.rvContent.apply {
            layoutManager = LinearLayoutManager(this@PostDetailActivity)
            adapter = concatAdapter
        }

        binding.btnBack.setOnClickListener { finish() }

        // 底部点赞交互
        binding.llLike.setOnClickListener { toggleLikeState() }
    }

    private fun loadData() {
        val postId = intent.getLongExtra("POST_ID", -1L)
        if (postId == -1L) {
            Toast.makeText(this, "参数错误: 未找到帖子ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.postApiService.getDetail(postId)
                if (response.code == 200 && response.data != null) {
                    updateUI(response.data)
                } else {
                    Toast.makeText(this@PostDetailActivity, "加载失败: ${response.msg}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@PostDetailActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(data: PostDetailVO) {
        this.currentPost = data

        val listConfigWith = FeedUIConfig.itemWidth
        var listConfigHeight = listConfigWith

        val originWidth = data.imgWidth ?: 0
        val originHeight = data.imgHeight ?:0

        if (originWidth > 0 && originHeight > 0) {
            val ratio = originHeight.toFloat() / originWidth.toFloat()
            // 最大比例限制1.33f
            val maxRatio = 1.33f
            val finalRatio = if (ratio > maxRatio) maxRatio else ratio
            listConfigHeight = (listConfigWith * finalRatio).toInt()
        }

        // 1. 【顶部栏】更新作者信息
        val avatarSize = FeedUIConfig.avatarSize

        val avatarThumb = Glide.with(this)
            .load(data.authorAvatar)
            .override(avatarSize, avatarSize) // 关键1：尺寸必须匹配列表页
            .circleCrop()                     // 关键2：变换必须匹配列表页
            .diskCacheStrategy(DiskCacheStrategy.ALL)

        binding.tvAuthorName.text = data.authorName
        Glide.with(this)
            .load(data.authorAvatar)
            .thumbnail(avatarThumb)
            .circleCrop()
            .into(binding.ivAuthorAvatar)

        // 2. 【列表】更新内容 (图片、标题、正文)
        headerAdapter.setPostData(data, listConfigWith, listConfigHeight)

        // 3. 【底部栏】更新数据
        binding.tvLikeCount.text = data.likeCount.toString()
        binding.tvCommentCount.text = data.commentCount.toString()
        updateLikeIconUI(data.isLiked)
    }

    private fun updateLikeIconUI(isLiked: Boolean) {
        if (isLiked) {
            binding.ivLikeIcon.setImageResource(R.drawable.ic_favorite_filled)
            binding.ivLikeIcon.setColorFilter("#FF2442".toColorInt()) // 红色
        } else {
            binding.ivLikeIcon.setImageResource(R.drawable.ic_favorite)
            binding.ivLikeIcon.setColorFilter("#333333".toColorInt()) // 深灰
        }
    }

    private fun toggleLikeState() {
        val post = currentPost ?: return

        // 1. 改变状态
        val newStatus = !post.isLiked
        post.isLiked = newStatus

        // 2. 改变计数
        if (newStatus) {
            post.likeCount += 1
        } else {
            post.likeCount -= 1
        }

        // 3. 刷新 UI
        binding.tvLikeCount.text = post.likeCount.toString()
        updateLikeIconUI(newStatus)

        // TODO: 这里调用 ViewModel 发送网络请求同步给后端
    }
}