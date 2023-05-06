package com.dirror.lyricviewx

import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/**
 * 一行歌词实体
 * @since 2021年1月19日09:51:40 Moriafly 基于 LrcEntry 改造，转换为 kt ，移除部分过时方法
 * @param time 歌词时间
 * @param text 歌词文本
 */
class LyricEntry(val time: Long, val text: String) : Comparable<LyricEntry> {

    /**
     * 第二文本
     */
    var secondText: String? = null

    /**
     * staticLayout
     */
    var staticLayout: StaticLayout? = null
        private set

    var secondStaticLayout: StaticLayout? = null
        private set

    @Deprecated("存在不显示翻译的情况，会导致offset发生改变，故不再固定存储offset")
    /**
     * 歌词距离视图顶部的距离
     */
    var offset = Float.MIN_VALUE

    /**
     * 初始化
     * @param textPaint 文本画笔
     * @param width 宽度
     * @param align 位置
     */
    fun init(
        textPaint: TextPaint,
        secondTextPaint: TextPaint,
        width: Int, align: Layout.Alignment
    ) {
        staticLayout = createStaticLayout(text, textPaint, width, align)
        secondStaticLayout = createStaticLayout(secondText, secondTextPaint, width, align)
        offset = Float.MIN_VALUE
    }

    /**
     * 继承 Comparable 比较
     * @param other LyricEntry
     * @return 时间差
     */
    override fun compareTo(other: LyricEntry): Int {
        return (time - other.time).toInt()
    }

    companion object {
        fun createStaticLayout(
            text: String?,
            paint: TextPaint,
            width: Number,
            align: Layout.Alignment
        ): StaticLayout? {
            if (text == null || text.isEmpty()) return null
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder
                    .obtain(text, 0, text.length, paint, width.toInt())
                    .setAlignment(align)
                    .setLineSpacing(0f, 1f)
                    .setIncludePad(false)
                    .build()
            } else {
                StaticLayout(text, paint, width.toInt(), align, 1f, 0f, false)
            }
        }
    }

}