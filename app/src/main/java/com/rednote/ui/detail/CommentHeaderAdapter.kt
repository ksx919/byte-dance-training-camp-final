package com.rednote.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rednote.R
import com.rednote.databinding.ItemCommentHeaderBinding
import com.rednote.utils.UserManager

class CommentHeaderAdapter(
    private val onInputClick: () -> Unit
) : RecyclerView.Adapter<CommentHeaderAdapter.HeaderViewHolder>() {

    private var commentCount: Int = 0

    fun setCommentCount(count: Int) {
        this.commentCount = count
        notifyItemChanged(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val binding = ItemCommentHeaderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HeaderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind(commentCount, onInputClick)
    }

    override fun getItemCount(): Int = 1

    class HeaderViewHolder(private val binding: ItemCommentHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(count: Int, onInputClick: () -> Unit) {
            binding.tvCommentCountHeader.text = "共 $count 条评论"

            // Load current user avatar
            val user = UserManager.getUser()
            if (user != null) {
                Glide.with(binding.root)
                    .load(user.avatarUrl)
                    .placeholder(R.color.darker_gray)
                    .circleCrop()
                    .into(binding.ivCurrentUserAvatar)
            } else {
                binding.ivCurrentUserAvatar.setImageResource(R.color.darker_gray)
            }
            
            // Click listeners for input box
            binding.clInputBox.setOnClickListener {
                onInputClick.invoke()
            }
        }
    }
}
