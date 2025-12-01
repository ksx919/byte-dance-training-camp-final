package com.rednote.ui.main.home

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.rednote.data.model.post.PostInfo
import com.rednote.databinding.FragmentContentBinding
import com.rednote.ui.base.BaseFragment
import com.rednote.ui.detail.PostDetailActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ContentFragment : BaseFragment<FragmentContentBinding, ContentViewModel>() {

    override val viewModel: ContentViewModel by viewModels()
    private lateinit var feedAdapter: FeedAdapter
    private val detailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val postId = data.getLongExtra("POST_ID", -1L)
        if (postId == -1L) return@registerForActivityResult
        val likeCount = data.getIntExtra("POST_LIKE_COUNT", -1)
        if (likeCount < 0) return@registerForActivityResult
        val isLiked = data.getBooleanExtra("POST_IS_LIKED", false)
        viewModel.syncLikeFromDetail(postId, isLiked, likeCount)
    }

    // 复用 IntArray 对象，避免在 onScrolled 中频繁 GC（针对 SpanCount=2 的情况）
    private val lastPositions = IntArray(2)

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentContentBinding {
        return FragmentContentBinding.inflate(inflater, container, false)
    }

    private var scrollDistSum = 0

    override fun initViews() {

        // 1. LayoutManager 配置
        val layoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL).apply {
            // 【关键】防止 item 移动时自动填充 gap 导致的跳动
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
            isItemPrefetchEnabled = false
        }

        binding.rvFeed.apply {
            this.layoutManager = layoutManager
            setHasFixedSize(true)

            // 加大 View 缓存，默认是 2，双列瀑布流建议设大一点，减少 onCreateViewHolder 频率
            setItemViewCacheSize(12)

            // 扩大共用缓存池。瀑布流布局复杂，默认的 5 个不够用，扩容到 20 可以大幅减少卡顿
            recycledViewPool.setMaxRecycledViews(0, 20)

            itemAnimator = null
        }

        feedAdapter = FeedAdapter()
        feedAdapter.onItemClick = { postId, _ ->
            // 找到对应的数据项
            val item = feedAdapter.currentList.find { it.id == postId }
            if (item != null) {
                val intent = Intent(requireContext(), PostDetailActivity::class.java)
                intent.putExtra("POST_ID", postId)
                
                // 传递预加载数据
                intent.putExtra("POST_TITLE", item.title)
                intent.putExtra("POST_IMAGE", item.localImageUri ?: item.imageUrl)
                if (item.content != null) intent.putExtra("POST_CONTENT", item.content)
                intent.putExtra("POST_WIDTH", item.width)
                intent.putExtra("POST_HEIGHT", item.height)
                intent.putExtra("POST_AUTHOR", item.author)
                intent.putExtra("POST_AVATAR", item.avatarUrl)
                intent.putExtra("POST_LIKES", item.likeCount)
                intent.putExtra("POST_IS_LIKED", item.isLiked)

                val isLocal = item.id < 0 || item.status != PostInfo.STATUS_NORMAL
                intent.putExtra("IS_LOCAL_POST", isLocal)
                
                // 移除共享元素动画，直接启动
                detailLauncher.launch(intent)
            }
        }
        
        // 点赞事件处理
        feedAdapter.onLikeClick = { postId, newLikeState ->
            viewModel.toggleLike(postId, newLikeState)
        }
        
        binding.rvFeed.adapter = feedAdapter

        // 3. 滚动监听
        binding.rvFeed.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // 停止滑动时，重置计数并强制检查一次（防止滑太快错过了）
                    scrollDistSum = 0
                    checkLoadMore(recyclerView)
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // 1. 基础过滤：向上滑或正在加载直接返回
                if (dy <= 0 || viewModel.isLoadingMore.value) return

                // 2. 距离节流
                // 累加滑动的距离
                scrollDistSum += dy

                // 只有当累积滑动超过一定像素（例如屏幕高度的 1/5 或固定值 100px）时，才去调用昂贵的 findLastVisibleItemPositions
                // 这样可以将高频的 Move 事件计算量减少 80% 以上
                if (scrollDistSum < 100) {
                    return
                }

                // 3. 达到阈值，执行检查，并归零计数器
                scrollDistSum = 0
                checkLoadMore(recyclerView)
            }

            // 抽离出的检查逻辑
            private fun checkLoadMore(recyclerView: RecyclerView) {
                val lm = recyclerView.layoutManager as StaggeredGridLayoutManager
                val totalCount = lm.itemCount
                if (totalCount < 4) return

                // 这行代码是耗时大户，现在被节流了
                lm.findLastVisibleItemPositions(lastPositions)
                val lastPos = lastPositions.maxOrNull() ?: 0

                if (lastPos >= totalCount - 8) {
                    // 【修复】使用 post 推迟执行，避免在滚动回调中修改 Adapter 数据
                    recyclerView.post {
                        viewModel.loadFeed(isRefresh = false, size = 10)
                    }
                }
            }
        })

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadFeed(isRefresh = true, size = 10)
        }
        binding.swipeRefresh.setColorSchemeResources(android.R.color.holo_red_light)
    }

    override fun initObservers() {
        lifecycleScope.launch {
            combine(
                viewModel.feedList,
                viewModel.isLoadingFlow,
                viewModel.isLoadingMore
            ) { list, isLoading, isLoadingMore ->
                Triple(list, isLoading, isLoadingMore)
            }.collectLatest { (list, isLoading, isLoadingMore) ->
                feedAdapter.submitList(list, isLoading || isLoadingMore)

                if (!isLoading) {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    override fun initData() {
        viewModel.loadFeed(isRefresh = true, size = 10)
    }
}