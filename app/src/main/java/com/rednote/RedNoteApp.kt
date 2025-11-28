package com.rednote

import android.app.Application
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import com.bumptech.glide.Glide
import com.rednote.utils.FeedUIConfig
import com.rednote.utils.FeedViewPool
import com.tencent.mmkv.MMKV

class RedNoteApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // --- 初始化 MMKV ---
        MMKV.initialize(this)
        FeedUIConfig.init(this)
        Glide.get(this)
        FeedViewPool.preCreateViews(this, 10)
    }
}