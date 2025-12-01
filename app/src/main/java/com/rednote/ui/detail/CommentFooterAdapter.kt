package com.rednote.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rednote.databinding.ItemCommentFooterBinding

class CommentFooterAdapter : RecyclerView.Adapter<CommentFooterAdapter.FooterViewHolder>() {

    companion object {
        const val STATE_HIDDEN = 0
        const val STATE_LOADING = 1
        const val STATE_NO_MORE = 2
    }

    private var currentState = STATE_HIDDEN
    private var pendingState: Int? = null
    
    private val updateRunnable = Runnable {
        pendingState?.let { state ->
            pendingState = null
            performUpdate(state)
        }
    }

    fun updateState(state: Int) {
        // 保存待更新状态
        pendingState = state
        
        // 延迟到下一帧执行，避免在滚动回调中修改adapter
        recyclerView?.post(updateRunnable)
    }
    
    private var recyclerView: RecyclerView? = null
    
    private fun performUpdate(state: Int) {
        if (currentState != state) {
            val oldState = currentState
            currentState = state
            
            if (oldState == STATE_HIDDEN) {
                notifyItemInserted(0)
            } else if (state == STATE_HIDDEN) {
                notifyItemRemoved(0)
            } else {
                notifyItemChanged(0)
            }
        }
    }
    
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }
    
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView?.removeCallbacks(updateRunnable)
        this.recyclerView = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val binding = ItemCommentFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FooterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FooterViewHolder, position: Int) {
        when (currentState) {
            STATE_LOADING -> {
                holder.binding.loadingView.visibility = View.VISIBLE
                holder.binding.tvNoMore.visibility = View.GONE
            }
            STATE_NO_MORE -> {
                holder.binding.loadingView.visibility = View.GONE
                holder.binding.tvNoMore.visibility = View.VISIBLE
            }
            else -> {
                holder.binding.loadingView.visibility = View.GONE
                holder.binding.tvNoMore.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = if (currentState != STATE_HIDDEN) 1 else 0

    class FooterViewHolder(val binding: ItemCommentFooterBinding) : RecyclerView.ViewHolder(binding.root)
}
