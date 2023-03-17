package com.dirror.lyricviewx

import android.util.LruCache

/**
 * 用于存储动画进度的帮助类
 */
interface ProgressKeeper {
    val progressMap: LruCache<Int, Float>
    val currentIndex: Int
    var needPostInvalidate: Boolean

    fun onPostInvalidate()

    /**
     * 在Canvas的draw方法中调用，用于更新进度
     */
    fun updateProgress(index: Int, currentIndexProgress: Float) {
        val targetProgress = if (index == currentIndex) currentIndexProgress else 0f

        // 初始化进度值，避免空指针
        progressMap.put(index, progressMap[index] ?: 0f)
        progressMap.put(index, progressMap[index]!! + (targetProgress - progressMap[index]!!) * 0.1f)

        // 当元素的进度接近目标进度时，直接设置为目标进度
        if (progressMap[index]!! - targetProgress <= 0.01f) {
            progressMap.put(index, targetProgress)
        }

        // 当元素的进度不等于目标进度时，需要继续调用postInvalidate
        if (progressMap[index] != targetProgress && !needPostInvalidate) {
            needPostInvalidate = true
        }
    }

    /**
     * 在Canvas的draw方法最后调用
     */
    fun onProgressPostHandle() {
        if (needPostInvalidate) {
            onPostInvalidate()
            needPostInvalidate = false
        }
    }

    fun getProgressByIndex(index: Int): Float {
        return progressMap[index] ?: 0f
    }
}