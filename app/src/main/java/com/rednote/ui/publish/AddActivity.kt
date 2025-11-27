package com.rednote.ui.publish

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rednote.databinding.ActivityAddBinding
import com.rednote.ui.base.BaseActivity
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddActivity : BaseActivity<ActivityAddBinding>() {
    
    private val viewModel: PublishViewModel by viewModels()
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var layoutHelper: ImageLayoutHelper
    
    // 相机拍摄的临时文件
    private var cameraImageUri: Uri? = null
    private var cameraImageFile: File? = null
    
    override fun getViewBinding(): ActivityAddBinding {
        return ActivityAddBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 设置Edge-to-Edge显示
        ViewCompat.setOnApplyWindowInsetsListener(binding.add) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // 初始化布局辅助类（View层的工具）
        layoutHelper = ImageLayoutHelper(
            recyclerView = binding.rvSelectedImages,
            addButton = binding.btnAddImage,
            container = binding.layoutImagePicker,
            resources = resources
        )
        
        // 初始化ImageAdapter（只负责展示，不管理状态）
        imageAdapter = ImageAdapter(
            onImageDelete = { position ->
                // 用户删除图片时，通知ViewModel更新状态
                viewModel.removeImage(position)
            }
        )
        
        // 设置RecyclerView
        binding.rvSelectedImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSelectedImages.adapter = imageAdapter
        
        // 观察ViewModel的图片列表状态
        observeViewModel()
        
        // 设置加号按钮点击事件
        binding.btnAddImage.setOnClickListener {
            showImageSourceDialog()
        }
        
        // 注册ActivityResultLauncher
        registerActivityResultLaunchers()
        
        // 绑定标题和内容输入框
        binding.etTitle.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updateTitle(s?.toString() ?: "")
            }
        })
        
        binding.etContent.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updateContent(s?.toString() ?: "")
            }
        })
        
        // 初始状态：没有图片，加号在第一个位置
        layoutHelper.updateLayout(0)
    }

    override fun initData() {
        // 可以在这里加载已保存的草稿等
    }
    
    /**
     * 观察ViewModel的状态变化
     */
    private fun observeViewModel() {
        // 观察图片列表变化
        lifecycleScope.launch {
            viewModel.imageList.collect { images ->
                // 更新Adapter显示
                imageAdapter.updateImages(images)
                // 根据图片数量调整布局
                layoutHelper.updateLayout(images.size)
                
                // 如果有图片，滚动到最后一张
                if (images.isNotEmpty()) {
                    binding.rvSelectedImages.post {
                        binding.rvSelectedImages.smoothScrollToPosition(images.size - 1)
                    }
                }
            }
        }
        
        // 观察Toast事件
        lifecycleScope.launch {
            viewModel.toastEvent.collect { message ->
                android.widget.Toast.makeText(this@AddActivity, message, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 注册ActivityResultLauncher
     */
    private fun registerActivityResultLaunchers() {
        // 相册选择（多选）- Android 13+使用PickMultipleVisualMedia，低版本使用Intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用新的API（多选）
            // PickMultipleVisualMedia 返回的 Contract 输入类型是 PickVisualMedia
            imagePickerMultipleMediaLauncher = registerForActivityResult(
                ActivityResultContracts.PickMultipleVisualMedia(9)
            ) { uris ->
                handleImageSelection(uris)
            }
        } else {
            // Android 12及以下使用Intent
            imagePickerIntentLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    val clipData = result.data?.clipData
                    val uris = mutableListOf<Uri>()
                    
                    if (clipData != null) {
                        // 多选
                        for (i in 0 until clipData.itemCount) {
                            clipData.getItemAt(i)?.uri?.let { uris.add(it) }
                        }
                    } else {
                        // 单选
                        result.data?.data?.let { uris.add(it) }
                    }
                    
                    handleImageSelection(uris)
                }
            }
        }
        
        // 相机拍摄
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && cameraImageUri != null) {
                cameraImageUri?.let { uri ->
                    viewModel.addImage(uri)
                }
            }
        }
        
        // 权限请求
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                // 权限已授予，重新执行操作
                when (pendingAction) {
                    PendingAction.PICK_FROM_GALLERY -> openGallery()
                    PendingAction.TAKE_PHOTO -> openCamera()
                    else -> {}
                }
            } else {
                viewModel.showToastMessage("需要权限才能选择图片")
            }
            pendingAction = PendingAction.NONE
        }
    }
    
    /**
     * 显示图片选择对话框
     */
    private fun showImageSourceDialog() {
        val options = arrayOf("从相册选择", "拍照", "取消")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // 从相册选择
                        checkPermissionAndAction(PendingAction.PICK_FROM_GALLERY)
                    }
                    1 -> {
                        // 拍照
                        checkPermissionAndAction(PendingAction.TAKE_PHOTO)
                    }
                    2 -> {
                        // 取消，不做任何操作
                    }
                }
            }
            .show()
    }
    
    /**
     * 检查权限并执行操作
     */
    private fun checkPermissionAndAction(action: PendingAction) {
        try {
            // Android 13+ 使用 PickMultipleVisualMedia 时，可能不需要权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && action == PendingAction.PICK_FROM_GALLERY) {
                // 直接打开相册，不需要权限检查
                openGallery()
                return
            }
            
            val permissions = getRequiredPermissions(action)
            if (permissions.isEmpty()) {
                // 不需要权限，直接执行
                when (action) {
                    PendingAction.PICK_FROM_GALLERY -> openGallery()
                    PendingAction.TAKE_PHOTO -> openCamera()
                    else -> {}
                }
                return
            }
            
            val needRequest = permissions.any {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }
            
            if (needRequest) {
                pendingAction = action
                permissionLauncher.launch(permissions.toTypedArray())
            } else {
                when (action) {
                    PendingAction.PICK_FROM_GALLERY -> openGallery()
                    PendingAction.TAKE_PHOTO -> openCamera()
                    else -> {}
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AddActivity", "检查权限失败", e)
            e.printStackTrace()
            viewModel.showToastMessage("操作失败: ${e.message}")
        }
    }
    
    /**
     * 获取所需的权限列表（根据操作类型）
     */
    private fun getRequiredPermissions(action: PendingAction): List<String> {
        val permissions = mutableListOf<String>()
        
        when (action) {
            PendingAction.PICK_FROM_GALLERY -> {
                // 相册选择只需要存储权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            PendingAction.TAKE_PHOTO -> {
                // 拍照需要相机权限和存储权限（用于保存照片）
                permissions.add(Manifest.permission.CAMERA)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            else -> {}
        }
        
        return permissions
    }
    
    /**
     * 处理图片选择结果
     */
    private fun handleImageSelection(uris: List<Uri>) {
        if (uris.isEmpty()) return
        
        // 检查当前已有图片数量，最多9张
        val currentCount = viewModel.imageList.value.size
        val remainingSlots = 9 - currentCount
        
        if (remainingSlots > 0) {
            val urisToAdd = uris.take(remainingSlots)
            viewModel.addImages(urisToAdd)
            if (uris.size > remainingSlots) {
                viewModel.showToastMessage("最多只能选择9张图片，已添加${remainingSlots}张")
            }
        } else {
            viewModel.showToastMessage("最多只能选择9张图片")
        }
    }
    
    /**
     * 打开相册选择
     */
    private fun openGallery() {
        // 检查当前已有图片数量
        val currentCount = viewModel.imageList.value.size
        val maxSelect = minOf(9 - currentCount, 9)
        
        if (maxSelect <= 0) {
            viewModel.showToastMessage("最多只能选择9张图片")
            return
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 使用新的API（多选）
                // 注意：Android 13+ 的 PickMultipleVisualMedia 可能不需要权限
                if (imagePickerMultipleMediaLauncher == null) {
                    android.util.Log.e("AddActivity", "imagePickerMultipleMediaLauncher 未初始化")
                    viewModel.showToastMessage("相册选择器未初始化")
                    return
                }
                imagePickerMultipleMediaLauncher?.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } else {
                // Android 12及以下使用Intent
                if (imagePickerIntentLauncher == null) {
                    android.util.Log.e("AddActivity", "imagePickerIntentLauncher 未初始化")
                    viewModel.showToastMessage("相册选择器未初始化")
                    return
                }
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                imagePickerIntentLauncher?.launch(Intent.createChooser(intent, "选择图片"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AddActivity", "打开相册失败", e)
            e.printStackTrace()
            viewModel.showToastMessage("打开相册失败: ${e.message}")
        }
    }
    
    /**
     * 打开相机
     */
    private fun openCamera() {
        // 检查当前已有图片数量
        if (viewModel.imageList.value.size >= 9) {
            viewModel.showToastMessage("最多只能选择9张图片")
            return
        }
        
        try {
            // 创建临时文件
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imageFile = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            )
            
            cameraImageFile = imageFile
            cameraImageUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                imageFile
            )
            
            cameraImageUri?.let { uri ->
                cameraLauncher.launch(uri)
            }
        } catch (e: Exception) {
            viewModel.showToastMessage("无法打开相机: ${e.message}")
        }
    }
    
    /**
     * 获取当前选中的图片列表（从ViewModel获取）
     */
    fun getSelectedImages(): List<Uri> = viewModel.imageList.value
    
    // ActivityResultLauncher
    private var imagePickerMultipleMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var imagePickerIntentLauncher: ActivityResultLauncher<Intent>? = null
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    
    // 待执行的操作
    private var pendingAction: PendingAction = PendingAction.NONE
    
    private enum class PendingAction {
        NONE,
        PICK_FROM_GALLERY,
        TAKE_PHOTO
    }
}