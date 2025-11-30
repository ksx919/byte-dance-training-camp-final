package com.rednote.ui.main.home

import androidx.lifecycle.viewModelScope
import com.rednote.data.api.RetrofitClient
import com.rednote.data.model.post.PostInfo
import com.rednote.ui.base.BaseViewModel
import com.rednote.utils.FeedUIConfig
import com.rednote.utils.TextMeasureUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContentViewModel : BaseViewModel() {

    private val _feedList = MutableStateFlow<List<PostInfo>>(emptyList())
    val feedList = _feedList.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _isLoadingFlow = MutableStateFlow(false)
    val isLoadingFlow = _isLoadingFlow.asStateFlow()

    private var lastId: Long? = null
    // private var hasMore: Boolean = true // 未使用，暂略

    private var isFirstLoad = true

    fun loadFeed(isRefresh: Boolean = false, size: Int) {
        if (!isRefresh && _isLoadingMore.value) return

        viewModelScope.launch {
            if (isRefresh) _isLoadingFlow.value = true else _isLoadingMore.value = true

            try {
                val response = RetrofitClient.postApiService.getFeed(lastId = lastId, size = size)

                if (response.code == 200 && response.data != null) {
                    val result = response.data
                    val newItems = result.list

                    // 后台线程全量预计算
                    val processedItems = withContext(Dispatchers.Default){
                        newItems.map { item ->
                            // 1. 标题
                            val (layout, textHeight) = TextMeasureUtils.preCalculateTitle(
                                item.title,
                                FeedUIConfig.itemWidth,
                                FeedUIConfig.titleTextSize
                            )
                            item.titleLayout = layout
                            item.titleHeight = textHeight

                            // 2. 封面高度
                            val aspectRatio = if (item.width > 0 && item.height > 0) {
                                item.height.toFloat() / item.width
                            } else {
                                1.33f
                            }
                            val coverHeight = (FeedUIConfig.itemWidth * aspectRatio).toInt()

                            // 3. 作者名预计算
                            val contentWidth = FeedUIConfig.itemWidth - (FeedUIConfig.padding * 2)
                            // 预留头像(例如20dp) + 间距 + 点赞区预估(例如50dp)
                            val maxAuthorWidth = contentWidth - FeedUIConfig.avatarSize - FeedUIConfig.padding * 2 - (50 * FeedUIConfig.density).toInt()
                            item.authorLayout = TextMeasureUtils.preCalculateAuthor(item.author, maxAuthorWidth)

                            // 4. 点赞数预计算
                            item.likeLayout = TextMeasureUtils.preCalculateLike(item.likeCount.toString())

                            // 5. 总高度
                            item.totalHeight = coverHeight + textHeight + FeedUIConfig.staticContentHeight
                            item
                        }
                    }
                    if (isFirstLoad) {
                        // 1. 先给 4 条，让屏幕先显示内容，极大减轻 LayoutManager 的首屏压力
                        _feedList.value = processedItems.take(4)

                        // 2. 标记首次加载已完成
                        isFirstLoad = false

                        // 3. 极短延迟后，悄悄补全剩余数据
                        // 此时界面已经渲染完成，用户肉眼几乎无法察觉追加过程
                        delay(100)
                        _feedList.value = processedItems
                    } else {
                        // 下拉刷新或加载更多：直接全量提交，防止闪烁
                        if (isRefresh) {
                            _feedList.value = processedItems
                        } else {
                            _feedList.value += processedItems
                        }
                    }

                    lastId = result.nextCursor
                } else {
                    showToast(response.msg ?: "加载失败")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("加载失败，请检查网络")
            } finally {
                if (isRefresh) _isLoadingFlow.value = false else _isLoadingMore.value = false
            }
        }
    }
}