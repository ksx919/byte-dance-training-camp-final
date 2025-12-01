package com.rednote.ui.main.home

import androidx.lifecycle.viewModelScope
import com.rednote.data.api.RetrofitClient
import com.rednote.data.model.post.PendingPost
import com.rednote.data.model.post.PostInfo
import com.rednote.ui.base.BaseViewModel
import com.rednote.utils.FeedUIConfig
import com.rednote.utils.ImageSizeUtils
import com.rednote.utils.PostUploadManager
import com.rednote.utils.TextMeasureUtils
import com.rednote.utils.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContentViewModel : BaseViewModel() {

    // 服务器返回的帖子列表
    private val _serverPosts = MutableStateFlow<List<PostInfo>>(emptyList())

    // 本地点赞状态缓存 <PostId, Pair<IsLiked, LikeCount>>
    private val _localLikeUpdates = MutableStateFlow<Map<Long, Pair<Boolean, Int>>>(emptyMap())

    // 最终展示的列表 = 本地待发布帖子 + 服务器帖子 + 本地点赞状态
    val feedList: StateFlow<List<PostInfo>> = combine(
        PostUploadManager.pendingPosts,
        _serverPosts,
        _localLikeUpdates
    ) { pendingList, serverList, likeUpdates ->
        mergePosts(pendingList, serverList, likeUpdates)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _isLoadingFlow = MutableStateFlow(false)
    val isLoadingFlow: StateFlow<Boolean> = _isLoadingFlow // 这里不需要 asStateFlow，因为已经是 StateFlow

    private var lastId: String? = null
    // private var hasMore: Boolean = true // 未使用，暂略

    private var isFirstLoad = true
    private var previousPendingCount = 0

    init {
        viewModelScope.launch {
            PostUploadManager.pendingPosts.collect { list ->
                // 如果列表变长了，说明有新任务添加
                if (list.size > previousPendingCount) {
                    // 只有当新任务是 PENDING 状态时才刷新
                    val hasNewPending = list.any { it.status == PendingPost.STATUS_PENDING }
                    if (hasNewPending) {
                        // 清空当前列表并刷新
                        _serverPosts.value = emptyList()
                        loadFeed(isRefresh = true, size = 4)
                    }
                }
                previousPendingCount = list.size
            }
        }
    }

    /**
     * 合并本地和网络帖子
     */
    private suspend fun mergePosts(
        pendingList: List<PendingPost>,
        serverList: List<PostInfo>,
        likeUpdates: Map<Long, Pair<Boolean, Int>>
    ): List<PostInfo> {
        // 1. 将 PendingPost 转换为 PostInfo
        // 获取当前用户信息
        val currentUser = UserManager.getUser()
        val currentAuthor = currentUser?.nickname ?: "我"
        val currentAvatar = currentUser?.avatarUrl

        val localPosts = pendingList.map { pending ->
            // 映射状态
            val uiStatus = when (pending.status) {
                PendingPost.STATUS_FAILED -> PostInfo.STATUS_FAILED
                PendingPost.STATUS_SUCCESS -> PostInfo.STATUS_NORMAL // 成功后暂时显示为正常，等待动画或刷新
                else -> PostInfo.STATUS_UPLOADING
            }

            // 如果已经上传成功并拿到了 serverId，就用 serverId，否则用本地 ID
            val finalId = if (pending.serverId != null && pending.serverId!! > 0) {
                pending.serverId!!
            } else {
                -(pending.localId.hashCode().toLong())
            }

            // 应用本地点赞状态
            val (isLiked, likeCount) = likeUpdates[finalId] ?: Pair(false, 0)

            val postInfo = PostInfo(
                id = finalId,
                title = pending.title,
                author = currentAuthor,
                avatarUrl = currentAvatar,
                likeCount = likeCount,
                isLiked = isLiked,
                imageUrl = null,
                width = 1080, // 默认宽高
                height = 1440,
                content = pending.content,
                status = uiStatus,
                localImageUri = pending.imageUris.firstOrNull(),
                failMessage = pending.errorMessage,
                localId = pending.localId // 【关键】传递 localId
            )
            
            // 预计算布局
            preCalculateItem(postInfo)
        }

        // 2. 过滤掉服务器列表中已经包含的本地发布成功的帖子 (通过 serverId 判断)
        // 防止出现“本地显示成功”和“服务器拉取到”重复显示的情况
        val successServerIds = pendingList
            .filter { it.status == PendingPost.STATUS_SUCCESS && it.serverId != null }
            .mapNotNull { it.serverId }
            .toSet()

        val filteredServerList = serverList.filter { it.id !in successServerIds }.map { item ->
            // 同样对服务器数据应用本地点赞状态（覆盖服务器返回的状态）
            if (likeUpdates.containsKey(item.id)) {
                val (isLiked, likeCount) = likeUpdates[item.id]!!
                val updatedItem = item.copy(isLiked = isLiked, likeCount = likeCount)
                // 重新计算点赞数布局
                updatedItem.titleLayout = item.titleLayout
                updatedItem.titleHeight = item.titleHeight
                updatedItem.authorLayout = item.authorLayout
                updatedItem.likeLayout = TextMeasureUtils.preCalculateLike(likeCount.toString())
                updatedItem.totalHeight = item.totalHeight
                updatedItem
            } else {
                item
            }
        }

        return localPosts + filteredServerList
    }

    private fun preCalculateItem(item: PostInfo): PostInfo {
        // 1. 标题
        val (layout, textHeight) = TextMeasureUtils.preCalculateTitle(
            item.title,
            FeedUIConfig.itemWidth,
            FeedUIConfig.titleTextSize
        )
        item.titleLayout = layout
        item.titleHeight = textHeight

        // 2. 封面高度
        // 【注意】本地发布的帖子可能还没获取到真实宽高，给个默认值防止计算出错
        val validWidth = if (item.width > 0) item.width else 1080
        val validHeight = if (item.height > 0) item.height else 1440
        val (_, coverHeight) = ImageSizeUtils.calculateCoverSize(validWidth, validHeight)

        // 3. 作者名预计算
        val contentWidth = FeedUIConfig.itemWidth - (FeedUIConfig.padding * 2)
        val maxAuthorWidth = contentWidth - FeedUIConfig.avatarSize - FeedUIConfig.padding * 2 - (50 * FeedUIConfig.density).toInt()
        item.authorLayout = TextMeasureUtils.preCalculateAuthor(item.author, maxAuthorWidth)

        // 4. 点赞数预计算
        item.likeLayout = TextMeasureUtils.preCalculateLike(item.likeCount.toString())

        // 5. 总高度
        item.totalHeight = coverHeight + textHeight + FeedUIConfig.staticContentHeight

        return item
    }

    fun loadFeed(isRefresh: Boolean = false, size: Int) {
        if (!isRefresh && _isLoadingMore.value) return

        viewModelScope.launch {
            if (isRefresh) _isLoadingFlow.value = true else _isLoadingMore.value = true

            try {
                val response = RetrofitClient.postApiService.getFeed(cursor = lastId, size = size)

                if (response.code == 200 && response.data != null) {
                    val result = response.data
                    val newItems = result.list

                    // 1. 预计算网络数据
                    val processedItems = withContext(Dispatchers.Default) {
                        newItems.map { item ->
                            preCalculateItem(item)
                        }
                    }

                    // 2. 【核心修改】如果是刷新，且有正在上传的本地帖子，强行插到第一位 // 移除
                    // if (isRefresh && pendingUploadingPost != null) { // 移除
                    //     val localItem = preCalculateItem(pendingUploadingPost!!) // 移除
                    //     processedItems.add(0, localItem) // 移除
                    // } // 移除

                    // 2. 更新 _serverPosts
                    if (isFirstLoad) {
                        // 1. 先给 4 条，让屏幕先显示内容，极大减轻 LayoutManager 的首屏压力
                        _serverPosts.value = processedItems.take(4)

                        // 2. 标记首次加载已完成
                        isFirstLoad = false

                        // 3. 极短延迟后，悄悄补全剩余数据
                        delay(100)
                        _serverPosts.value = processedItems
                    } else {
                        if (isRefresh) {
                            // 刷新：直接替换整个列表
                            _serverPosts.value = processedItems
                        } else {
                            // 加载更多：追加
                            _serverPosts.value += processedItems
                        }
                    }

                    lastId = result.nextCursor
                } else {
                    showToast("内容加载失败，请稍后重试")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("网络连接异常，请检查网络")
            } finally {
                if (isRefresh) _isLoadingFlow.value = false else _isLoadingMore.value = false
            }
        }
    }

    /**
     * 切换点赞状态（异步）
     */
    fun toggleLike(postId: Long, newLikeState: Boolean) {
        viewModelScope.launch {
            // 1. 先乐观更新UI（立即反馈，主线程）
            // 更新本地点赞状态缓存
            val currentMap = _localLikeUpdates.value.toMutableMap()
            
            // 获取当前点赞数（需要从当前列表中找）
            val currentItem = feedList.value.find { it.id == postId }
            val currentLikeCount = currentItem?.likeCount ?: 0
            val newLikeCount = if (newLikeState) currentLikeCount + 1 else currentLikeCount - 1
            
            currentMap[postId] = Pair(newLikeState, newLikeCount)
            _localLikeUpdates.value = currentMap

            // 2. 异步调用后端API
            launch(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.postApiService.like(postId, newLikeState)
                    if (response.code != 200) {
                        // API调用失败，回滚状态（切回主线程）
                        withContext(Dispatchers.Main) {
                            showToast("点赞失败，请稍后重试")
                            // 回滚UI
                            val rollbackMap = _localLikeUpdates.value.toMutableMap()
                            // 恢复到反向状态
                            val oldLikeCount = if (newLikeState) newLikeCount - 1 else newLikeCount + 1
                            rollbackMap[postId] = Pair(!newLikeState, oldLikeCount)
                            _localLikeUpdates.value = rollbackMap
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        showToast("网络连接异常，请检查网络")
                        // 回滚UI
                        val rollbackMap = _localLikeUpdates.value.toMutableMap()
                        val oldLikeCount = if (newLikeState) newLikeCount - 1 else newLikeCount + 1
                        rollbackMap[postId] = Pair(!newLikeState, oldLikeCount)
                        _localLikeUpdates.value = rollbackMap
                    }
                }
            }
        }
    }

    /**
     * 详情页返回后的点赞同步
     */
    fun syncLikeFromDetail(postId: Long, isLiked: Boolean, likeCount: Int) {
        val currentMap = _localLikeUpdates.value.toMutableMap()
        val existing = currentMap[postId]
        if (existing?.first == isLiked && existing.second == likeCount) {
            return
        }
        currentMap[postId] = Pair(isLiked, likeCount)
        _localLikeUpdates.value = currentMap
    }
}