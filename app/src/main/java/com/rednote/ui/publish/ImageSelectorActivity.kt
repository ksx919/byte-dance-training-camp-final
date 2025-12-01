package com.rednote.ui.publish

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rednote.R
import com.rednote.databinding.ActivityImageSelectorBinding
import com.rednote.databinding.ItemImageSelectorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageSelectorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageSelectorBinding
    private lateinit var adapter: ImageSelectorAdapter
    private var maxCount = 9
    private var currentCount = 0

    // 已存在的图片列表（从上次选择中传入）
    private val existingImages = mutableListOf<Uri>()
    
    // 本次新选中的图片列表 (存放 content URI)
    private val selectedImages = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取参数
        currentCount = intent.getIntExtra(EXTRA_CURRENT_COUNT, 0)
        maxCount = intent.getIntExtra(EXTRA_MAX_COUNT, 9)
        
        // 获取已存在的图片列表
        val existingUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(EXTRA_SELECTED_URIS, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(EXTRA_SELECTED_URIS)
        }
        existingUris?.let { existingImages.addAll(it) }

        setupView()
        checkPermissionAndLoadImages()
    }

    private fun setupView() {
        adapter = ImageSelectorAdapter(
            onImageClick = { uri -> toggleSelection(uri) }
        )
        binding.rvImages.layoutManager = GridLayoutManager(this, 4)
        binding.rvImages.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnDone.setOnClickListener {
            // 合并已存在的图片和新选中的图片
            val allSelectedImages = mutableListOf<Uri>()
            allSelectedImages.addAll(existingImages)
            allSelectedImages.addAll(selectedImages)
            
            // 无论是否为空，都返回结果
            // 如果为空，表示用户取消了所有选择
            val resultIntent = Intent().apply {
                putParcelableArrayListExtra(EXTRA_RESULT_URIS, ArrayList(allSelectedImages))
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
        
        updateDoneButton()
    }

    private fun toggleSelection(uri: Uri) {
        // 检查是否为已存在的图片
        if (existingImages.contains(uri)) {
            // 已存在的图片，点击则取消选择
            existingImages.remove(uri)
            // 需要更新所有选中图片的序号
            updateAllSelectedItemsSequence()
        } else if (selectedImages.contains(uri)) {
            // 本次新选中的图片，点击则取消选择
            selectedImages.remove(uri)
            // 需要更新所有选中图片的序号
            updateAllSelectedItemsSequence()
        } else {
            // 未选中的图片，点击则选中
            val totalCount = existingImages.size + selectedImages.size
            if (totalCount >= maxCount) {
                Toast.makeText(this, "最多只能选择${maxCount}张图片", Toast.LENGTH_SHORT).show()
                return
            }
            selectedImages.add(uri)
            // 只需要更新这一项
            val position = adapter.images.indexOf(uri)
            if (position != -1) {
                adapter.notifyItemChanged(position, PAYLOAD_UPDATE_SEQUENCE)
            }
        }
        updateDoneButton()
    }
    
    /**
     * 更新所有选中图片的序号（使用 payload 避免重新加载图片）
     */
    private fun updateAllSelectedItemsSequence() {
        // 收集所有选中图片
        val allSelectedUris = mutableSetOf<Uri>()
        allSelectedUris.addAll(existingImages)
        allSelectedUris.addAll(selectedImages)
        
        // 遍历所有图片，只更新选中的项的序号
        adapter.images.forEachIndexed { index, uri ->
            if (allSelectedUris.contains(uri)) {
                // 使用 payload 只更新序号，不重新加载图片
                adapter.notifyItemChanged(index, PAYLOAD_UPDATE_SEQUENCE)
            }
        }
        
        // 同时也需要更新那些刚被取消选择的项
        adapter.images.forEachIndexed { index, uri ->
            if (!allSelectedUris.contains(uri) && 
                (uri in adapter.getPreviouslySelectedUris())) {
                adapter.notifyItemChanged(index, PAYLOAD_UPDATE_SEQUENCE)
            }
        }
    }

    private fun updateDoneButton() {
        val count = existingImages.size + selectedImages.size
        if (count > 0) {
            binding.btnDone.text = "下一步($count)"
            binding.btnDone.isEnabled = true
            binding.btnDone.alpha = 1.0f
        } else {
            binding.btnDone.text = "下一步"
            binding.btnDone.isEnabled = true
        }
    }

    private fun checkPermissionAndLoadImages() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadImages()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            loadImages()
        } else {
            Toast.makeText(this, "需要权限才能访问相册", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadImages() {
        lifecycleScope.launch {
            val images = withContext(Dispatchers.IO) {
                val imageList = mutableListOf<Uri>()
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATE_ADDED
                )
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

                contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        imageList.add(contentUri)
                    }
                }
                imageList
            }
            adapter.submitList(images)
        }
    }

    companion object {
        const val EXTRA_CURRENT_COUNT = "extra_current_count"
        const val EXTRA_MAX_COUNT = "extra_max_count"
        const val EXTRA_SELECTED_URIS = "extra_selected_uris"
        const val EXTRA_RESULT_URIS = "extra_result_uris"
        const val PAYLOAD_UPDATE_SEQUENCE = "update_sequence"
    }

    inner class ImageSelectorAdapter(
        private val onImageClick: (Uri) -> Unit
    ) : RecyclerView.Adapter<ImageSelectorAdapter.ViewHolder>() {

        var images: List<Uri> = emptyList()
            private set
        
        // 记录上一次选中的图片，用于检测哪些需要更新
        private var previouslySelectedUris = mutableSetOf<Uri>()

        fun submitList(newImages: List<Uri>) {
            val oldImages = images
            val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
                override fun getOldListSize(): Int = oldImages.size
                override fun getNewListSize(): Int = newImages.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldImages[oldItemPosition] == newImages[newItemPosition]
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldImages[oldItemPosition] == newImages[newItemPosition]
                }
            })
            images = newImages
            diffResult.dispatchUpdatesTo(this)
        }
        
        fun getPreviouslySelectedUris(): Set<Uri> = previouslySelectedUris

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemImageSelectorBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(images[position])
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.isEmpty()) {
                // 没有 payload，执行完整绑定
                super.onBindViewHolder(holder, position, payloads)
            } else {
                // 有 payload，只更新序号部分
                if (payloads.contains(PAYLOAD_UPDATE_SEQUENCE)) {
                    holder.updateSequenceOnly(images[position])
                }
            }
        }

        override fun getItemCount(): Int = images.size

        inner class ViewHolder(private val binding: ItemImageSelectorBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(uri: Uri) {
                // 异步加载缩略图
                binding.ivImage.tag = uri // 防止复用错乱
                binding.ivImage.setImageDrawable(null) // 清空旧图
                binding.ivImage.setBackgroundColor(0xFFEEEEEE.toInt()) // 占位色

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentResolver.loadThumbnail(uri, Size(200, 200), null)
                        } else {
                            // API 28 降级处理：简单采样
                            val options = BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                            }
                            contentResolver.openInputStream(uri)?.use { 
                                BitmapFactory.decodeStream(it, null, options) 
                            }
                            options.inSampleSize = calculateInSampleSize(options, 200, 200)
                            options.inJustDecodeBounds = false
                            contentResolver.openInputStream(uri)?.use { 
                                BitmapFactory.decodeStream(it, null, options) 
                            }
                        }
                        
                        withContext(Dispatchers.Main) {
                            if (binding.ivImage.tag == uri) {
                                binding.ivImage.setImageBitmap(bitmap)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // 更新选择状态和序号
                updateSequenceOnly(uri)

                binding.root.setOnClickListener {
                    onImageClick(uri)
                }
            }
            
            /**
             * 只更新序号和选择状态，不重新加载图片
             */
            fun updateSequenceOnly(uri: Uri) {
                // 检查是否为已存在的图片
                val existingIndex = existingImages.indexOf(uri)
                if (existingIndex != -1) {
                    // 已存在的图片，显示其序号
                    binding.vMask.visibility = View.VISIBLE
                    binding.tvIndicator.setBackgroundResource(R.drawable.shape_circle_sequence)
                    binding.tvIndicator.text = (existingIndex + 1).toString()
                    // 记录到已选列表
                    previouslySelectedUris.add(uri)
                } else {
                    // 检查是否为本次新选中的图片
                    val newIndex = selectedImages.indexOf(uri)
                    if (newIndex != -1) {
                        // 本次新选中
                        binding.vMask.visibility = View.VISIBLE
                        binding.tvIndicator.setBackgroundResource(R.drawable.shape_circle_sequence)
                        // 序号 = 已存在的数量 + 本次选中的索引 + 1
                        binding.tvIndicator.text = (existingImages.size + newIndex + 1).toString()
                        // 记录到已选列表
                        previouslySelectedUris.add(uri)
                    } else {
                        // 未选中
                        binding.vMask.visibility = View.GONE
                        binding.tvIndicator.setBackgroundResource(R.drawable.shape_circle_unselected)
                        binding.tvIndicator.text = ""
                        // 从已选列表中移除
                        previouslySelectedUris.remove(uri)
                    }
                }
            }
        }

        private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            val (height: Int, width: Int) = options.outHeight to options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
    }
}
