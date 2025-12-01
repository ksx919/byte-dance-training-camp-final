package com.rednote.ui.publish

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.rednote.R
import com.rednote.databinding.ActivityAddBinding
import com.rednote.ui.base.BaseActivity
import com.rednote.utils.DraftManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddActivity : BaseActivity<ActivityAddBinding>() {

    private val viewModel: PublishViewModel by viewModels()
    private lateinit var imageAdapter: ImageAdapter

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    // 临时记录需要请求权限后执行的操作类型
    private var pendingAction: PendingAction = PendingAction.NONE
    private enum class PendingAction { NONE, PICK_FROM_GALLERY, TAKE_PHOTO }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 初始化草稿管理器 (建议移到 Application 中，但放在这里也可以)
        DraftManager.init(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    override fun getViewBinding(): ActivityAddBinding {
        return ActivityAddBinding.inflate(layoutInflater)
    }

    override fun initView() {
        setupEdgeToEdge()
        setupRecyclerView()
        setupInputs()
        registerLaunchers()
        binding.btnPublishBottom.setOnClickListener {
            viewModel.publish()
        }
        try {
            val closeBtn = binding.root.findViewById<View>(R.id.btn_back)
            closeBtn?.setOnClickListener { handleBackPress() }
        } catch (e: Exception) {}
    }

    override fun initData() {
        viewModel.loadDraft()
        observeViewModel()
    }

    override fun onPause() {
        super.onPause()
        // 如果不是 finish() 导致的暂停（即：切后台、锁屏、跳转其他App），则执行智能保存/清理
        // 这样可以解决：
        // 1. 用户编辑一半切后台 -> saveDraft -> 保存成功
        // 2. 用户清空内容切后台 -> clearDraft -> 草稿被删，下次进来是空的
        if (!isFinishing) {
            viewModel.saveDraftOrClear()
        }
    }

    private fun handleBackPress() {
        if (viewModel.hasUnsavedChanges()) {
            showSaveDraftDialog()
        } else {
            // 如果没有内容，直接退出，onPause 会因为 isFinishing = true 而跳过
            // 但为了保险起见，可以在这里也清理一下（虽然内容已经是空的了）
            viewModel.clearDraft()
            finish()
        }
    }

    private fun showSaveDraftDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("保存草稿")
            .setMessage("是否保存当前内容为草稿？")
            .setPositiveButton("保存") { _, _ ->
                // 明确用户点击保存
                viewModel.saveDraft()
                finish()
            }
            .setNegativeButton("不保存") { _, _ ->
                // 明确用户点击不保存，清理草稿
                viewModel.clearDraft()
                finish()
            }
            .setNeutralButton("取消", null)
            .show()
    }

    private fun setupInputs() {
        binding.etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val content = s?.toString() ?: ""
                viewModel.updateTitle(content)
                binding.tvTitleCount.text = "${content.length}/20"
            }
        })

        binding.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val content = s?.toString() ?: ""
                viewModel.updateContent(content)
                binding.tvContentCount.text = "${content.length}/1000"
            }
        })
    }

    private var isKeyboardVisible = false

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.add) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val horizontalPadding = dpToPx(16)
            v.setPadding(
                systemBars.left + horizontalPadding,
                systemBars.top,
                systemBars.right + horizontalPadding,
                systemBars.bottom
            )

            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (isImeVisible != isKeyboardVisible) {
                isKeyboardVisible = isImeVisible
                animateImagePicker(!isImeVisible)
            }

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

    private fun animateImagePicker(show: Boolean) {
        val rv = binding.rvSelectedImages
        val layout = binding.layoutImagePicker

        rv.pivotX = 0f
        rv.pivotY = 0f

        val startScale = rv.scaleX
        val endScale = if (show) 1f else 0.5f

        val startHeight = layout.height
        val endHeight = if (show) dpToPx(80) else dpToPx(40)

        val startMargin = (layout.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0
        val endMargin = if (show) dpToPx(24) else dpToPx(12)

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 300
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedFraction
            val newScale = startScale + (endScale - startScale) * fraction
            rv.scaleX = newScale
            rv.scaleY = newScale

            val newHeight = (startHeight + (endHeight - startHeight) * fraction).toInt()
            val layoutParams = layout.layoutParams
            layoutParams.height = newHeight
            layout.layoutParams = layoutParams

            val newMargin = (startMargin + (endMargin - startMargin) * fraction).toInt()
            val marginParams = layout.layoutParams as? ViewGroup.MarginLayoutParams
            marginParams?.topMargin = newMargin
            layout.layoutParams = marginParams
        }
        animator.start()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                launch {
                    viewModel.title.collect { title ->
                        if (binding.etTitle.text.toString() != title) {
                            binding.etTitle.setText(title)
                        }
                    }
                }
                launch {
                    viewModel.content.collect { content ->
                        if (binding.etContent.text.toString() != content) {
                            binding.etContent.setText(content)
                        }
                    }
                }
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
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
            is PublishViewEvent.OpenGallery -> {
                openGalleryInternal()
            }
            is PublishViewEvent.PrepareCamera -> {
                prepareAndLaunchCamera()
            }
            is PublishViewEvent.Finish -> {
                finish()
            }
        }
    }

    private fun showImageSourceDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_select_image_source, null)
        dialog.setContentView(view)

        try {
            val parent = view.parent as? View
            parent?.setBackgroundColor(Color.TRANSPARENT)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        view.findViewById<TextView>(R.id.tv_take_photo).setOnClickListener {
            dialog.dismiss()
            checkPermissionAndAction(PendingAction.TAKE_PHOTO)
        }
        view.findViewById<TextView>(R.id.tv_pick_album).setOnClickListener {
            dialog.dismiss()
            checkPermissionAndAction(PendingAction.PICK_FROM_GALLERY)
        }
        view.findViewById<TextView>(R.id.tv_cancel).setOnClickListener {
            dialog.dismiss()
        }
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
            PendingAction.PICK_FROM_GALLERY -> viewModel.onPickImageClick()
            PendingAction.TAKE_PHOTO -> viewModel.onTakePhotoClick()
            else -> {}
        }
    }

    private var imageSelectorLauncher: ActivityResultLauncher<Intent>? = null

    private fun registerLaunchers() {
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
                if (uris != null) {
                    viewModel.tryAddImages(uris)
                }
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            viewModel.onCameraResult(success)
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                performAction(pendingAction)
            } else {
                Toast.makeText(this, "需要权限才能操作", Toast.LENGTH_SHORT).show()
            }
            pendingAction = PendingAction.NONE
        }
    }

    private fun openGalleryInternal() {
        try {
            val intent = Intent(this, ImageSelectorActivity::class.java).apply {
                putExtra(ImageSelectorActivity.EXTRA_CURRENT_COUNT, viewModel.imageList.value.size)
                putExtra(ImageSelectorActivity.EXTRA_MAX_COUNT, 9)
                putParcelableArrayListExtra(ImageSelectorActivity.EXTRA_SELECTED_URIS, ArrayList(viewModel.imageList.value))
            }
            imageSelectorLauncher?.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun prepareAndLaunchCamera() {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES))
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
            viewModel.setPendingCameraUri(uri)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开相机", Toast.LENGTH_SHORT).show()
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