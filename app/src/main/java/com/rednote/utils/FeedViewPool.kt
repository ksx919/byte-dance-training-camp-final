package com.rednote.utils

import android.content.Context
import com.rednote.ui.main.home.FeedItemView
import java.util.LinkedList

object FeedViewPool {
    private val pool = LinkedList<FeedItemView>()

    // 在 Application 中调用
    fun preCreateViews(context: Context, count: Int) {
        // 注意：这里需要主线程，但利用 IdleHandler 可以不阻碍 UI
        android.os.Looper.myQueue().addIdleHandler {
            for (i in 0 until count) {
                pool.offer(FeedItemView(context))
            }
            false // false 表示执行一次后移除
        }
    }

    fun get(context: Context): FeedItemView? {
        return pool.poll() // 如果池里有，直接拿；没有返回 null
    }
}