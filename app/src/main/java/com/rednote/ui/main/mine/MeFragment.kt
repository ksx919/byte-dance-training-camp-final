package com.rednote.ui.main.mine

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.rednote.R
import com.rednote.databinding.FragmentMeBinding
import com.rednote.ui.base.BaseFragment
import com.rednote.ui.login.LoginActivity
import com.rednote.utils.UserManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class MeFragment : BaseFragment<FragmentMeBinding, MeViewModel>() {

    override val viewModel: MeViewModel by viewModels()

    // 图片选择器
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            uploadAvatar(uri)
        }
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMeBinding {
        return FragmentMeBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        // 编辑资料
        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        // 退出登录
        binding.btnLogout.setOnClickListener {
            performLogout()
        }

        // 点击头像修改
        binding.ivAvatar.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    override fun initObservers() {
        // 观察 ViewModel 的状态变化（如果有）
    }

    override fun initData() {
        refreshUserInfo()
    }

    private fun refreshUserInfo() {
        val user = UserManager.getUser()
        if (user != null) {
            binding.tvNickname.text = user.nickname
            binding.tvRedId.text = "小红书号：${user.id}"
            binding.tvBio.text = if (user.bio.isNullOrEmpty()) "暂时还没有简介" else user.bio
            
            // 加载头像
            loadAvatar(user.avatarUrl)
        }
    }

    private fun loadAvatar(url: String?) {
        if (url.isNullOrEmpty()) {
            binding.ivAvatar.setImageResource(R.mipmap.ic_launcher)
            return
        }

        // 创建圆形进度条 Drawable
        val circularProgressDrawable = androidx.swiperefreshlayout.widget.CircularProgressDrawable(requireContext())
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 30f
        circularProgressDrawable.start()

        // 使用 Glide加载头像
        Glide.with(this)
            .load(url)
            .placeholder(circularProgressDrawable) // 设置加载动画
            .error(R.mipmap.ic_launcher)
            .circleCrop()
            .into(binding.ivAvatar)
    }

    private fun uploadAvatar(uri: Uri) {
        val file = uriToFile(uri) ?: return
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("avatar", file.name, requestFile)

        viewModel.uploadAvatar(body) { newUrl ->
            loadAvatar(newUrl)
        }
    }

    // 将 Uri 转换为 File
    private fun uriToFile(uri: Uri): File? {
        return try {
            val contentResolver = requireContext().contentResolver
            val tempFile = File.createTempFile("avatar", ".jpg", requireContext().cacheDir)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showEditProfileDialog() {
        val user = UserManager.getUser() ?: return

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
        val etNickname = dialogView.findViewById<EditText>(R.id.et_nickname)
        val etBio = dialogView.findViewById<EditText>(R.id.et_bio)
        val btnCancel = dialogView.findViewById<android.view.View>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<android.view.View>(R.id.btn_save)

        etNickname.setText(user.nickname)
        etBio.setText(user.bio)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val newNickname = etNickname.text.toString().trim()
            val newBio = etBio.text.toString().trim()

            if (newNickname.isNotEmpty()) {
                viewModel.updateUserInfo(newNickname, newBio) {
                    refreshUserInfo()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun performLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("提示")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定") { _, _ ->
                UserManager.logout()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}