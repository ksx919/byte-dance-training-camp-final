package com.rednote

import android.app.Application
import androidx.appcompat.view.ContextThemeWrapper
import com.bumptech.glide.Glide
import com.rednote.utils.CommentUIConfig
import com.rednote.utils.DraftManager
import com.rednote.utils.FeedUIConfig
import com.rednote.utils.FeedViewPool
import com.tencent.mmkv.MMKV

class RedNoteApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // --- 初始化  ---
        MMKV.initialize(this)
        FeedUIConfig.init(this)
        Glide.get(this)
        FeedViewPool.preCreateViews(ContextThemeWrapper(this, R.style.Theme_Rednote), 10)
        CommentUIConfig.init(this)
        DraftManager.init(this)
    }
}