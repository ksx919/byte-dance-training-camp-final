package com.rednote.ui.detail

import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.rednote.data.api.RetrofitClient
import com.rednote.data.model.comment.AddCommentDTO
import com.rednote.data.model.comment.CommentVO
import com.rednote.data.model.post.PostDetailVO
import com.rednote.ui.base.BaseViewModel
import com.rednote.utils.CommentLayoutCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class PostDetailViewModel : BaseViewModel() {

    // --- 1. UI 状态流 (StateFlow) ---

    // 帖子详情数据
    private val _postDetail = MutableStateFlow<PostDetailVO?>(null)
    val postDetail = _postDetail.asStateFlow()

    // 评论列表数据 (Adapter使用)
    // 这里直接持有 MutableList，方便局部刷新，真实项目中推荐使用 List 并提交新实例
    val commentList = mutableListOf<CommentVO>()

    // 通知 Adapter 刷新的信号 (简单起见，使用 Int 代表刷新类型，或者 Unit)
    private val _commentListUpdateEvent = Channel<Any>()
    val commentListUpdateEvent = _commentListUpdateEvent.receiveAsFlow()

    // 底部 Footer 状态
    private val _footerState = MutableStateFlow(CommentFooterAdapter.STATE_HIDDEN)
    val footerState = _footerState.asStateFlow()

    // 空状态控制
    private val _isEmptyStateVisible = MutableStateFlow(false)
    val isEmptyStateVisible = _isEmptyStateVisible.asStateFlow()

    // 单次事件：滚动到顶部
    private val _scrollToTopEvent = Channel<Unit>()
    val scrollToTopEvent = _scrollToTopEvent.receiveAsFlow()

    // --- 2. 内部变量 ---
    private var postId: Long = -1L
    private var nextCommentCursor: String? = null
    private var hasMoreComments = true
    private var isLoadingComments = false

    fun setPostId(id: Long) {
        this.postId = id
    }

    // --- 3. 业务逻辑方法 ---

    // 加载帖子详情
    fun loadPostDetail() {
        if (postId == -1L) return
        launchDataLoad {
            val response = RetrofitClient.postApiService.getDetail(postId)
            if (response.code == 200 && response.data != null) {
                _postDetail.value = response.data
            } else {
                showToast("内容加载失败，请稍后重试")
            }
        }
    }

    // 加载评论列表 (初始/更多)
    fun loadComments(isRefresh: Boolean = false) {
        if (isLoadingComments) return
        if (!isRefresh && !hasMoreComments) return

        isLoadingComments = true
        _footerState.value = CommentFooterAdapter.STATE_LOADING

        viewModelScope.launch {
            try {
                // 如果是刷新，重置游标
                val cursor = if (isRefresh) null else nextCommentCursor

                val response = RetrofitClient.commentApiService.getFeed(postId, cursor)

                if (response.code == 200 && response.data != null) {
                    val result = response.data
                    val newItems = result.list

                    // 后台计算高度
                    withContext(Dispatchers.Default) {
                        newItems.forEach {
                            it.totalHeight = CommentLayoutCalculator.calculate(it)
                        }
                    }

                    if (isRefresh) {
                        commentList.clear()
                    }
                    commentList.addAll(newItems)

                    // 通知 UI 列表变化
                    _commentListUpdateEvent.send("FULL_UPDATE")

                    nextCommentCursor = result.nextCursor
                    hasMoreComments = result.hasMore

                    // 更新底部状态
                    if (!hasMoreComments && commentList.isNotEmpty()) {
                        _footerState.value = CommentFooterAdapter.STATE_NO_MORE
                    } else {
                        _footerState.value = CommentFooterAdapter.STATE_HIDDEN
                    }

                    _isEmptyStateVisible.value = commentList.isEmpty()
                } else {
                    _footerState.value = CommentFooterAdapter.STATE_HIDDEN
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _footerState.value = CommentFooterAdapter.STATE_HIDDEN
            } finally {
                isLoadingComments = false
            }
        }
    }

    // 展开回复
    fun fetchReplies(comment: CommentVO) {
        if (comment.isLoadingReplies) return

        viewModelScope.launch {
            try {
                comment.isLoadingReplies = true
                // 通知 UI loading 状态
                notifyItemChanged(comment, "PAYLOAD_REPLY")

                val response = RetrofitClient.commentApiService.getReplies(
                    rootId = comment.id,
                    cursor = comment.nextCursor,
                    size = 5
                )

                if (response.code == 200 && response.data != null) {
                    val result = response.data
                    if (comment.childReplies == null) {
                        comment.childReplies = mutableListOf()
                        comment.topReply?.let { comment.childReplies?.add(it) }
                    }

                    val newReplies = result.list.filter { it.id != comment.topReply?.id }
                    comment.childReplies?.addAll(newReplies)

                    comment.nextCursor = result.nextCursor
                    comment.hasMoreReplies = result.hasMore

                    // 重新计算高度
                    withContext(Dispatchers.Default) {
                        comment.totalHeight = CommentLayoutCalculator.calculate(comment)
                    }
                    notifyItemChanged(comment, "PAYLOAD_REPLY")
                } else {
                    showToast("评论加载失败，请稍后重试")
                }
            } catch (_: Exception) {
                showToast("网络连接异常，请检查网络")
            } finally {
                comment.isLoadingReplies = false
                notifyItemChanged(comment, "PAYLOAD_REPLY")
            }
        }
    }

    // 点赞帖子
    fun togglePostLike() {
        val current = _postDetail.value ?: return
        val newStatus = !current.isLiked

        // 乐观更新
        val updatedPost = current.copy(
            isLiked = newStatus,
            likeCount = if (newStatus) current.likeCount + 1 else current.likeCount - 1
        )
        _postDetail.value = updatedPost

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.postApiService.like(current.id, newStatus)
                if (response.code != 200 || response.data != true) {
                    // 失败回滚
                    withContext(Dispatchers.Main) {
                        _postDetail.value = current
                        showToast("操作失败，请重试")
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    _postDetail.value = current
                    showToast("网络连接异常，请检查网络")
                }
            }
        }
    }

    // 点赞评论
    fun toggleCommentLike(comment: CommentVO) {
        val newStatus = !comment.isLiked
        comment.isLiked = newStatus
        comment.likeCount = if (newStatus) comment.likeCount + 1 else comment.likeCount - 1

        notifyItemChanged(comment, "PAYLOAD_LIKE")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.commentApiService.like(comment.id, newStatus)
                if (response.code != 200 || response.data != true) {
                    // 回滚
                    withContext(Dispatchers.Main) {
                        comment.isLiked = !newStatus
                        comment.likeCount = if (!newStatus) comment.likeCount + 1 else comment.likeCount - 1
                        notifyItemChanged(comment, "PAYLOAD_LIKE")
                        showToast("操作失败，请重试")
                    }
                }
            } catch (_: Exception) {
                // 回滚
                withContext(Dispatchers.Main) {
                    comment.isLiked = !newStatus
                    comment.likeCount = if (!newStatus) comment.likeCount + 1 else comment.likeCount - 1
                    notifyItemChanged(comment, "PAYLOAD_LIKE")
                }
            }
        }
    }

    // 发送评论
    fun sendComment(
        localComment: CommentVO,
        filePart: MultipartBody.Part?,
        width: Int?,
        height: Int?,
        isRoot: Boolean
    ) {
        // 1. 本地立即插入 (UI 逻辑在 Activity 做，或者在这里处理 list)
        if (isRoot) {
            commentList.add(0, localComment)
            _isEmptyStateVisible.value = false
            // 通知 UI 列表全量刷新或插入
            viewModelScope.launch {
                _commentListUpdateEvent.send("INSERT_TOP")
                _scrollToTopEvent.send(Unit)
            }
        } else {
            // 回复逻辑：找到父节点插入
            val parentIndex = commentList.indexOfFirst { it.id == localComment.tempRootId }
            if (parentIndex != -1) {
                val parent = commentList[parentIndex]
                if (parent.childReplies == null) {
                    parent.childReplies = mutableListOf()
                    parent.topReply?.let { parent.childReplies?.add(it) }
                }
                parent.childReplies?.add(localComment)
                // 计算高度
                viewModelScope.launch(Dispatchers.Default) {
                    parent.totalHeight = CommentLayoutCalculator.calculate(parent)
                    withContext(Dispatchers.Main) {
                        notifyItemChanged(parent, "PAYLOAD_REPLY")
                    }
                }
            }
        }

        // 2. 后台上传
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val commentDTO = AddCommentDTO(
                    postId = postId,
                    content = localComment.content,
                    parentId = localComment.tempParentId,
                    rootParentId = localComment.tempRootId,
                    replyToUserId = localComment.tempReplyToUserId,
                    imageWidth = width,
                    imageHeight = height
                )

                val gson = Gson()
                val requestBody = gson.toJson(commentDTO).toRequestBody("application/json".toMediaTypeOrNull())

                val response = RetrofitClient.commentApiService.addComment(requestBody, filePart)

                withContext(Dispatchers.Main) {
                    if (response.code == 200 && response.data != null) {
                        val serverComment = response.data
                        
                        // 替换本地数据为服务器数据
                        if (isRoot) {
                            // 使用引用查找，避免因属性变化导致 equals 失败
                            val index = commentList.indexOfFirst { it === localComment }
                            if (index != -1) {
                                // 预计算高度
                                withContext(Dispatchers.Default) {
                                    serverComment.totalHeight = CommentLayoutCalculator.calculate(serverComment)
                                }
                                commentList[index] = serverComment
                                // notifyItemChanged(serverComment) // 全量刷新这个 Item
                                // 【修复】必须发送 FULL_UPDATE 触发 submitList，否则 Adapter 的 currentList 不会更新
                                _commentListUpdateEvent.send("FULL_UPDATE")
                            }
                        } else {
                            // 替换子评论
                            val parentIndex = commentList.indexOfFirst { it.id == localComment.tempRootId }
                            if (parentIndex != -1) {
                                val parent = commentList[parentIndex]
                                // 同样使用引用查找
                                val childIndex = parent.childReplies?.indexOfFirst { it === localComment } ?: -1
                                if (childIndex != -1) {
                                    parent.childReplies?.set(childIndex, serverComment)
                                    // 重新计算父评论高度（因为子评论变了）
                                    withContext(Dispatchers.Default) {
                                        parent.totalHeight = CommentLayoutCalculator.calculate(parent)
                                    }
                                    _commentListUpdateEvent.send("FULL_UPDATE")
                                }
                            }
                        }

                        // 更新评论数 (需要从 postDetail 获取并更新)
                        val currentPost = _postDetail.value
                        if (currentPost != null) {
                            _postDetail.value = currentPost.copy(commentCount = currentPost.commentCount + 1)
                        }
                    } else {
                        localComment.status = CommentVO.STATUS_FAILED
                        notifyItemChanged(localComment)
                        showToast("评论发送失败，请重试")
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    localComment.status = CommentVO.STATUS_FAILED
                    notifyItemChanged(localComment)
                    showToast("评论发送失败，请检查网络")
                }
            }
        }
    }

    // 辅助方法：通知列表更新某个 Item
    private fun notifyItemChanged(comment: CommentVO, payload: Any? = null) {
        viewModelScope.launch {
            _commentListUpdateEvent.send(Pair(comment, payload))
        }
    }
}