package com.dirror.lyricviewx

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Layout
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.Px
import java.io.File

const val GRAVITY_CENTER = 0    // 居中
const val GRAVITY_LEFT = 1      // 左
const val GRAVITY_RIGHT = 2     // 右

fun Int.toLayoutAlign(): Layout.Alignment {
    return when (this) {
        GRAVITY_LEFT -> Layout.Alignment.ALIGN_NORMAL
        GRAVITY_CENTER -> Layout.Alignment.ALIGN_CENTER
        GRAVITY_RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        else -> Layout.Alignment.ALIGN_CENTER
    }
}

/**
 * LyricViewX 接口
 * 从 LyricViewX 提取，方便管理
 *
 * @author Moriafly
 * @since 2021年1月28日16:29:16
 */
interface LyricViewXInterface {

    /**
     * 设置整句之间的间隔高度
     * @param height px
     */
    fun setSentenceDividerHeight(@Px height: Float)

    /**
     * 设置原句与翻译之间的间隔高度
     * @param height px
     */
    fun setTranslateDividerHeight(@Px height: Float)

    /**
     * 设置歌词整体的垂直偏移值，配合[setHorizontalOffsetPercent]使用
     * @param offset px
     *
     * @see [setHorizontalOffsetPercent]
     */
    fun setHorizontalOffset(@Px offset: Float)

    /**
     * 设置歌词整体的垂直偏移，相对于控件高度的百分比,0.5f即表示居中，配合[setHorizontalOffset]使用
     *
     * @param percent 0.0f ~ 1.0f
     *
     * @see [setHorizontalOffset]
     */
    fun setHorizontalOffsetPercent(@FloatRange(from = 0.0, to = 1.0) percent: Float)

    /**
     * 设置翻译相对与原词之间的缩放比例值
     * @param scaleValue 一般来说 0.8f 是个不错的值
     */
    fun setTranslateTextScaleValue(@FloatRange(from = 0.1, to = 2.0) scaleValue: Float)

    /**
     * 设置文字的对齐方向
     */
    fun setTextGravity(gravity: Int)

    /**
     * 设置非当前行歌词字体颜色 [normalColor]
     */
    fun setNormalColor(@ColorInt normalColor: Int)

    /**
     * 普通歌词文本字体大小 [size]，单位 px
     */
    fun setNormalTextSize(@Px size: Float)

    /**
     * 当前歌词文本字体大小
     */
    fun setCurrentTextSize(size: Float)

    /**
     * 设置当前行歌词的字体颜色
     */
    fun setCurrentColor(currentColor: Int)

    /**
     * 设置拖动歌词时选中歌词的字体颜色
     */
    fun setTimelineTextColor(timelineTextColor: Int)

    /**
     * 设置拖动歌词时时间线的颜色
     */
    fun setTimelineColor(timelineColor: Int)

    /**
     * 设置拖动歌词时右侧时间字体颜色
     */
    fun setTimeTextColor(timeTextColor: Int)

    /**
     * 设置歌词为空时屏幕中央显示的文字 [label]，如“暂无歌词”
     */
    fun setLabel(label: String)

    /**
     * 加载歌词文本
     * 两种语言的歌词时间戳需要一致
     *
     * @param mainLyricText 第一种语言歌词文本
     * @param secondLyricText 可选，第二种语言歌词文本
     */
    fun loadLyric(mainLyricText: String?, secondLyricText: String? = null)

    /**
     * 加载歌词 [LyricEntry] 集合
     * 如果你在 Service 等地方自行解析歌词包装成 [LyricEntry] 集合，那么可以使用此方法载入歌词
     *
     * @param lyricEntries 歌词集合
     * @since 1.3.1
     */
    fun loadLyric(lyricEntries: List<LyricEntry>)

    /**
     * 刷新歌词
     *
     * @param time 当前播放时间
     */
    fun updateTime(time: Long, force: Boolean = false)

    /**
     * 设置歌词是否允许拖动
     *
     * @param draggable 是否允许拖动
     * @param onPlayClickListener 设置歌词拖动后播放按钮点击监听器，如果允许拖动，则不能为 null
     */
    fun setDraggable(draggable: Boolean, onPlayClickListener: OnPlayClickListener?)

    /**
     * 设置单击
     */
    fun setOnSingerClickListener(onSingerClickListener: OnSingleClickListener?)

    /**
     * 获取当前歌词每句实体，可用于歌词分享
     *
     * @return LyricEntry 集合
     */
    fun getLyricEntryList(): List<LyricEntry>

    /**
     * 设置当前歌词每句实体
     */
    fun setLyricEntryList(newList: List<LyricEntry>)

    /**
     * 获取当前行歌词
     */
    fun getCurrentLineLyricEntry(): LyricEntry?

    /**
     * 为歌词设置自定义的字体
     *
     * @param file 字体文件
     */
    fun setLyricTypeface(file: File)

    /**
     * 为歌词设置自定义的字体
     *
     * @param path 字体文件路径
     */
    fun setLyricTypeface(path: String)

    /**
     * 为歌词设置自定义的字体，可为空，若为空则应清除字体
     *
     * @param typeface 字体对象
     */
    fun setLyricTypeface(typeface: Typeface?)

    /**
     * 为歌词的过渡动画设置阻尼比（数值越大，回弹次数越多）
     *
     * @param dampingRatio 阻尼比 详见[androidx.dynamicanimation.animation.SpringForce]
     */
    fun setDampingRatioForLyric(dampingRatio: Float)

    /**
     * 为歌词视图的滚动动画设置阻尼比（数值越大，回弹次数越多）
     *
     * @param dampingRatio 阻尼比 详见[androidx.dynamicanimation.animation.SpringForce]
     */
    fun setDampingRatioForViewPort(dampingRatio: Float)

    /**
     * 为歌词的过渡动画设置刚度（数值越大，动画越短）
     *
     * @param stiffness 刚度 详见[androidx.dynamicanimation.animation.SpringForce]
     */
    fun setStiffnessForLyric(stiffness: Float)

    /**
     * 为歌词视图的滚动动画设置刚度（数值越大，动画越短）
     *
     * @param stiffness 刚度 详见[androidx.dynamicanimation.animation.SpringForce]
     */
    fun setStiffnessForViewPort(stiffness: Float)

    /**
     * 设置跳转播放按钮
     */
    fun setPlayDrawable(drawable: Drawable)

    /**
     * 设置是否绘制歌词翻译
     */
    fun setIsDrawTranslation(isDrawTranslation: Boolean)

    /**
     * 是否开启特定的模糊效果
     */
    fun setIsEnableBlurEffect(isEnableBlurEffect: Boolean)

    /**
     * 设置元素的偏移百分比，0.5f即表示居中
     *
     * @param itemOffsetPercent 0f ~ 1f 偏移百分比
     */
    fun setItemOffsetPercent(@FloatRange(from = 0.0, to = 1.0) itemOffsetPercent: Float)
}

/**
 * 播放按钮点击监听器，点击后应该跳转到指定播放位置
 */
interface OnPlayClickListener {
    /**
     * 播放按钮被点击，应该跳转到指定播放位置
     *
     * @return 是否成功消费该事件，如果成功消费，则会更新UI
     */
    fun onPlayClick(time: Long): Boolean
}

/**
 * 点击歌词布局
 */
interface OnSingleClickListener {
    fun onClick()
}