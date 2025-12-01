package com.rednote.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.rednote.databinding.ItemCommentEmptyBinding

class CommentEmptyAdapter : RecyclerView.Adapter<CommentEmptyAdapter.EmptyViewHolder>() {

    private var isVisible = false

    fun setVisible(visible: Boolean) {
        if (this.isVisible != visible) {
            this.isVisible = visible
            if (visible) {
                notifyItemInserted(0)
            } else {
                notifyItemRemoved(0)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmptyViewHolder {
        val binding = ItemCommentEmptyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EmptyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmptyViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int = if (isVisible) 1 else 0

    class EmptyViewHolder(private val binding: ItemCommentEmptyBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.btnFirstComment.setOnClickListener {
                Toast.makeText(binding.root.context, "快来发表第一条评论吧！", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
