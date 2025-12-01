package com.rednote.utils

object ImageSizeUtils {
    /**
     * 统一计算封面图尺寸
     * @param originWidth 图片原始宽度
     * @param originHeight 图片原始高度
     * @return Pair(计算后的宽, 计算后的高)
     */
    fun calculateCoverSize(originWidth: Int, originHeight: Int): Pair<Int, Int> {
        // 获取列表页固定的卡片宽度
        val targetWidth = FeedUIConfig.itemWidth

        // 默认正方形
        if (originWidth <= 0 || originHeight <= 0) {
            return targetWidth to targetWidth
        }

        val ratio = originHeight.toFloat() / originWidth.toFloat()
        // 核心：这里的限制逻辑必须与列表页完全一致
        val maxRatio = 1.33f
        val finalRatio = if (ratio > maxRatio) maxRatio else ratio

        val targetHeight = (targetWidth * finalRatio).toInt()

        return targetWidth to targetHeight
    }
}