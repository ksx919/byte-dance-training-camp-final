package com.rednote

import android.app.Application
import android.util.Log
import com.tencent.mmkv.MMKV

class RedNoteApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // --- 初始化 MMKV ---
        MMKV.initialize(this)
    }
}