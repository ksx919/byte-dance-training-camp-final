package com.rednote.ui.main.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rednote.R
import com.rednote.data.model.FeedItem

class FeedAdapter(
    private val items: List<FeedItem>
) : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feed_card, parent, false)
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.iv_cover)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tv_author)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tv_like_count)

        fun bind(item: FeedItem, position: Int) {
            ivCover.setImageResource(item.imageRes)
            val heights = COVER_HEIGHTS
            val params = ivCover.layoutParams
            params.height = heights[position % heights.size]
            ivCover.layoutParams = params
            tvTitle.text = item.title
            tvAuthor.text = item.author
            tvLikeCount.text = item.likeCount.toString()
        }

        companion object {
            private val COVER_HEIGHTS = listOf(320, 260, 380, 300, 340, 360)
        }
    }
}