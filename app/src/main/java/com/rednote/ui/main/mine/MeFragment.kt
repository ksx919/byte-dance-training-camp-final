package com.rednote.ui.main.mine

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.rednote.R
import com.rednote.databinding.FragmentMeBinding
import com.rednote.ui.base.BaseFragment
import com.rednote.ui.login.LoginActivity
import com.rednote.utils.UserManager

class MeFragment : BaseFragment<FragmentMeBinding, MeViewModel>() {

    override val viewModel: MeViewModel by viewModels()

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
            // TODO: 加载头像 user.avatarUrl
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

        // 设置背景透明，以便显示圆角
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val newNickname = etNickname.text.toString().trim()
            val newBio = etBio.text.toString().trim()

            if (newNickname.isNotEmpty()) {
                viewModel.updateUserInfo(newNickname, newBio) {
                    // 更新成功回调
                    refreshUserInfo()
                    dialog.dismiss()
                }
            } else {
                // 提示昵称不能为空 (可选)
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
                // 跳转到登录页
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}