package com.dirror.lyricviewx

import java.io.File

/**
 * LyricViewX 接口
 * 从 LyricViewX 提取，方便管理
 * @author Moriafly
 * @since 2021年1月28日16:29:16
 */
interface LyricViewXInterface {

    /**
     * 设置非当前行歌词字体颜色 [normalColor]
     */
    fun setNormalColor(normalColor: Int)

    /**
     * 普通歌词文本字体大小 [size]，单位 px
     */
    fun setNormalTextSize(size: Float)

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
     * 加载歌词文件
     * 两种语言的歌词时间戳需要一致
     * @param mainLyricFile 第一种语言歌词文件
     * @param secondLyricFile 可选，第二种语言歌词文件
     */
    fun loadLyric(mainLyricFile: File, secondLyricFile: File? = null)

    /**
     * 加载歌词文本
     * 两种语言的歌词时间戳需要一致
     * @param mainLyricText 第一种语言歌词文本
     * @param secondLyricText 可选，第二种语言歌词文本
     */
    fun loadLyric(mainLyricText: String?, secondLyricText: String? = null)

    /**
     * 加载在线歌词
     * @param lyricUrl  歌词文件的网络地址
     * @param charset 编码格式
     */
    fun loadLyricByUrl(lyricUrl: String, charset: String? = "utf-8")

    /**
     * 刷新歌词
     * @param time 当前播放时间
     */
    fun updateTime(time: Long)

    /**
     * 设置歌词是否允许拖动
     * @param draggable 是否允许拖动
     * @param onPlayClickListener 设置歌词拖动后播放按钮点击监听器，如果允许拖动，则不能为 null
     */
    fun setDraggable(draggable: Boolean, onPlayClickListener: OnPlayClickListener?)

    /**
     * 设置单击
     */
    fun setOnSingerClickListener(onSingerClickListener: OnSingleClickListener?)

    /**
     * @新增加
     * 获取当前歌词每句实体，可用于歌词分享
     * @return LyricEntry 集合
     */
    fun getLyricEntryList(): List<LyricEntry>

    /**
     * @新增加
     * 设置当前歌词每句实体
     */
    fun setLyricEntryList(newList: List<LyricEntry>)

    /**
     * @新增加
     * 获取当前行歌词
     */
    fun getCurrentLineLyricEntry(): LyricEntry?
}

/**
 * 播放按钮点击监听器，点击后应该跳转到指定播放位置
 */
interface OnPlayClickListener {
    /**
     * 播放按钮被点击，应该跳转到指定播放位置
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