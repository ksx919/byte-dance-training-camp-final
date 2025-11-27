package com.rednote.ui.publish

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView

class ImageLayoutHelper(
    private val recyclerView: RecyclerView,
    private val addButton: View,
    private val container: ConstraintLayout,
    private val resources: Resources
) {

    private val density = resources.displayMetrics.density
    private val itemWidth = (100 * density).toInt() // 每张图片宽度（100dp转px）
    private val itemMargin = (4 * density).toInt() // 图片间距（8dp转px）

    /**
     * 根据图片数量更新布局
     * @param imageCount 当前图片数量
     */
    fun updateLayout(imageCount: Int) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(container)

        if (imageCount == 0) {
            // 没有图片：RecyclerView隐藏，加号在最左边
            constraintSet.setVisibility(recyclerView.id, View.GONE)
            constraintSet.clear(addButton.id, ConstraintSet.START)
            constraintSet.clear(addButton.id, ConstraintSet.END)
            // 只约束START到parent的START，不约束END，让按钮保持固定宽度在最左边
            constraintSet.connect(
                addButton.id,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                0
            )
        } else {
            // 有图片：显示RecyclerView
            constraintSet.setVisibility(recyclerView.id, View.VISIBLE)
            constraintSet.clear(addButton.id, ConstraintSet.START)
            constraintSet.clear(addButton.id, ConstraintSet.END)

            if (imageCount <= 2) {
                // 1-2张图片：RecyclerView自适应宽度，加号紧贴在图片后
                constraintSet.constrainWidth(recyclerView.id, ConstraintSet.WRAP_CONTENT)
                
                // 加号紧贴在RecyclerView后面
                constraintSet.connect(
                    addButton.id,
                    ConstraintSet.START,
                    recyclerView.id,
                    ConstraintSet.END,
                    itemMargin
                )
            } else {
                // 超过2张图片：RecyclerView占满剩余空间 (0dp)
                // 这样可以保证RecyclerView在加号左边，且不会遮挡加号，同时支持滑动
                constraintSet.constrainWidth(recyclerView.id, ConstraintSet.MATCH_CONSTRAINT)
                
                // 加号固定在右侧
                constraintSet.connect(
                    addButton.id,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END,
                    0
                )
            }
        }

        constraintSet.applyTo(container)
    }
}

