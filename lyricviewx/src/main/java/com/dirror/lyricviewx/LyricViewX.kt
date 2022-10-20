@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.dirror.lyricviewx

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.Scroller
import androidx.core.content.ContextCompat
import com.dirror.lyricviewx.LyricUtil.formatTime
import com.dirror.lyricviewx.LyricUtil.getContentFromNetwork
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max

/**
 * LyricViewX
 * Based on https://github.com/zion223/NeteaseCloudMusic-MVVM Kotlin 重构
 *
 * @change Moriafly
 * @since 2021年1月22日15:25:24
 */
@SuppressLint("StaticFieldLeak")
open class LyricViewX @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), LyricViewXInterface {
    protected val readyHelper = ReadyHelper()

    private val lyricEntryList: MutableList<LyricEntry> = ArrayList() // 单句歌词集合
    private val lyricPaint = TextPaint() // 歌词画笔
    private val secondLyricPaint = TextPaint() // 歌词画笔
    private val timePaint = TextPaint() // 时间文字画笔
    private var mTimeFontMetrics: Paint.FontMetrics? = null

    /** 跳转播放按钮 */
    private var playDrawable: Drawable? = null

    private var mTranslateDividerHeight = 0f
    private var mSentenceDividerHeight = 0f
    private var mAnimationDuration: Long = 0
    private var mNormalTextColor = 0
    private var mNormalTextSize = 0f
    private var mCurrentTextColor = 0
    private var mCurrentTextSize = 0f
    private var mTranslateTextScaleValue = 1f
    private var mTimelineTextColor = 0
    private var mTimelineColor = 0
    private var mTimeTextColor = 0
    private var mDrawableWidth = 0
    private var mTimeTextWidth = 0
    private var mDefaultLabel: String? = null
    private var mLrcPadding = 0f
    private var mOnPlayClickListener: OnPlayClickListener? = null
    private var mOnSingerClickListener: OnSingleClickListener? = null
    private var animator: ValueAnimator? = null
    private var mGestureDetector: GestureDetector? = null
    private var mScroller: Scroller? = null
    private var mOffset = 0f
    private var mCurrentLine = 0
    private var flag: Any? = null
    private var isShowTimeline = false
    private var isTouching = false
    private var isFling = false
    private var mTextGravity = GRAVITY_CENTER // 歌词显示位置，靠左 / 居中 / 靠右
    private var mHorizontalOffset: Float = 0f

    /**
     * 调用 [smoothScrollTo] 动画使用的插值器
     *
     * @since 1.3.0
     */
    var smoothScrollInterpolator: Interpolator = DecelerateInterpolator()

    @SuppressLint("CustomViewStyleable")
    private fun init(attrs: AttributeSet?) {
        readyHelper.readyState = STATE_INITIALIZING
        val ta = context.obtainStyledAttributes(attrs, R.styleable.LyricView)
        mCurrentTextSize = ta.getDimension(
            R.styleable.LyricView_lrcTextSize,
            resources.getDimension(R.dimen.lrc_text_size)
        )
        mNormalTextSize = ta.getDimension(
            R.styleable.LyricView_lrcNormalTextSize,
            resources.getDimension(R.dimen.lrc_text_size)
        )
        if (mNormalTextSize == 0f) {
            mNormalTextSize = mCurrentTextSize
        }

        mSentenceDividerHeight = ta.getDimension(
            R.styleable.LyricView_lrcSentenceDividerHeight,
            resources.getDimension(R.dimen.lrc_sentence_divider_height)
        )
        mTranslateDividerHeight = ta.getDimension(
            R.styleable.LyricView_lrcTranslateDividerHeight,
            resources.getDimension(R.dimen.lrc_translate_divider_height)
        )
        val defDuration = resources.getInteger(R.integer.lrc_animation_duration)
        mAnimationDuration = ta.getInt(
            R.styleable.LyricView_lrcAnimationDuration, defDuration
        ).toLong()
        mAnimationDuration =
            if (mAnimationDuration < 0) defDuration.toLong() else mAnimationDuration

        mNormalTextColor = ta.getColor(
            R.styleable.LyricView_lrcNormalTextColor,
            ContextCompat.getColor(context, R.color.lrc_normal_text_color)
        )
        mCurrentTextColor = ta.getColor(
            R.styleable.LyricView_lrcCurrentTextColor,
            ContextCompat.getColor(context, R.color.lrc_current_text_color)
        )
        mTimelineTextColor = ta.getColor(
            R.styleable.LyricView_lrcTimelineTextColor,
            ContextCompat.getColor(context, R.color.lrc_timeline_text_color)
        )
        mDefaultLabel = ta.getString(R.styleable.LyricView_lrcLabel).toString()
        mDefaultLabel = if (mDefaultLabel.isNullOrEmpty()) {
            "暂无歌词"
        } else {
            mDefaultLabel
        }
        mLrcPadding = ta.getDimension(R.styleable.LyricView_lrcPadding, 0f)
        mTimelineColor = ta.getColor(
            R.styleable.LyricView_lrcTimelineColor,
            ContextCompat.getColor(context, R.color.lrc_timeline_color)
        )
        val timelineHeight = ta.getDimension(
            R.styleable.LyricView_lrcTimelineHeight,
            resources.getDimension(R.dimen.lrc_timeline_height)
        )
        playDrawable = ta.getDrawable(R.styleable.LyricView_lrcPlayDrawable)
        playDrawable = if (playDrawable == null) ContextCompat.getDrawable(
            context,
            R.drawable.lrc_play
        ) else playDrawable
        mTimeTextColor = ta.getColor(
            R.styleable.LyricView_lrcTimeTextColor,
            ContextCompat.getColor(context, R.color.lrc_time_text_color)
        )
        val timeTextSize = ta.getDimension(
            R.styleable.LyricView_lrcTimeTextSize,
            resources.getDimension(R.dimen.lrc_time_text_size)
        )
        mTextGravity = ta.getInteger(R.styleable.LyricView_lrcTextGravity, GRAVITY_CENTER)
        mTranslateTextScaleValue = ta.getFloat(R.styleable.LyricView_lrcTranslateTextScaleValue, 1f)
        mHorizontalOffset = ta.getDimension(R.styleable.LyricView_lrcHorizontalOffset, 0f)
        ta.recycle()
        mDrawableWidth = resources.getDimension(R.dimen.lrc_drawable_width).toInt()
        mTimeTextWidth = resources.getDimension(R.dimen.lrc_time_width).toInt()
        lyricPaint.isAntiAlias = true
        lyricPaint.textSize = mCurrentTextSize
        lyricPaint.textAlign = Paint.Align.LEFT
//        lyricPaint.setShadowLayer(0.1f, 0f, 1f, Color.DKGRAY)
        secondLyricPaint.isAntiAlias = true
        secondLyricPaint.textSize = mCurrentTextSize
        secondLyricPaint.textAlign = Paint.Align.LEFT
//        secondLyricPaint.setShadowLayer(0.1f, 0f, 1f, Color.DKGRAY)
        timePaint.isAntiAlias = true
        timePaint.textSize = timeTextSize
        timePaint.textAlign = Paint.Align.CENTER
        timePaint.strokeWidth = timelineHeight
        timePaint.strokeCap = Paint.Cap.ROUND
        mTimeFontMetrics = timePaint.fontMetrics
        mGestureDetector = GestureDetector(context, mSimpleOnGestureListener)
        mGestureDetector!!.setIsLongpressEnabled(false)
        mScroller = Scroller(context)
    }

    /**
     * 歌词是否有效
     * @return true，如果歌词有效，否则false
     */
    private fun hasLrc(): Boolean {
        return lyricEntryList.isNotEmpty()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            initPlayDrawable()
            initEntryList()
            if (hasLrc()) {
                smoothScrollTo(mCurrentLine, 0L)
            }
        }
        readyHelper.readyState = STATE_INITIALIZED
    }

    /**
     * 绘制
     */
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = startOffset
        // 无歌词
        if (!hasLrc()) {
            lyricPaint.color = mCurrentTextColor
            LyricEntry.createStaticLayout(
                mDefaultLabel,
                lyricPaint,
                lrcWidth,
                Layout.Alignment.ALIGN_CENTER
            )?.let {
                drawText(canvas, it, centerY)
            }
            return
        }
        // 歌词有效
        val centerLine = centerLine
        if (isShowTimeline) {
            playDrawable!!.draw(canvas)
            timePaint.color = mTimelineColor
            canvas.drawLine(
                mTimeTextWidth.toFloat(),
                centerY,
                (width - mTimeTextWidth).toFloat(),
                centerY,
                timePaint
            )
            timePaint.color = mTimeTextColor
            val timeText = formatTime(lyricEntryList[centerLine].time)
            val timeX = width - mTimeTextWidth.toFloat() / 2
            val timeY = centerY - (mTimeFontMetrics!!.descent + mTimeFontMetrics!!.ascent) / 2
            canvas.drawText(timeText, timeX, timeY, timePaint)
        }
        canvas.translate(0f, mOffset)
        var y = 0f
        for (i in lyricEntryList.indices) {
            if (i == mCurrentLine) {
                lyricPaint.textSize = mCurrentTextSize
                lyricPaint.color = mCurrentTextColor
                secondLyricPaint.textSize = mCurrentTextSize * mTranslateTextScaleValue
                secondLyricPaint.color = mCurrentTextColor
            } else if (isShowTimeline && i == centerLine) {
                lyricPaint.color = mTimelineTextColor
                secondLyricPaint.color = mTimelineTextColor
            } else {
                lyricPaint.textSize = mNormalTextSize
                lyricPaint.color = mNormalTextColor
                secondLyricPaint.textSize = mNormalTextSize * mTranslateTextScaleValue
                secondLyricPaint.color = mNormalTextColor
            }
            lyricEntryList[i].staticLayout?.let {
                drawText(canvas, it, y)
                y += it.height
                lyricEntryList[i].secondStaticLayout?.let { second ->
                    y += mTranslateDividerHeight
                    drawText(canvas, second, y)
                    y += second.height
                }
                y += mSentenceDividerHeight
            }
        }
    }

    /**
     * 画一行歌词
     *
     * @param y 歌词中心 Y 坐标
     */
    private fun drawText(canvas: Canvas, staticLayout: StaticLayout, y: Float) {
        canvas.save()
        canvas.translate(mLrcPadding, y)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            isTouching = false
            if (hasLrc() && !isFling) {
                adjustCenter()
                postDelayed(hideTimelineRunnable, TIMELINE_KEEP_TIME)
            }
        }
        return mGestureDetector!!.onTouchEvent(event)
    }

    /**
     * 手势监听器
     */
    private val mSimpleOnGestureListener: SimpleOnGestureListener = object : SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            // 有歌词并且设置了 mOnPlayClickListener
            if (hasLrc() && mOnPlayClickListener != null) {
                mScroller!!.forceFinished(true)
                removeCallbacks(hideTimelineRunnable)
                isTouching = true
                invalidate()
                return true
            }
            return super.onDown(e)
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (hasLrc()) {
                // 滚动显示时间线
                isShowTimeline = true
                mOffset += -distanceY
                mOffset = mOffset.coerceAtMost(getOffset(0))
                mOffset = mOffset.coerceAtLeast(getOffset(lyricEntryList.size - 1))
                invalidate()
                return true
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (hasLrc()) {
                mScroller!!.fling(
                    0,
                    mOffset.toInt(),
                    0,
                    velocityY.toInt(),
                    0,
                    0,
                    getOffset(lyricEntryList.size - 1).toInt(),
                    getOffset(0).toInt()
                )
                isFling = true
                return true
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (hasLrc() && isShowTimeline && playDrawable!!.bounds.contains(e.x.toInt(), e.y.toInt())) {
                val centerLine = centerLine
                val centerLineTime = lyricEntryList[centerLine].time
                // onPlayClick 消费了才更新 UI
                if (mOnPlayClickListener != null && mOnPlayClickListener!!.onPlayClick(centerLineTime)) {
                    isShowTimeline = false
                    removeCallbacks(hideTimelineRunnable)
                    mCurrentLine = centerLine
                    invalidate()
                    return true
                }
            } else {
                if (mOnSingerClickListener != null) {
                    mOnSingerClickListener!!.onClick()
                }
            }
            return super.onSingleTapConfirmed(e)
        }
    }

    private val hideTimelineRunnable = Runnable {
        if (hasLrc() && isShowTimeline) {
            isShowTimeline = false
            smoothScrollTo(mCurrentLine)
        }
    }

    override fun computeScroll() {
        if (mScroller!!.computeScrollOffset()) {
            mOffset = mScroller!!.currY.toFloat()
            invalidate()
        }
        if (isFling && mScroller!!.isFinished) {
            isFling = false
            if (hasLrc() && !isTouching) {
                adjustCenter()
                postDelayed(hideTimelineRunnable, TIMELINE_KEEP_TIME)
            }
        }
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(hideTimelineRunnable)
        super.onDetachedFromWindow()
    }

    private fun onLrcLoaded(entryList: List<LyricEntry>?) {
        if (entryList != null && entryList.isNotEmpty()) {
            lyricEntryList.addAll(entryList)
        }
        lyricEntryList.sort()
        initEntryList()
        invalidate()
    }

    private fun initPlayDrawable() {
        val l = (mTimeTextWidth - mDrawableWidth) / 2
        val t = startOffset.toInt() - mDrawableWidth / 2
        val r = l + mDrawableWidth
        val b = t + mDrawableWidth
        playDrawable!!.setBounds(l, t, r, b)
    }

    private fun initEntryList() {
        if (!hasLrc() || width == 0) {
            return
        }
        /**
         * StaticLayout 根据初始化时传入的TextSize计算换行的位置
         * 如果 [mCurrentTextSize] 与 [mNormalTextSize] 相差较大，
         * 则会导致歌词渲染时溢出边界，或行间距不足挤压在一起
         *
         * 故计算出可能的最大TextSize以后，用其初始化，使StaticLayout拥有足够的高度
         */
        lyricPaint.textSize = max(mCurrentTextSize, mNormalTextSize)
        secondLyricPaint.textSize = lyricPaint.textSize * mTranslateTextScaleValue
        for (lrcEntry in lyricEntryList) {
            lrcEntry.init(
                lyricPaint, secondLyricPaint,
                lrcWidth.toInt(), mTextGravity.toLayoutAlign()
            )
        }
        mOffset = startOffset
    }

    private fun reset() {
        endAnimation()
        mScroller!!.forceFinished(true)
        isShowTimeline = false
        isTouching = false
        isFling = false
        removeCallbacks(hideTimelineRunnable)
        lyricEntryList.clear()
        mOffset = 0f
        mCurrentLine = 0
        invalidate()
    }

    /**
     * 将中心行微调至正中心
     */
    private fun adjustCenter() {
        smoothScrollTo(centerLine, ADJUST_DURATION)
    }

    /**
     * 平滑滚动到某一行
     * @param line 行号
     * @param duration 时长，0 就是马上滚动到
     */
    private fun smoothScrollTo(line: Int, duration: Long = mAnimationDuration) {
        val offset = getOffset(line)
        endAnimation()
        animator = ValueAnimator.ofFloat(mOffset, offset).apply {
            setDuration(duration)
            // Salt Spring 插值器
            interpolator = smoothScrollInterpolator
            addUpdateListener { animation: ValueAnimator ->
                mOffset = animation.animatedValue as Float
                this@LyricViewX.invalidate()
            }
            // resetDurationScale()
            start()
        }
    }

    /**
     * 结束滚动动画
     */
    private fun endAnimation() {
        if (animator != null && animator!!.isRunning) {
            animator!!.end()
        }
    }

    /**
     * 二分法查找当前时间应该显示的行数（最后一个 <= time 的行数）
     */
    private fun findShowLine(time: Long): Int {
        var left = 0
        var right = lyricEntryList.size
        while (left <= right) {
            val middle = (left + right) / 2
            val middleTime = lyricEntryList[middle].time
            if (time < middleTime) {
                right = middle - 1
            } else {
                if (middle + 1 >= lyricEntryList.size || time < lyricEntryList[middle + 1].time) {
                    return middle
                }
                left = middle + 1
            }
        }
        return 0
    }

    /**
     * 获取当前在视图中央的行数
     */
    private val centerLine: Int
        get() {
            var centerLine = 0
            var minDistance = Float.MAX_VALUE
            for (i in lyricEntryList.indices) {
                if (abs(mOffset - getOffset(i)) < minDistance) {
                    minDistance = abs(mOffset - getOffset(i))
                    centerLine = i
                }
            }
            return centerLine
        }

    /**
     * 因为添加了 [mTranslateDividerHeight] 用来间隔开歌词与翻译，
     * 所以直接从 [LyricEntry] 获取高度不可行，
     * 故使用该 [getLyricHeight] 方法来计算 [LyricEntry] 的高度
     */
    open fun getLyricHeight(line: Int): Int {
        var height = lyricEntryList[line].staticLayout?.height ?: return 0
        lyricEntryList[line].secondStaticLayout?.height?.let {
            height += (it + mTranslateDividerHeight).toInt()
        }
        return height
    }

    /**
     * 获取歌词距离视图顶部的距离
     * 采用懒加载方式
     */
    private fun getOffset(line: Int): Float {
        if (lyricEntryList[line].offset == Float.MIN_VALUE) {
            var offset = startOffset
            for (i in 1..line) {
                offset -= (getLyricHeight(i - 1) + getLyricHeight(i) shr 1) + mSentenceDividerHeight
            }
            lyricEntryList[line].offset = offset
        }
        return lyricEntryList[line].offset
    }

    /**
     * 获取歌词宽度
     */
    open val lrcWidth: Float
        get() = width - mLrcPadding * 2

    /**
     * 歌词整体的垂直偏移值
     */
    open val startOffset: Float
        get() = height.toFloat() / 2f + mHorizontalOffset

    /**
     * 在主线程中运行
     */
    private fun runOnMain(r: Runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run()
        } else {
            post(r)
        }
    }

    // 放在最后，不要移动
    init {
        init(attrs)
    }

    /**
     * 以下是公共部分
     * 用法见接口 [LyricViewXInterface]
     */

    override fun setSentenceDividerHeight(height: Float) {
        mSentenceDividerHeight = height
        if (hasLrc()) {
            smoothScrollTo(mCurrentLine, 0L)
        }
        postInvalidate()
    }

    override fun setTranslateDividerHeight(height: Float) {
        mTranslateDividerHeight = height
        if (hasLrc()) {
            smoothScrollTo(mCurrentLine, 0L)
        }
        postInvalidate()
    }

    override fun setHorizontalOffset(offset: Float) {
        mHorizontalOffset = offset
        initPlayDrawable()
        initEntryList()
        postInvalidate()
    }

    override fun setTranslateTextScaleValue(scaleValue: Float) {
        mTranslateTextScaleValue = scaleValue
        initEntryList()
        if (hasLrc()) {
            smoothScrollTo(mCurrentLine, 0L)
        }
    }

    override fun setTextGravity(gravity: Int) {
        mTextGravity = gravity
        initEntryList()
        if (hasLrc()) {
            smoothScrollTo(mCurrentLine, 0L)
        }
    }

    override fun setNormalColor(normalColor: Int) {
        mNormalTextColor = normalColor
        postInvalidate()
    }

    override fun setNormalTextSize(size: Float) {
        mNormalTextSize = size
        initEntryList()
        if (hasLrc()) {
            smoothScrollTo(mCurrentLine, 0L)
        }
    }

    override fun setCurrentTextSize(size: Float) {
        mCurrentTextSize = size
        initEntryList()
        if (hasLrc()) {
            smoothScrollTo(mCurrentLine, 0L)
        }
    }

    override fun setCurrentColor(currentColor: Int) {
        mCurrentTextColor = currentColor
        postInvalidate()
    }

    override fun setTimelineTextColor(timelineTextColor: Int) {
        mTimelineTextColor = timelineTextColor
        postInvalidate()
    }

    override fun setTimelineColor(timelineColor: Int) {
        mTimelineColor = timelineColor
        postInvalidate()
    }

    override fun setTimeTextColor(timeTextColor: Int) {
        mTimeTextColor = timeTextColor
        postInvalidate()
    }

    override fun setLabel(label: String) {
        runOnMain {
            mDefaultLabel = label
            this@LyricViewX.invalidate()
        }
    }

    override fun loadLyric(mainLyricFile: File, secondLyricFile: File?) {
        runOnMain {
            reset()
            val sb = StringBuilder("file://")
            sb.append(mainLyricFile.path)
            if (secondLyricFile != null) {
                sb.append("#").append(secondLyricFile.path)
            }
            val flag = sb.toString()
            this@LyricViewX.flag = flag
            object : AsyncTask<File?, Int?, List<LyricEntry>>() {
                override fun onPostExecute(lrcEntries: List<LyricEntry>?) {
                    if (flag === flag) {
                        onLrcLoaded(lrcEntries)
                        this@LyricViewX.flag = null
                    }
                }

                override fun doInBackground(vararg params: File?): List<LyricEntry>? {
                    return LyricUtil.parseLrc(params)
                }
            }.execute(mainLyricFile, secondLyricFile)
        }
    }

    override fun loadLyric(mainLyricText: String?, secondLyricText: String?) {
        runOnMain {
            reset()
            val sb = StringBuilder("file://")
            sb.append(mainLyricText)
            if (secondLyricText != null) {
                sb.append("#").append(secondLyricText)
            }
            val flag = sb.toString()
            this@LyricViewX.flag = flag
            thread {
                val lrcEntries = LyricUtil.parseLrc(arrayOf(mainLyricText, secondLyricText))
                runOnMain {
                    if (flag === flag) {
                        onLrcLoaded(lrcEntries)
                        this@LyricViewX.flag = null
                    }
                }
            }
        }
    }

    override fun loadLyricByUrl(lyricUrl: String, charset: String?) {
        val flag = "url://$lyricUrl"
        this.flag = flag
        object : AsyncTask<String?, Int?, String?>() {
            override fun onPostExecute(lrcText: String?) {
                if (flag === flag) {
                    loadLyric(lrcText)
                }
            }

            override fun doInBackground(vararg params: String?): String? {
                return getContentFromNetwork(params[0], params[1])
            }
        }.execute(lyricUrl, charset)
    }

    override fun updateTime(time: Long, force: Boolean) {
        // 将方法的执行延后至 View 创建完成后执行
        readyHelper.whenReady {
            if (!it) return@whenReady
            runOnMain {
                if (hasLrc()) {
                    val line = findShowLine(time)
                    if (line != mCurrentLine) {
                        mCurrentLine = line
                        if (!isShowTimeline) {
                            smoothScrollTo(line, if (force) 0L else mAnimationDuration)
                        } else {
                            this@LyricViewX.invalidate()
                        }
                    }
                }
            }
        }
    }

    override fun setDraggable(draggable: Boolean, onPlayClickListener: OnPlayClickListener?) {
        mOnPlayClickListener = if (draggable) {
            requireNotNull(onPlayClickListener) { "if draggable == true, onPlayClickListener must not be null" }
            onPlayClickListener
        } else {
            null
        }
    }

    override fun setOnSingerClickListener(onSingerClickListener: OnSingleClickListener?) {
        this.mOnSingerClickListener = onSingerClickListener
    }

    override fun getLyricEntryList(): List<LyricEntry> {
        return lyricEntryList.toList()
    }

    override fun setLyricEntryList(newList: List<LyricEntry>) {
        reset()
        onLrcLoaded(newList)
        this@LyricViewX.flag = null
    }

    override fun getCurrentLineLyricEntry(): LyricEntry? {
        if (mCurrentLine <= lyricEntryList.lastIndex) {
            return lyricEntryList[mCurrentLine]
        }
        return null
    }

    override fun setLyricTypeface(file: File) {
        val typeface = file.takeIf { it.exists() }
            ?.runCatching { Typeface.createFromFile(this) }
            ?.getOrNull() ?: return

        setLyricTypeface(typeface)
    }

    override fun setLyricTypeface(path: String) {
        setLyricTypeface(File(path))
    }

    override fun setLyricTypeface(typeface: Typeface?) {
        lyricPaint.typeface = typeface
        secondLyricPaint.typeface = typeface

        invalidate()
    }

    companion object {
        // 调整时间
        private const val ADJUST_DURATION: Long = 100

        // 时间线持续时间
        private const val TIMELINE_KEEP_TIME = 3 * DateUtils.SECOND_IN_MILLIS
    }
}