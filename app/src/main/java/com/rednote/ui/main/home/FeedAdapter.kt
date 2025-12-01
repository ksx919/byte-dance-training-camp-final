package com.rednote.ui.main.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.rednote.R
import com.rednote.data.model.post.PostInfo
import com.rednote.utils.FeedUIConfig
import com.rednote.utils.FeedViewPool

class FeedAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 使用 AsyncListDiffer 自动处理后台 Diff 计算
    private val differ = AsyncListDiffer(this, object : DiffUtil.ItemCallback<PostInfo>() {
        override fun areItemsTheSame(oldItem: PostInfo, newItem: PostInfo): Boolean {
            // 【核心修复】优先比较 localId (用于处理本地帖子转网络帖子时的 ID 变化)
            if (oldItem.localId != null && newItem.localId != null && oldItem.localId == newItem.localId) {
                return true
            }
            // 必须保证 ID 唯一且不变
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PostInfo, newItem: PostInfo): Boolean {
            // Data Class 自动比较内容
            // 注意：PostInfo 中的 layout/height 字段是 @Transient 的，
            // 它们的变化不应该触发 Diff（因为它们是由内容决定的），这符合预期
            return oldItem == newItem
        }
    })

    // 公开当前列表数据供外部访问
    val currentList: List<PostInfo>
        get() = differ.currentList

    private var isFooterLoading = false
    var onItemClick: ((Long, View) -> Unit)? = null
    var onLikeClick: ((Long, Boolean) -> Unit)? = null  // 点赞回调 (postId, newLikeState)

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_FOOTER = 1
    }

    init {
        // 这对 StaggeredGridLayoutManager 至关重要，防止 item 刷新时位置乱跳
        setHasStableIds(true)
    }

    fun submitList(newList: List<PostInfo>, isLoading: Boolean) {
        isFooterLoading = isLoading
        differ.submitList(newList) {
            // 仅刷新 Footer 状态，避免刷新整个列表
            // 必须在 Diff 计算完成后的回调中执行，否则 itemCount 还是旧的
            if (itemCount > 0) {
                notifyItemChanged(itemCount - 1)
            }
        }
    }

    override fun getItemId(position: Int): Long {
        if (position >= differ.currentList.size) {
            return Long.MAX_VALUE // Footer 的固定 ID
        }
        return differ.currentList[position].id
    }

    override fun getItemCount(): Int {
        val count = differ.currentList.size
        // 如果列表为空，不显示 Footer，避免页面只有一个 Loading
        return if (count == 0) 0 else count + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == differ.currentList.size) TYPE_FOOTER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // 兜底初始化 Config，防止空指针
        if (!FeedUIConfig.isInitialized) FeedUIConfig.init(parent.context)

        // --- 1. 创建 Footer ---
        if (viewType == TYPE_FOOTER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_feed_footer, parent, false)
            // Footer 必须占满全宽 (FullSpan)
            val params = StaggeredGridLayoutManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                isFullSpan = true
            }
            view.layoutParams = params
            return FooterViewHolder(view)
        }

        // --- 2. 创建 Item (核心优化) ---

        // A. 优先从自定义缓存池获取 View，减少 new 对象开销
        val view = FeedViewPool.get() ?: FeedItemView(parent.context)

        // B. 初始化 LayoutParams 并设置间距
        val params = StaggeredGridLayoutManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // C. 设置 Item 四周的 Margin (假设 FeedUIConfig.padding 是列间距)
        val margin = FeedUIConfig.padding / 2
        params.setMargins(margin, margin, margin, margin)

        view.layoutParams = params

        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FeedViewHolder) {
            val item = differ.currentList[position]

            // 【核心优化 - 消除跳动】
            // 在 FeedItemView 绘制之前，直接把 LayoutParams 的高度修正为 ViewModel 算好的 totalHeight。
            // 这样 RecyclerView 在布局阶段就能知道确切高度，无需等待 View 测量完毕。
            // 这消除了 StaggeredGridLayoutManager 因为高度不确定而导致的 item 重排。
            val params = holder.itemView.layoutParams
            if (params.height != item.totalHeight) {
                params.height = item.totalHeight
                holder.itemView.layoutParams = params
            }

            holder.bind(item)

        } else if (holder is FooterViewHolder) {
            holder.bind(isFooterLoading)
        }
    }

    // --- ViewHolders ---

    inner class FeedViewHolder(val feedView: FeedItemView) : RecyclerView.ViewHolder(feedView) {
        fun bind(item: PostInfo) {
            feedView.bind(item)
            feedView.setOnClickListener {
                // 无论什么状态，都允许点击进入详情页
                // 详情页会根据 IS_LOCAL_POST 参数自行处理（只读模式）
                onItemClick?.invoke(item.id, feedView.ivCover)
            }
            feedView.onLikeClick = { postId, newLikeState ->
                // 本地帖子不允许点赞
                if (item.status == PostInfo.STATUS_NORMAL) {
                    onLikeClick?.invoke(postId, newLikeState)
                }
            }
        }
    }

    class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(isVisible: Boolean) {
            val params = itemView.layoutParams
            if (isVisible) {
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                itemView.visibility = View.VISIBLE
            } else {
                // 隐藏时高度设为 0，防止占位
                params.height = 0
                params.width = 0
                itemView.visibility = View.GONE
            }
            itemView.layoutParams = params
        }
    }
}