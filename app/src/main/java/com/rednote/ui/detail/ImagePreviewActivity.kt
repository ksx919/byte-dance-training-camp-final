package com.rednote.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rednote.databinding.ActivityImagePreviewBinding
import com.rednote.databinding.ItemPreviewPhotoBinding

class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrls = intent.getStringArrayListExtra("IMAGE_URLS") ?: arrayListOf()
        val initialPosition = intent.getIntExtra("INITIAL_POSITION", 0)

        if (imageUrls.isEmpty()) {
            val singleUrl = intent.getStringExtra("IMAGE_URL")
            if (singleUrl != null) {
                imageUrls.add(singleUrl)
            }
        }

        val adapter = PreviewAdapter(imageUrls)
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(initialPosition, false)

        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    class PreviewAdapter(private val images: List<String>) : RecyclerView.Adapter<PreviewAdapter.PreviewViewHolder>() {

        class PreviewViewHolder(val binding: ItemPreviewPhotoBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
            val binding = ItemPreviewPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PreviewViewHolder(binding)
        }

        override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
            val url = images[position]
            Glide.with(holder.itemView)
                .load(url)
                .into(holder.binding.photoView)
        }

        override fun getItemCount(): Int = images.size
    }
}
