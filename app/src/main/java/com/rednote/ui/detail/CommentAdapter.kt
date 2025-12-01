package com.rednote.ui.detail

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.rednote.data.model.comment.CommentVO
import com.rednote.databinding.ItemCommentBinding
import com.rednote.databinding.ItemCommentReplyBinding
import com.rednote.utils.CommentUIConfig
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.core.graphics.toColorInt

class CommentAdapter(
    var postAuthorId: Long = 0L,
    private val onExpandReplies: (CommentVO) -> Unit,
    private val onReplyClick: (CommentVO, Long) -> Unit,
    private val onRetryClick: (CommentVO) -> Unit,
    private val onLikeClick: (CommentVO) -> Unit,
    private val onImageClick: (String) -> Unit
) : ListAdapter<CommentVO, CommentAdapter.CommentViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<CommentVO>() {
        override fun areItemsTheSame(oldItem: CommentVO, newItem: CommentVO): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CommentVO, newItem: CommentVO): Boolean {
            // 比较所有可能变化的字段
            return oldItem.content == newItem.content &&
                   oldItem.likeCount == newItem.likeCount &&
                   oldItem.isLiked == newItem.isLiked &&
                   oldItem.childReplies?.size == newItem.childReplies?.size &&
                   oldItem.hasMoreReplies == newItem.hasMoreReplies &&
                   oldItem.isLoadingReplies == newItem.isLoadingReplies &&
                   oldItem.status == newItem.status &&
                   // 比较子评论的关键字段
                   areRepliesEqual(oldItem.childReplies, newItem.childReplies)
        }
        
        private fun areRepliesEqual(oldReplies: List<CommentVO>?, newReplies: List<CommentVO>?): Boolean {
            if (oldReplies == null && newReplies == null) return true
            if (oldReplies == null || newReplies == null) return false
            if (oldReplies.size != newReplies.size) return false
            
            return oldReplies.indices.all { i ->
                val old = oldReplies[i]
                val new = newReplies[i]
                old.id == new.id && 
                old.isLiked == new.isLiked && 
                old.likeCount == new.likeCount &&
                old.content == new.content
            }
        }
    }
    
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        if (!CommentUIConfig.isInitialized) CommentUIConfig.init(parent.context)
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        onBindViewHolder(holder, position, emptyList())
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int, payloads: List<Any>) {
        val comment = getItem(position)
        
        // Pre-set height to avoid layout jumps
        val params = holder.itemView.layoutParams
        if (params.height != comment.totalHeight && comment.totalHeight > 0) {
            params.height = comment.totalHeight
            holder.itemView.layoutParams = params
        }

        // Set click listener for the main comment item to reply
        holder.itemView.setOnClickListener {
            if (comment.status == CommentVO.STATUS_FAILED) {
                onRetryClick(comment)
            } else if (comment.status == CommentVO.STATUS_NORMAL) {
                onReplyClick(comment, comment.id)
            }
        }
        
        if (payloads.isEmpty()) {
            fullBind(holder, comment)
        } else {
            // Partial bind
            val payload = payloads[0]
            when (payload) {
                PAYLOAD_REPLY -> {
                    bindReplies(holder, comment)
                }
                PAYLOAD_LIKE -> {
                    // Only update like UI
                    with(holder.binding) {
                        tvLikeCount.text = comment.likeCount.toString()
                        if (comment.isLiked) {
                            ivLike.setImageResource(com.rednote.R.drawable.ic_favorite_filled)
                            ivLike.setColorFilter("#FF2442".toColorInt())
                        } else {
                            ivLike.setImageResource(com.rednote.R.drawable.ic_favorite)
                            ivLike.setColorFilter("#999999".toColorInt())
                        }
                    }
                }
                PAYLOAD_REPLY_LIKE -> {
                    // Only update replies (specifically likes)
                    // Since replies are dynamic views, we re-bind them.
                    // bindReplies is efficient enough as it reuses views.
                    bindReplies(holder, comment)
                }
            }
        }
    }

    private fun fullBind(holder: CommentViewHolder, comment: CommentVO) {
        with(holder.binding) {
            tvUsername.text = comment.nickname
            tvContent.text = comment.content
            tvLikeCount.text = comment.likeCount.toString()

            // 显示作者标签
            if (comment.userId == postAuthorId) {
                tvAuthorTag.visibility = View.VISIBLE
            } else {
                tvAuthorTag.visibility = View.GONE
            }

            if (comment.id > 0) {
                llLikeContainer.visibility = View.VISIBLE
                if (comment.isLiked) {
                    ivLike.setImageResource(com.rednote.R.drawable.ic_favorite_filled)
                    ivLike.setColorFilter("#FF2442".toColorInt())
                } else {
                    ivLike.setImageResource(com.rednote.R.drawable.ic_favorite)
                    ivLike.setColorFilter("#999999".toColorInt())
                }

                llLikeContainer.setOnClickListener {
                    onLikeClick(comment)
                }
            } else {
                llLikeContainer.visibility = View.GONE
                llLikeContainer.setOnClickListener(null)
            }

            // Handle Status
            when (comment.status) {
                CommentVO.STATUS_SENDING -> {
                    tvInfo.text = "发送中..."
                    tvInfo.setTextColor(Color.GRAY)
                    tvInfo.setOnClickListener(null)
                }
                CommentVO.STATUS_FAILED -> {
                    tvInfo.text = "发送失败，点击重试"
                    tvInfo.setTextColor(Color.RED)
                    tvInfo.setOnClickListener { onRetryClick(comment) }
                }
                else -> {
                    tvInfo.setTextColor("#999999".toColorInt())
                    // Format date and bold "回复"
                    val dateText = getRelativeTime(comment.createdAt)
                    val infoText = "$dateText 回复"
                    val spannable = SpannableString(infoText)
                    spannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        infoText.length - 2,
                        infoText.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    tvInfo.text = spannable
                    tvInfo.setOnClickListener { onReplyClick(comment, comment.id) }
                }
            }

            // 【优化】头像缩略图
            val avatarUrl = getThumbnailUrl(comment.avatarUrl ?: "", 100)
            Glide.with(holder.itemView)
                .load(avatarUrl)
                .circleCrop()
                .into(ivAvatar)

            // Handle comment image
            val imageToLoad = comment.localImageUri ?: comment.imageUrl
            if (imageToLoad != null) {
                ivCommentImage.visibility = View.VISIBLE

                val w = comment.imageWidth
                val h = comment.imageHeight

                // 【核心修复】强制计算并应用具体像素高度，不再依赖 Glide 撑开
                if (w != null && h != null && w > 0 && h > 0) {
                    val params = ivCommentImage.layoutParams
                    // 1. 设置比例 (用于宽度自适应场景)
                    if (params is ConstraintLayout.LayoutParams) {
                        params.dimensionRatio = "$w:$h"
                    }

                    // 2. 如果宽度是固定的（或者已知），直接算出高度设进去
                    // 假设 XML 中 ivCommentImage 的宽度是 0dp (match_constraint) 或固定值
                    // 我们这里采取更稳妥的方式：
                    // 如果能获取到 ImageView 的测量宽度最好，如果获取不到，
                    // 至少保证 ratio 生效。对于 wrap_content 的高度，dimensionRatio 有时需要 match_constraint (0dp)

                    // 最佳实践：结合 XML 修改，确保 layout_height="0dp" (match_constraint)
                    // 如果 XML 改不了，就在这里动态改：
                    params.height = 0 // 配合 dimensionRatio 使用，强制占位
                    ivCommentImage.layoutParams = params
                }

                // 【优化】使用 OSS 缩略图参数
                val thumbnailUrl = getThumbnailUrl(imageToLoad.toString(), w ?: 500)

                Glide.with(holder.itemView)
                    .load(thumbnailUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivCommentImage)

                ivCommentImage.setOnClickListener {
                    onImageClick(imageToLoad.toString())
                }
            } else {
                ivCommentImage.visibility = View.GONE
            }
            
            bindReplies(holder, comment)
        }
    }

    private fun bindReplies(holder: CommentViewHolder, comment: CommentVO) {
        with(holder.binding) {
            // Handle replies
            val repliesToShow = if (comment.childReplies?.isNotEmpty() == true) {
                comment.childReplies!!
            } else if (comment.topReply != null) {
                listOf(comment.topReply)
            } else {
                emptyList()
            }

            if (repliesToShow.isNotEmpty()) {
                llReplyContainer.visibility = View.VISIBLE
                
                val childCount = llRepliesList.childCount
                val neededCount = repliesToShow.size
                
                // 1. Reuse or create views
                for (i in 0 until neededCount) {
                    val reply = repliesToShow[i]
                    val replyBinding: ItemCommentReplyBinding
                    
                    if (i < childCount) {
                        val view = llRepliesList.getChildAt(i)
                        replyBinding = ItemCommentReplyBinding.bind(view)
                    } else {
                        // Inflate new view
                        replyBinding = ItemCommentReplyBinding.inflate(
                            LayoutInflater.from(holder.itemView.context),
                            llRepliesList,
                            false
                        )
                        llRepliesList.addView(replyBinding.root)
                    }
                    
                    // Bind data
                    replyBinding.tvReplyUsername.text = reply.nickname
                    
                    val contentText = if (!reply.targetUserNickname.isNullOrEmpty()) {
                        "回复 ${reply.targetUserNickname}：${reply.content}"
                    } else {
                        reply.content
                    }
                    replyBinding.tvReplyContent.text = contentText
                    
                    // Handle reply image
                    val replyImageToLoad = reply.localImageUri ?: reply.imageUrl
                    if (replyImageToLoad != null) {
                        replyBinding.ivReplyImage.visibility = View.VISIBLE
                        
                        val w = reply.imageWidth
                        val h = reply.imageHeight
                        
                        if (w != null && h != null && w > 0 && h > 0) {
                            val ratio = "$w:$h"
                            (replyBinding.ivReplyImage.layoutParams as? ConstraintLayout.LayoutParams)?.let { params ->
                                params.dimensionRatio = ratio
                                replyBinding.ivReplyImage.layoutParams = params
                            }
                        }

                        // 【优化】使用 OSS 缩略图参数
                        val replyThumbnailUrl = getThumbnailUrl(replyImageToLoad.toString(), w ?: 300)

                        Glide.with(holder.itemView)
                            .load(replyThumbnailUrl)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(replyBinding.ivReplyImage)

                        replyBinding.ivReplyImage.setOnClickListener {
                            onImageClick(replyImageToLoad.toString())
                        }
                    } else {
                        replyBinding.ivReplyImage.visibility = View.GONE
                    }

                    replyBinding.tvReplyLikeCount.text = reply.likeCount.toString()
                    
                    if (reply.id > 0) {
                        replyBinding.llReplyLikeContainer.visibility = View.VISIBLE
                        if (reply.isLiked) {
                            replyBinding.ivReplyLike.setImageResource(com.rednote.R.drawable.ic_favorite_filled)
                            replyBinding.ivReplyLike.setColorFilter("#FF2442".toColorInt())
                        } else {
                            replyBinding.ivReplyLike.setImageResource(com.rednote.R.drawable.ic_favorite)
                            replyBinding.ivReplyLike.setColorFilter("#999999".toColorInt())
                        }

                        replyBinding.llReplyLikeContainer.setOnClickListener {
                            onLikeClick(reply)
                        }
                    } else {
                        replyBinding.llReplyLikeContainer.visibility = View.GONE
                        replyBinding.llReplyLikeContainer.setOnClickListener(null)
                    }
                    
                    if (reply.userId == postAuthorId) {
                        replyBinding.tvReplyAuthorTag.visibility = View.VISIBLE
                    } else {
                        replyBinding.tvReplyAuthorTag.visibility = View.GONE
                    }

                    // 【优化】头像缩略图
                    val avatarUrl = getThumbnailUrl(reply.avatarUrl ?: "", 100)
                    Glide.with(holder.itemView)
                        .load(avatarUrl)
                        .circleCrop()
                        .into(replyBinding.ivReplyAvatar)

                    val replyDateText = getRelativeTime(reply.createdAt)
                    val replyInfoText = "$replyDateText 回复"
                    val replySpannable = SpannableString(replyInfoText)
                    replySpannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        replyInfoText.length - 2,
                        replyInfoText.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    replyBinding.tvReplyInfo.text = replySpannable

                    replyBinding.root.setOnClickListener {
                        onReplyClick(reply, comment.id)
                    }
                }

                if (childCount > neededCount) {
                    llRepliesList.removeViews(neededCount, childCount - neededCount)
                }
            } else {
                if ((comment.replyCount ?: 0) > 0) {
                    llReplyContainer.visibility = View.VISIBLE
                    llRepliesList.removeAllViews() // Clear any existing views
                } else {
                    llReplyContainer.visibility = View.GONE
                }
            }

            val totalCount = comment.replyCount ?: 0
            val currentCount = comment.childReplies?.size ?: 0

            fun updateExpandButtonState() {
                if (comment.isLoadingReplies) {
                    tvExpandReplies.visibility = View.GONE
                    pbLoadingReplies.visibility = View.VISIBLE
                    llExpandReplies.setOnClickListener(null) // Disable click while loading
                } else {
                    tvExpandReplies.visibility = View.VISIBLE
                    pbLoadingReplies.visibility = View.GONE
                    llExpandReplies.setOnClickListener {
                        onExpandReplies(comment)
                    }
                }
            }

            if (currentCount == 0) {
                val displayedCount = if (comment.topReply != null) 1 else 0
                val remainingCount = totalCount - displayedCount
                
                if (remainingCount > 0) {
                    llExpandReplies.visibility = View.VISIBLE
                    tvExpandReplies.text = "展开 $remainingCount 条回复"
                    updateExpandButtonState()
                } else {
                    llExpandReplies.visibility = View.GONE
                }
            } else {
                if (comment.hasMoreReplies) {
                    llExpandReplies.visibility = View.VISIBLE
                    tvExpandReplies.text = "展开更多回复"
                    updateExpandButtonState()
                } else {
                    llExpandReplies.visibility = View.GONE
                }
            }
        }
    }

    private fun getRelativeTime(dateString: String): String {
        try {
            val commentTime = LocalDateTime.parse(dateString)
            val now = LocalDateTime.now()
            val daysDiff = ChronoUnit.DAYS.between(commentTime.toLocalDate(), now.toLocalDate())

            return when (daysDiff) {
                0L -> {
                    val minutesDiff = ChronoUnit.MINUTES.between(commentTime, now)
                    when {
                        minutesDiff < 1 -> "刚刚"
                        minutesDiff < 60 -> "${minutesDiff}分钟前"
                        else -> "${ChronoUnit.HOURS.between(commentTime, now)}小时前"
                    }
                }
                1L -> {
                    val formatter = DateTimeFormatter.ofPattern("HH:mm")
                    "昨天 ${commentTime.format(formatter)}"
                }
                else -> "${daysDiff}天前"
            }
        } catch (_: Exception) {
            return dateString
        }
    }

    private fun getThumbnailUrl(originalUrl: String, width: Int): String {
        if (originalUrl.startsWith("content://") || originalUrl.startsWith("file://") || !originalUrl.startsWith("http")) {
            return originalUrl
        }
        
        if (originalUrl.contains("aliyuncs.com")) {
            if (originalUrl.contains("x-oss-process")) {
                return originalUrl
            }
            val separator = if (originalUrl.contains("?")) "&" else "?"
            return "$originalUrl${separator}x-oss-process=image/resize,w_$width"
        }
        return originalUrl
    }

    class CommentViewHolder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)
}


const val PAYLOAD_REPLY = "PAYLOAD_REPLY"
const val PAYLOAD_LIKE = "PAYLOAD_LIKE"
const val PAYLOAD_REPLY_LIKE = "PAYLOAD_REPLY_LIKE"