package com.rednote.ui.main.mine

import com.rednote.data.api.RetrofitClient
import com.rednote.data.model.UserInfo
import com.rednote.ui.base.BaseViewModel
import com.rednote.utils.UserManager

class MeViewModel : BaseViewModel() {

    fun updateUserInfo(nickname: String, bio: String, onSuccess: () -> Unit) {
        val currentUser = UserManager.getUser() ?: return
        
        // 创建更新后的 UserInfo 对象
        val updatedUser = currentUser.copy(
            nickname = nickname,
            bio = bio
        )

        launchDataLoad {
            val response = RetrofitClient.userApiService.updateUserInfo(updatedUser)
            if (response.code == 200 && response.data == true) {
                // 更新成功，保存到本地
                UserManager.saveUser(updatedUser)
                showToast("更新成功")
                onSuccess()
            } else {
                showToast(response.msg ?: "更新失败")
            }
        }
    }

    fun uploadAvatar(part: okhttp3.MultipartBody.Part, onSuccess: (String) -> Unit) {
        launchDataLoad {
            val response = RetrofitClient.userApiService.uploadAvatar(part)
            if (response.code == 200 && response.data != null) {
                val newAvatarUrl = response.data
                // 更新本地缓存
                val currentUser = UserManager.getUser()
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(avatarUrl = newAvatarUrl)
                    UserManager.saveUser(updatedUser)
                }
                showToast("头像上传成功")
                onSuccess(newAvatarUrl)
            } else {
                showToast(response.msg ?: "头像上传失败")
            }
        }
    }
}
