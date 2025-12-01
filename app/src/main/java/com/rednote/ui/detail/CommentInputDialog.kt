package com.rednote.ui.detail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rednote.R
import com.rednote.databinding.DialogCommentInputBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentInputDialog : BottomSheetDialogFragment() {

    private var _binding: DialogCommentInputBinding? = null
    private val binding get() = _binding!!

    private var postId: Long = -1L
    private var parentId: Long? = null
    private var rootParentId: Long? = null
    private var replyToUserId: Long? = null
    private var replyToUserName: String? = null

    private var selectedImageUri: Uri? = null
    private var pendingCameraUri: Uri? = null
    
    var onSendComment: ((String, Uri?, Long?, Long?, Long?, String?) -> Unit)? = null

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    companion object {
        fun newInstance(
            postId: Long,
            parentId: Long? = null,
            rootParentId: Long? = null,
            replyToUserId: Long? = null,
            replyToUserName: String? = null
        ): CommentInputDialog {
            val fragment = CommentInputDialog()
            val args = Bundle()
            args.putLong("POST_ID", postId)
            if (parentId != null) args.putLong("PARENT_ID", parentId)
            if (rootParentId != null) args.putLong("ROOT_PARENT_ID", rootParentId)
            if (replyToUserId != null) args.putLong("REPLY_TO_USER_ID", replyToUserId)
            if (replyToUserName != null) args.putString("REPLY_TO_USER_NAME", replyToUserName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            postId = it.getLong("POST_ID")
            if (it.containsKey("PARENT_ID")) parentId = it.getLong("PARENT_ID")
            if (it.containsKey("ROOT_PARENT_ID")) rootParentId = it.getLong("ROOT_PARENT_ID")
            if (it.containsKey("REPLY_TO_USER_ID")) replyToUserId = it.getLong("REPLY_TO_USER_ID")
            replyToUserName = it.getString("REPLY_TO_USER_NAME")
        }
        registerLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCommentInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        dialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        dialog?.behavior?.skipCollapsed = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 对于 API 30 及更高版本，使用新的 WindowInsets API
            dialog?.window?.let { window ->
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                window.decorView.windowInsetsController?.show(android.view.WindowInsets.Type.ime())
            }
        } else {
            @Suppress("DEPRECATION")
            dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        (view?.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        binding.etComment.requestFocus()
        binding.etComment.postDelayed({
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etComment, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }

    private fun initView() {
        if (!replyToUserName.isNullOrEmpty()) {
            binding.etComment.hint = "回复 $replyToUserName"
        }

        binding.btnSend.setOnClickListener {
            sendComment()
        }

        binding.ivImagePicker.setOnClickListener {
            showImageSourceDialog()
        }

        binding.ivDeleteImage.setOnClickListener {
            selectedImageUri = null
            binding.cvImagePreview.visibility = View.GONE
            binding.ivImagePicker.visibility = View.VISIBLE
        }
    }

    private fun registerLaunchers() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && pendingCameraUri != null) {
                selectedImageUri = pendingCameraUri
                showImagePreview(pendingCameraUri!!)
            }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                showImagePreview(uri)
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showImageSourceDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_select_image_source, null)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tv_take_photo).setOnClickListener {
            dialog.dismiss()
            checkCameraPermission()
        }

        view.findViewById<TextView>(R.id.tv_pick_album).setOnClickListener {
            dialog.dismiss()
            openGallery()
        }

        view.findViewById<TextView>(R.id.tv_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val photoFile = File.createTempFile(
            "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}_",
            ".jpg",
            requireContext().externalCacheDir
        )
        pendingCameraUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(pendingCameraUri!!)
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun showImagePreview(uri: Uri) {
        binding.cvImagePreview.visibility = View.VISIBLE
        binding.ivImagePicker.visibility = View.GONE
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(binding.ivPreview)
    }

    private fun sendComment() {
        val content = binding.etComment.text.toString().trim()
        if (content.isEmpty() && selectedImageUri == null) {
            Toast.makeText(requireContext(), "请输入评论内容或选择图片", Toast.LENGTH_SHORT).show()
            return
        }

        // Pass data back to activity for optimistic update
        onSendComment?.invoke(content, selectedImageUri, parentId, rootParentId, replyToUserId, replyToUserName)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
