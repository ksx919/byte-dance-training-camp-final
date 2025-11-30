package com.rednote.ui.publish

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.rednote.databinding.ActivityAddBinding
import com.rednote.ui.base.BaseActivity
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.View
import android.graphics.Color
import com.rednote.R

class AddActivity : BaseActivity<ActivityAddBinding>() {

    private val viewModel: PublishViewModel by viewModels()
    private lateinit var imageAdapter: ImageAdapter

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    // 临时记录需要请求权限后执行的操作类型
    private var pendingAction: PendingAction = PendingAction.NONE
    private enum class PendingAction { NONE, PICK_FROM_GALLERY, TAKE_PHOTO }

    override fun getViewBinding(): ActivityAddBinding {
        return ActivityAddBinding.inflate(layoutInflater)
    }

    override fun initView() {
        setupEdgeToEdge()
        setupRecyclerView()
        setupInputs()
        registerLaunchers()
        binding.btnPublishBottom.setOnClickListener {
            viewModel.publish(this)
        }
    }

    override fun initData() {
        observeViewModel()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.add) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {


        imageAdapter = ImageAdapter(
            onAddClick = { showImageSourceDialog() }
        )

        binding.rvSelectedImages.apply {
            layoutManager = LinearLayoutManager(this@AddActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = imageAdapter
        }
    }

    private fun setupInputs() {
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
    }



    private fun observeViewModel() {
        lifecycleScope.launch {
            // 使用 repeatOnLifecycle 确保在后台不处理 UI 更新
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. 观察图片列表
                launch {
                    viewModel.imageList.collect { images ->
                        imageAdapter.updateImages(images)

                        if (images.isNotEmpty()) {
                            binding.rvSelectedImages.post {
                                binding.rvSelectedImages.smoothScrollToPosition(images.size - 1)
                            }
                        }
                    }
                }

                // 2. 观察 View 事件 (Toast, 打开相机, 打开相册)
                launch {
                    viewModel.viewEvent.collect { event ->
                        handleViewEvent(event)
                    }
                }
            }
        }
    }

    private fun handleViewEvent(event: PublishViewEvent) {
        when (event) {
            is PublishViewEvent.ShowToast -> {
                android.widget.Toast.makeText(this, event.message, android.widget.Toast.LENGTH_SHORT).show()
            }
            is PublishViewEvent.OpenGallery -> {
                // 这里只管执行，数量限制已经在 ViewModel 计算过了
                openGalleryInternal()
            }
            is PublishViewEvent.PrepareCamera -> {
                // ViewModel 允许拍照，现在开始创建文件并打开相机
                prepareAndLaunchCamera()
            }
            is PublishViewEvent.Finish -> {
                finish()
            }
        }
    }

    /**
     * 显示图片选择底部弹窗
     */
    private fun showImageSourceDialog() {
        // 1. 创建 Dialog
        val dialog = BottomSheetDialog(this)

        // 2. 加载布局 (使用 R.layout 引用资源)
        val view = layoutInflater.inflate(R.layout.dialog_select_image_source, null)
        dialog.setContentView(view)

        // 3. 设置背景透明 (为了显示圆角)
        try {
            // view.parent 是 Dialog 的容器，强转为 View
            val parent = view.parent as? View
            parent?.setBackgroundColor(Color.TRANSPARENT)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. 绑定点击事件 (使用 findViewById)

        // 按钮：拍照
        view.findViewById<TextView>(R.id.tv_take_photo).setOnClickListener {
            dialog.dismiss() // 关闭弹窗
            checkPermissionAndAction(PendingAction.TAKE_PHOTO)
        }

        // 按钮：从相册选择
        view.findViewById<TextView>(R.id.tv_pick_album).setOnClickListener {
            dialog.dismiss() // 关闭弹窗
            checkPermissionAndAction(PendingAction.PICK_FROM_GALLERY)
        }

        // 按钮：取消
        view.findViewById<TextView>(R.id.tv_cancel).setOnClickListener {
            dialog.dismiss()
        }

        // 5. 显示
        dialog.show()
    }

    private fun checkPermissionAndAction(action: PendingAction) {
        val permissions = getRequiredPermissions(action)
        if (permissions.isEmpty()) {
            performAction(action)
            return
        }

        val needRequest = permissions.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (needRequest) {
            pendingAction = action
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            performAction(action)
        }
    }

    private fun performAction(action: PendingAction) {
        when (action) {
            PendingAction.PICK_FROM_GALLERY -> viewModel.onPickImageClick() // 关键：调用 VM
            PendingAction.TAKE_PHOTO -> viewModel.onTakePhotoClick() // 关键：调用 VM
            else -> {}
        }
    }

    private var imageSelectorLauncher: ActivityResultLauncher<Intent>? = null

    private fun registerLaunchers() {
        // 1. 自定义相册选择器
        imageSelectorLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableArrayListExtra(ImageSelectorActivity.EXTRA_RESULT_URIS, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableArrayListExtra(ImageSelectorActivity.EXTRA_RESULT_URIS)
                }
                
                // 允许空列表（表示用户取消了所有选择）
                if (uris != null) {
                    viewModel.tryAddImages(uris)
                }
            }
        }

        // 2. 相机
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            viewModel.onCameraResult(success)
        }

        // 3. 权限
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                performAction(pendingAction)
            } else {
                android.widget.Toast.makeText(this, "需要权限才能操作", android.widget.Toast.LENGTH_SHORT).show()
            }
            pendingAction = PendingAction.NONE
        }
    }

    /**
     * 真正的打开相册操作（由 VM Event 触发）
     */
    private fun openGalleryInternal() {
        try {
            val intent = Intent(this, ImageSelectorActivity::class.java).apply {
                putExtra(ImageSelectorActivity.EXTRA_CURRENT_COUNT, viewModel.imageList.value.size)
                putExtra(ImageSelectorActivity.EXTRA_MAX_COUNT, 9)
                // 传递已选择的图片列表
                putParcelableArrayListExtra(ImageSelectorActivity.EXTRA_SELECTED_URIS, ArrayList(viewModel.imageList.value))
            }
            imageSelectorLauncher?.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 准备文件并打开相机（由 VM Event 触发）
     */
    private fun prepareAndLaunchCamera() {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES))

            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)

            // 关键：将生成的 URI 传回给 VM 保存
            viewModel.setPendingCameraUri(uri)

            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "无法打开相机", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun getRequiredPermissions(action: PendingAction): List<String> {
        val permissions = mutableListOf<String>()
        when (action) {
            PendingAction.PICK_FROM_GALLERY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            PendingAction.TAKE_PHOTO -> {
                permissions.add(Manifest.permission.CAMERA)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            else -> {}
        }
        return permissions
    }
}