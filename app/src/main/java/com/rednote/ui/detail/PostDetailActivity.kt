package com.rednote.ui.detail

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.viewModels // 需要 fragment-ktx 库
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rednote.R
import com.rednote.data.model.comment.CommentVO
import com.rednote.data.model.post.PostDetailVO
import com.rednote.databinding.ActivityPostDetailBinding
import com.rednote.ui.base.BaseActivity
import com.rednote.utils.CommentLayoutCalculator
import com.rednote.utils.CommentUIConfig
import com.rednote.utils.FeedUIConfig
import com.rednote.utils.ImageSizeUtils
import com.rednote.utils.UserManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.LocalDateTime

class PostDetailActivity : BaseActivity<ActivityPostDetailBinding>() {

    // 1. 获取 ViewModel (使用委托)
    private val viewModel: PostDetailViewModel by viewModels()

    // 2. Adapters (保持不变)
    private val headerAdapter = PostHeaderAdapter()
    private val commentHeaderAdapter = CommentHeaderAdapter(
        onInputClick = { showCommentDialog() }
    )
    private val commentEmptyAdapter = CommentEmptyAdapter()
    private lateinit var commentAdapter: CommentAdapter // 延迟初始化以便设置回调
    private val footerAdapter = CommentFooterAdapter()

    override fun getViewBinding(): ActivityPostDetailBinding {
        return ActivityPostDetailBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 初始化计算工具
        CommentUIConfig.init(this)

        // 初始化 Adapter 回调
        commentAdapter = CommentAdapter(
            onExpandReplies = { viewModel.fetchReplies(it) },
            onReplyClick = { comment, rootId -> showCommentDialog(comment, rootId) },
            onRetryClick = {
                // 重试逻辑：简单处理为重新调用发送，需要传递参数，这里简化处理
                // 实际建议在 VM 中封装 retrySend(comment)
            },
            onLikeClick = { viewModel.toggleCommentLike(it) },
            onImageClick = { imageUrl ->
                val intent = Intent(this, ImagePreviewActivity::class.java)
                intent.putExtra("IMAGE_URL", imageUrl)
                startActivity(intent)
            }
        )

        // 设置 RecyclerView
        val concatAdapter = ConcatAdapter(headerAdapter, commentHeaderAdapter, commentEmptyAdapter, commentAdapter, footerAdapter)
        binding.rvContent.apply {
            val linearManager = LinearLayoutManager(this@PostDetailActivity)
            linearManager.initialPrefetchItemCount = 4
            layoutManager = linearManager
            adapter = concatAdapter

            // 滚动监听：加载更多
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val visibleItemCount = linearManager.childCount
                    val totalItemCount = linearManager.itemCount
                    val firstVisibleItemPosition = linearManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 && firstVisibleItemPosition >= 0) {
                        viewModel.loadComments(isRefresh = false)
                    }
                }
            })
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.llLike.setOnClickListener { viewModel.togglePostLike() }
        binding.tvInput.setOnClickListener { showCommentDialog() }
    }

    override fun initData() {
        val postId = intent.getLongExtra("POST_ID", -1L)
        if (postId == -1L) {
            showToast("参数错误")
            finish()
            return
        }
        viewModel.setPostId(postId)

        // 【核心修改】检查是否是本地帖子
        val isLocalPost = intent.getBooleanExtra("IS_LOCAL_POST", false)

        if (isLocalPost) {
            // --- A. 如果是本地帖子：直接构造数据渲染，不请求网络 ---
            val localVO = PostDetailVO(
                id = postId,
                title = intent.getStringExtra("POST_TITLE") ?: "",
                // 获取 Intent 里的正文，如果为空则设为""，避免骨架屏
                content = intent.getStringExtra("POST_CONTENT") ?: "",
                images = listOfNotNull(intent.getStringExtra("POST_IMAGE")),
                imgWidth = intent.getIntExtra("POST_WIDTH", 0),
                imgHeight = intent.getIntExtra("POST_HEIGHT", 0),
                createdAt = "刚刚", // 本地帖子时间
                authorId = 0L,
                authorName = intent.getStringExtra("POST_AUTHOR") ?: "",
                authorAvatar = intent.getStringExtra("POST_AVATAR"),
                likeCount = 0,
                commentCount = 0,
                isLiked = false
            )

            // 1. 更新 UI
            updatePostUI(localVO)

            // 2. 隐藏评论区和底部栏 (因为本地帖子不能评论)
            footerAdapter.updateState(CommentFooterAdapter.STATE_HIDDEN)
            commentEmptyAdapter.setVisible(false) // 或者显示一个提示“发布成功后可评论”

            // 3. 禁用点赞
            binding.llLike.alpha = 0.5f
            binding.llLike.isEnabled = false

            return // 【重要】直接返回，不执行下面的网络请求
        }

        // --- B. 如果是网络帖子：执行原有逻辑 ---

        // 1. 处理 Intent 预加载数据 (实现秒开体验)
        handleIntentSnapshot()

        // 2. 开始观察 ViewModel 数据
        observeViewModel()

        // 3. 触发网络请求
        viewModel.loadPostDetail()
        viewModel.loadComments(isRefresh = true)
    }

    private fun handleIntentSnapshot() {
        val preTitle = intent.getStringExtra("POST_TITLE")
        // 如果有预加载数据，先构建一个临时 VO 更新 UI
        if (!preTitle.isNullOrEmpty()) {
            val tempVO = PostDetailVO(
                id = intent.getLongExtra("POST_ID", -1L),
                title = preTitle,
                content = "",
                images = listOfNotNull(intent.getStringExtra("POST_IMAGE")),
                imgWidth = intent.getIntExtra("POST_WIDTH", 0),
                imgHeight = intent.getIntExtra("POST_HEIGHT", 0),
                createdAt = "",
                authorId = 0L,
                authorName = intent.getStringExtra("POST_AUTHOR") ?: "",
                authorAvatar = intent.getStringExtra("POST_AVATAR"),
                likeCount = intent.getIntExtra("POST_LIKES", 0),
                commentCount = 0,
                isLiked = false
            )
            updatePostUI(tempVO)
        }
    }

    // 核心：观察 VM 状态
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. 监听 Loading (来自 BaseViewModel)
                launch {
                    viewModel.isLoading.collect {
                        // 如果有全局 loading 可以在这里显示，通常详情页不需要全屏 loading
                    }
                }

                // 2. 监听 Toast (来自 BaseViewModel)
                launch {
                    viewModel.toastEvent.collect { showToast(it) }
                }

                // 3. 监听帖子详情
                launch {
                    viewModel.postDetail.collect { post ->
                        post?.let { updatePostUI(it) }
                    }
                }

                // 4. 监听评论列表更新事件
                launch {
                    viewModel.commentListUpdateEvent.collect { event ->
                        when(event) {
                            "FULL_UPDATE" -> commentAdapter.submitList(ArrayList(viewModel.commentList))
                            "INSERT_TOP" -> {
                                commentAdapter.submitList(ArrayList(viewModel.commentList))
                                // 提交后可能需要滚动，在下面处理
                            }
                            is Pair<*, *> -> {
                                // 局部更新
                                val comment = event.first as CommentVO
                                val payload = event.second
                                updateCommentInList(comment, payload)
                            }
                        }
                    }
                }

                // 5. 监听 Footer 状态
                launch {
                    viewModel.footerState.collect { state ->
                        footerAdapter.updateState(state)
                    }
                }

                // 6. 监听空状态
                launch {
                    viewModel.isEmptyStateVisible.collect { visible ->
                        commentEmptyAdapter.setVisible(visible)
                    }
                }

                // 7. 监听滚动事件
                launch {
                    viewModel.scrollToTopEvent.collect {
                        binding.rvContent.scrollToPosition(1) // 0是header
                    }
                }
            }
        }
    }

    private fun updatePostUI(data: PostDetailVO) {
        commentAdapter.postAuthorId = data.authorId

        // 计算图片比例 (UI Config 逻辑)
        val (width,height) = ImageSizeUtils.calculateCoverSize(
            data.imgWidth ?:0,
            data.imgHeight?:0
        )

        // Header Adapter 更新
        headerAdapter.setPostData(data, width, height)
        commentHeaderAdapter.setCommentCount(data.commentCount)

        // 底部栏更新
        binding.tvLikeCount.text = data.likeCount.toString()
        binding.tvCommentCount.text = data.commentCount.toString()

        // 更新点赞图标颜色
        if (data.isLiked) {
            binding.ivLikeIcon.setImageResource(R.drawable.ic_favorite_filled)
            binding.ivLikeIcon.setColorFilter("#FF2442".toColorInt())
        } else {
            binding.ivLikeIcon.setImageResource(R.drawable.ic_favorite)
            binding.ivLikeIcon.setColorFilter("#333333".toColorInt())
        }

        // 作者信息
        binding.tvAuthorName.text = data.authorName
        Glide.with(this).load(data.authorAvatar).circleCrop().into(binding.ivAuthorAvatar)
    }

    // 辅助方法：处理 Adapter 的局部刷新 (从 Activity 原逻辑搬运并简化)
    private fun updateCommentInList(comment: CommentVO, payload: Any? = null) {
        val currentList = commentAdapter.currentList
        val index = currentList.indexOf(comment)
        if (index != -1) {
            commentAdapter.notifyItemChanged(index, payload)
        } else {
            // 查找是否是回复
            currentList.forEachIndexed { i, parent ->
                val isChildReply = parent.childReplies?.contains(comment) == true
                val isTopReply = parent.topReply?.id == comment.id
                if (isChildReply || isTopReply) {
                    val effectivePayload = if (payload == "PAYLOAD_LIKE") "PAYLOAD_REPLY_LIKE" else "PAYLOAD_REPLY"
                    commentAdapter.notifyItemChanged(i, effectivePayload)
                    return
                }
            }
        }
    }

    // --- 评论弹窗与图片上传逻辑 (属于 View 层与 VM 的交互) ---

    private fun showCommentDialog(replyToComment: CommentVO? = null, rootId: Long? = null) {
        val dialog = CommentInputDialog.newInstance(
            postId = intent.getLongExtra("POST_ID", -1L),
            parentId = replyToComment?.id,
            rootParentId = rootId,
            replyToUserId = replyToComment?.userId,
            replyToUserName = replyToComment?.nickname
        )
        dialog.onSendComment = { content, imageUri, pId, rId, rUserId, rUserName ->
            handleSendComment(content, imageUri, pId, rId, rUserId, rUserName)
        }
        dialog.show(supportFragmentManager, "CommentInputDialog")
    }

    private fun handleSendComment(
        content: String,
        imageUri: Uri?,
        parentId: Long?,
        rootParentId: Long?,
        replyToUserId: Long?,
        replyToUserName: String?
    ) {
        // 1. 构造本地对象
        val currentUser = UserManager.getUser()
        val localComment = CommentVO(
            id = -System.currentTimeMillis(),
            postId = viewModel.postDetail.value?.id ?: -1L,
            userId = currentUser?.id ?: 0L,
            nickname = currentUser?.nickname ?: "我",
            avatarUrl = currentUser?.avatarUrl ?: "",
            content = content,
            likeCount = 0,
            isLiked = false,
            createdAt = LocalDateTime.now().toString(),

            // --- 补全缺失的参数 (与原代码保持一致) ---
            imageUrl = null,       // 新评论暂时没有网络图片URL
            imageWidth = null,     // 宽高在 apply 中通过 Uri 另行处理，或者此处传 null
            imageHeight = null,
            topReply = null,       // 新评论没有置顶回复
            replyCount = 0,        // 新评论回复数为 0
            // -------------------------------------

            targetUserNickname = replyToUserName
        ).apply {
            status = CommentVO.STATUS_SENDING
            localImageUri = imageUri
            tempParentId = parentId
            tempRootId = rootParentId
            tempReplyToUserId = replyToUserId
            totalHeight = CommentLayoutCalculator.calculate(this)
        }

        // 2. 准备文件上传所需参数
        var filePart: MultipartBody.Part? = null
        var width: Int? = null
        var height: Int? = null

        if (imageUri != null) {
            val file = uriToFile(imageUri)
            if (file != null) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, options)
                width = options.outWidth
                height = options.outHeight
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
            }
        }

        // 3. 移交 ViewModel
        viewModel.sendComment(localComment, filePart, width, height, isRoot = (rootParentId == null))
    }

    private fun uriToFile(uri: Uri): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload", ".jpg", cacheDir)
            tempFile.outputStream().use { inputStream.copyTo(it) }
            return tempFile
        } catch (e: Exception) { return null }
    }
}