/**
 * LyricViewX Copyright (C) 2020-2022 Moriafly
 *
 * This program comes with ABSOLUTELY NO WARRANTY; for details type `show w'.
 * This is free software, and you are welcome to redistribute it
 * under certain conditions; type `show c' for details.
 *
 * The hypothetical commands `show w' and `show c' should show the appropriate
 * parts of the General Public License.  Of course, your program's commands
 * might be different; for a GUI interface, you would use an "about box".
 *
 * You should also get your employer (if you work as a programmer) or school,
 * if any, to sign a "copyright disclaimer" for the program, if necessary.
 * For more information on this, and how to apply and follow the GNU GPL, see
 * <https://www.gnu.org/licenses/>.
 *
 * The GNU General Public License does not permit incorporating your program
 * into proprietary programs.  If your program is a subroutine library, you
 * may consider it more useful to permit linking proprietary applications with
 * the library.  If this is what you want to do, use the GNU Lesser General
 * Public License instead of this License.  But first, please read
 * <https://www.gnu.org/licenses/why-not-lgpl.html>.
 */

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.dirror.lyricviewx

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.widget.Scroller
import androidx.annotation.FloatRange
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.SpringForce
import androidx.dynamicanimation.animation.springAnimationOf
import androidx.dynamicanimation.animation.withSpringForceProperties
import com.dirror.lyricviewx.LyricUtil.calcScaleValue
import com.dirror.lyricviewx.LyricUtil.formatTime
import com.dirror.lyricviewx.LyricUtil.insideOf
import com.dirror.lyricviewx.LyricUtil.lerp
import com.dirror.lyricviewx.LyricUtil.lerpColor
import com.dirror.lyricviewx.LyricUtil.normalize
import com.dirror.lyricviewx.extension.BlurMaskFilterExt
import com.lalilu.easeview.EaseView
import com.lalilu.easeview.animatevalue.BoolValue
import com.lalilu.easeview.animatevalue.FloatListAnimateValue
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max

/**
 * LyricViewX
 *
 * Based on https://github.com/zion223/NeteaseCloudMusic-MVVM Kotlin
 *
 * Thanks:
 * https://github.com/cy745
 */
open class LyricViewX : EaseView, LyricViewXInterface {
    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    protected val readyHelper = ReadyHelper()
    private val blurMaskFilterExt = BlurMaskFilterExt()

    /** 单句歌词集合 */
    private val lyricEntryList: MutableList<LyricEntry> = ArrayList()

    /** 主歌词画笔 */
    private val lyricPaint = TextPaint()

    /** 副歌词（一般为翻译歌词）画笔 */
    private val secondLyricPaint = TextPaint()

    /** 时间文字画笔 */
    private val timePaint = TextPaint()

    private var timeFontMetrics: Paint.FontMetrics? = null

    /** 跳转播放按钮 */
    private var playDrawable: Drawable? = null

    private var translateDividerHeight = 0f
    private var sentenceDividerHeight = 0f
    private var animationDuration: Long = 0
    private var normalTextColor = 0
    private var normalTextSize = 0f
    private var currentTextColor = 0
    private var currentTextSize = 0f
    private var translateTextScaleValue = 1f
    private var timelineTextColor = 0
    private var timelineColor = 0
    private var timeTextColor = 0
    private var drawableWidth = 0
    private var timeTextWidth = 0
    private var defaultLabel: String? = null
    private var lrcPadding = 0f
    private var onPlayClickListener: OnPlayClickListener? = null
    private var onSingerClickListener: OnSingleClickListener? = null
    private var animator: ValueAnimator? = null
    private var gestureDetector: GestureDetector? = null
    private var scroller: Scroller? = null
    private var flag: Any? = null
    private var isTouching = false
    private var isFling = false
    private var textGravity = GRAVITY_CENTER // 歌词显示位置，靠左 / 居中 / 靠右
    private var horizontalOffset: Float = 0f
    private var horizontalOffsetPercent: Float = 0.5f
    private var itemOffsetPercent: Float = 0.5f
    private var dampingRatioForLyric: Float = SpringForce.DAMPING_RATIO_LOW_BOUNCY
    private var dampingRatioForViewPort: Float = SpringForce.DAMPING_RATIO_NO_BOUNCY
    private var stiffnessForLyric: Float = SpringForce.STIFFNESS_LOW
    private var stiffnessForViewPort: Float = SpringForce.STIFFNESS_VERY_LOW

    private var currentLine = 0            // 当前高亮显示的歌词
    private val focusLine: Int             // 当前焦点歌词
        get() = if (isTouching || isFling) centerLine else currentLine

    /**
     * 获取当前在视图中央的行数
     */
    private val centerLine: Int
        get() {
            var centerLine = 0
            var minDistance = Float.MAX_VALUE
            var tempDistance: Float

            for (i in lyricEntryList.indices) {
                tempDistance = abs(mViewPortOffset - getOffset(i))
                if (tempDistance < minDistance) {
                    minDistance = tempDistance
                    centerLine = i
                }
            }
            return centerLine
        }

    /**
     * 获取歌词宽度
     */
    open val lrcWidth: Float
        get() = width - lrcPadding * 2

    /**
     * 歌词整体的垂直偏移值
     */
    open val startOffset: Float
        get() = height.toFloat() * horizontalOffsetPercent + horizontalOffset


    /**
     * 原有的mOffset被拆分成两个独立的offset，这样可以更好地让进度和拖拽滚动独立开来
     */
    private var mCurrentOffset = 0f             // 实际的歌词进度Offset
    private var mViewPortOffset = 0f            // 歌词显示窗口的Offset

    private var animateProgress = 0f            // 动画进度
    private var animateTargetOffset = 0f        // 动画目标Offset
    private var animateStartOffset = 0f         // 动画起始Offset

    private val viewPortSpringAnimator = springAnimationOf(
        getter = { mViewPortOffset },
        setter = { value ->
            if (!isShowTimeline.value && !isTouching && !isFling) {
                mViewPortOffset = value
                invalidate()
            }
        }
    ).withSpringForceProperties {
        dampingRatio = dampingRatioForViewPort
        stiffness = stiffnessForViewPort
        finalPosition = 0f
    }

    /**
     * 弹性动画Scroller
     */
    private val progressSpringAnimator = springAnimationOf(
        getter = { mCurrentOffset },
        setter = { value ->
            animateProgress = normalize(animateStartOffset, animateTargetOffset, value)
            mCurrentOffset = value

            if (!isShowTimeline.value && !isTouching && !isFling) {
                viewPortSpringAnimator.animateToFinalPosition(animateTargetOffset)
            }
            invalidate()
        }
    ).withSpringForceProperties {
        dampingRatio = dampingRatioForLyric
        stiffness = stiffnessForLyric
        finalPosition = 0f
    }

    @SuppressLint("CustomViewStyleable")
    private fun init(attrs: AttributeSet?) {
        readyHelper.readyState = STATE_INITIALIZING
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.LyricView)
        currentTextSize = typedArray.getDimension(R.styleable.LyricView_lrcTextSize, resources.getDimension(R.dimen.lrc_text_size))
        normalTextSize = typedArray.getDimension(R.styleable.LyricView_lrcNormalTextSize, resources.getDimension(R.dimen.lrc_text_size))
        if (normalTextSize == 0f) {
            normalTextSize = currentTextSize
        }

        sentenceDividerHeight =
            typedArray.getDimension(R.styleable.LyricView_lrcSentenceDividerHeight, resources.getDimension(R.dimen.lrc_sentence_divider_height))
        translateDividerHeight =
            typedArray.getDimension(R.styleable.LyricView_lrcTranslateDividerHeight, resources.getDimension(R.dimen.lrc_translate_divider_height))
        val defDuration = resources.getInteger(R.integer.lrc_animation_duration)
        animationDuration = typedArray.getInt(R.styleable.LyricView_lrcAnimationDuration, defDuration).toLong()
        animationDuration =
            if (animationDuration < 0) defDuration.toLong() else animationDuration

        normalTextColor = typedArray.getColor(
            R.styleable.LyricView_lrcNormalTextColor,
            ContextCompat.getColor(context, R.color.lrc_normal_text_color)
        )
        currentTextColor = typedArray.getColor(
            R.styleable.LyricView_lrcCurrentTextColor,
            ContextCompat.getColor(context, R.color.lrc_current_text_color)
        )
        timelineTextColor = typedArray.getColor(
            R.styleable.LyricView_lrcTimelineTextColor,
            ContextCompat.getColor(context, R.color.lrc_timeline_text_color)
        )
        defaultLabel = typedArray.getString(R.styleable.LyricView_lrcLabel)
        defaultLabel = if (defaultLabel.isNullOrEmpty()) "暂无歌词" else defaultLabel
        lrcPadding = typedArray.getDimension(R.styleable.LyricView_lrcPadding, 0f)
        timelineColor = typedArray.getColor(
            R.styleable.LyricView_lrcTimelineColor,
            ContextCompat.getColor(context, R.color.lrc_timeline_color)
        )
        val timelineHeight = typedArray.getDimension(
            R.styleable.LyricView_lrcTimelineHeight,
            resources.getDimension(R.dimen.lrc_timeline_height)
        )
        playDrawable = typedArray.getDrawable(R.styleable.LyricView_lrcPlayDrawable)
        playDrawable = if (playDrawable == null) ContextCompat.getDrawable(
            context,
            R.drawable.lrc_play
        ) else playDrawable
        timeTextColor = typedArray.getColor(
            R.styleable.LyricView_lrcTimeTextColor,
            ContextCompat.getColor(context, R.color.lrc_time_text_color)
        )
        val timeTextSize = typedArray.getDimension(
            R.styleable.LyricView_lrcTimeTextSize,
            resources.getDimension(R.dimen.lrc_time_text_size)
        )
        textGravity = typedArray.getInteger(R.styleable.LyricView_lrcTextGravity, GRAVITY_CENTER)
        translateTextScaleValue = typedArray.getFloat(R.styleable.LyricView_lrcTranslateTextScaleValue, 1f)
        horizontalOffset = typedArray.getDimension(R.styleable.LyricView_lrcHorizontalOffset, 0f)
        horizontalOffsetPercent = typedArray.getDimension(R.styleable.LyricView_lrcHorizontalOffsetPercent, 0.5f)
        itemOffsetPercent = typedArray.getDimension(R.styleable.LyricView_lrcItemOffsetPercent, 0.5f)
        isDrawTranslation = typedArray.getBoolean(R.styleable.LyricView_lrcIsDrawTranslation, false)
        typedArray.recycle()
        drawableWidth = resources.getDimension(R.dimen.lrc_drawable_width).toInt()
        timeTextWidth = resources.getDimension(R.dimen.lrc_time_width).toInt()
        lyricPaint.isAntiAlias = true
        lyricPaint.textSize = currentTextSize
        lyricPaint.textAlign = Paint.Align.LEFT
//        lyricPaint.setShadowLayer(0.1f, 0f, 1f, Color.DKGRAY)
        secondLyricPaint.isAntiAlias = true
        secondLyricPaint.textSize = currentTextSize
        secondLyricPaint.textAlign = Paint.Align.LEFT
//        secondLyricPaint.setShadowLayer(0.1f, 0f, 1f, Color.DKGRAY)
        timePaint.isAntiAlias = true
        timePaint.textSize = timeTextSize
        timePaint.textAlign = Paint.Align.CENTER
        timePaint.strokeWidth = timelineHeight
        timePaint.strokeCap = Paint.Cap.ROUND
        timeFontMetrics = timePaint.fontMetrics
        gestureDetector = GestureDetector(context, mSimpleOnGestureListener)
        gestureDetector!!.setIsLongpressEnabled(false)
        scroller = Scroller(context)
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
                smoothScrollTo(currentLine)
            }
        }
        readyHelper.readyState = STATE_INITIALIZED
    }

    private val isShowTimeline = BoolValue().also(::registerValue)
    private val isEnableBlurEffect = BoolValue().also(::registerValue)
    private val progressKeeper = FloatListAnimateValue().also(::registerValue)
    private val blurProgressKeeper = FloatListAnimateValue().also(::registerValue)

    private val heightKeeper = LinkedHashMap<Int, Float>()
    private val offsetKeeper = LinkedHashMap<Int, Float>()
    private val minOffsetKeeper = LinkedHashMap<Int, Float>()
    private val maxOffsetKeeper = LinkedHashMap<Int, Float>()

    private var viewPortStartOffset: Float = 0f
    private var isDrawTranslationValue = 0f
    private var isDrawTranslation: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            viewPortStartOffset = mViewPortOffset
            isDrawTranslationAnimator.animateToFinalPosition(if (value) 1000f else 0f)
        }
    private val isDrawTranslationAnimator = springAnimationOf(
        getter = { isDrawTranslationValue * 1000f },
        setter = {
            isDrawTranslationValue = it / 1000f

            if (!isTouching && !isFling) {
                viewPortSpringAnimator.cancel()

                val targetOffset = if (isDrawTranslation) getMaxOffset(focusLine) else getMinOffset(focusLine)
                val animateValue = if (isDrawTranslation) isDrawTranslationValue else 1f - isDrawTranslationValue

                mViewPortOffset = lerp(viewPortStartOffset, targetOffset, animateValue)
            }
            invalidate()
        },
    ).withSpringForceProperties {
        dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
        stiffness = SpringForce.STIFFNESS_LOW
        finalPosition = if (isDrawTranslation) 1000f else 0f
    }

    override fun onPreDraw(canvas: Canvas): Boolean {
        // 无歌词，只渲染一句无歌词的提示语句
        if (!hasLrc()) {
            lyricPaint.color = currentTextColor
            lyricPaint.textSize = normalTextSize
            LyricEntry.createStaticLayout(
                defaultLabel,
                lyricPaint,
                lrcWidth,
                Layout.Alignment.ALIGN_CENTER
            )?.let {
                drawText(
                    canvas = canvas,
                    staticLayout = it,
                    calcHeightOnly = false,
                    yOffset = startOffset,
                    yClipPercentage = 1f
                )
            }
            return false
        }
        return super.onPreDraw(canvas)
    }

    override fun onDoDraw(canvas: Canvas): Boolean {
        val centerY = startOffset
        val currentCenterLine = centerLine

        // 当显示时间线时，需要绘制时间线
        if (isShowTimeline.value || isShowTimeline.animateValue > 0f) {
            val alpha = (isShowTimeline.animateValue * 255f).toInt()

            // 绘制播放按钮
            playDrawable?.let {
                it.alpha = alpha
                it.draw(canvas)
            }

            // 绘制时间线
            timePaint.color = timelineColor
            timePaint.alpha = alpha
            canvas.drawLine(
                timeTextWidth.toFloat(), centerY,
                (width - timeTextWidth).toFloat(), centerY, timePaint
            )

            // 绘制当前时间
            val timeText = formatTime(lyricEntryList[currentCenterLine].time)
            val timeX = width - timeTextWidth.toFloat() / 2
            val timeY = centerY - (timeFontMetrics!!.descent + timeFontMetrics!!.ascent) / 2
            timePaint.color = timeTextColor
            timePaint.alpha = alpha
            canvas.drawText(timeText, timeX, timeY, timePaint)
        }

        canvas.translate(0f, mViewPortOffset)

        var yOffset = 0f
        var yMinOffset = 0f
        var yMaxOffset = 0f
        var scaleValue: Float
        var progress: Float
        var radius: Int
        var calcHeightOnly: Boolean

        for (i in lyricEntryList.indices) {
            // 根据上一项所计算得到的offset值，判断当前元素是否在需要绘制的区间，如果不在，则只需要计算高度不进行绘制相关计算
            calcHeightOnly = getOffset(i - 1) !in (mViewPortOffset - height)..(mViewPortOffset + height)
            progressKeeper.updateTargetValue(i, if (currentLine == i) animateProgress else 0f)
            progress = progressKeeper.getValueByIndex(i)
            scaleValue = 1f
            radius = 0

            if (!calcHeightOnly) {
                when {
                    // 当前行动画未结束
                    progress > 0f -> {
                        scaleValue = calcScaleValue(currentTextSize, normalTextSize, progress)
                        lyricPaint.color = lerpColor(normalTextColor, currentTextColor, progress.coerceIn(0f, 1f))
                    }

                    isShowTimeline.value && i == currentCenterLine -> {
                        lyricPaint.color = timelineTextColor
                    }

                    else -> {
                        lyricPaint.color = normalTextColor
                    }
                }
                lyricPaint.textSize = normalTextSize
                secondLyricPaint.textSize = lyricPaint.textSize * translateTextScaleValue
                secondLyricPaint.color = lyricPaint.color

                if (isEnableBlurEffect.value || isEnableBlurEffect.animateValue > 0f) {
                    radius = when (i) {
                        currentCenterLine -> 0
                        currentCenterLine + 1 -> 3
                        currentCenterLine + 2, currentCenterLine - 1 -> 7
                        currentCenterLine + 3, currentCenterLine - 2 -> 11
                        currentCenterLine + 4, currentCenterLine - 3 -> 20
                        else -> 20
                    }
                    blurProgressKeeper.updateTargetValue(i, radius.toFloat())
                    radius = blurProgressKeeper.getValueByIndex(i).toInt()
                    radius = (radius * isEnableBlurEffect.animateValue).toInt()
                }
            }

            val itemHeight = drawLyricEntry(
                canvas = canvas,
                entry = lyricEntryList[i],
                calcHeightOnly = calcHeightOnly,
                yOffset = yOffset,
                scaleValue = scaleValue,
                blurRadius = radius,
            ) { minHeight, maxHeight ->
                minOffsetKeeper[i] = yMinOffset + calcOffsetOfItem(minHeight, sentenceDividerHeight)
                yMinOffset += minHeight

                maxOffsetKeeper[i] = yMaxOffset + calcOffsetOfItem(maxHeight, sentenceDividerHeight)
                yMaxOffset += maxHeight
            }
            heightKeeper[i] = itemHeight
            offsetKeeper[i] = yOffset + calcOffsetOfItem(itemHeight, sentenceDividerHeight)
            yOffset += itemHeight
        }
        return super.onDoDraw(canvas)
    }

    /**
     * 画一组歌词语句
     *
     * @param calcHeightOnly    是否只计算高度
     * @param yOffset           歌词中心 Y 坐标
     * @param scaleValue        缩放比例
     * @param blurRadius        模糊半径
     *
     * @return 该组歌词的实际绘制高度
     */
    private fun drawLyricEntry(
        canvas: Canvas,
        entry: LyricEntry,
        calcHeightOnly: Boolean,
        yOffset: Float,
        scaleValue: Float,
        blurRadius: Int,
        callback: (minHeight: Float, maxHeight: Float) -> Unit = { _, _ -> }
    ): Float {
        var tempHeight = 0f
        var minTempHeight = 0f
        var maxTempHeight = 0f

        entry.staticLayout?.let {
            tempHeight += drawText(
                canvas = canvas,
                staticLayout = it,
                calcHeightOnly = calcHeightOnly,
                yOffset = yOffset,
                yClipPercentage = 1f,
                scale = scaleValue,
                blurRadius = blurRadius
            )
            minTempHeight = tempHeight
            maxTempHeight = tempHeight

            entry.secondStaticLayout?.let { second ->
                tempHeight += translateDividerHeight * isDrawTranslationValue
                maxTempHeight += translateDividerHeight

                tempHeight += drawText(
                    canvas = canvas,
                    staticLayout = second,
                    calcHeightOnly = calcHeightOnly,
                    yOffset = yOffset + tempHeight,
                    yClipPercentage = isDrawTranslationValue,
                    alpha = isDrawTranslationValue,
                    scale = scaleValue,
                    blurRadius = blurRadius
                ) { _, max ->
                    maxTempHeight += max
                }
            }
            tempHeight += sentenceDividerHeight
            minTempHeight += sentenceDividerHeight
            maxTempHeight += sentenceDividerHeight
        }
        callback(minTempHeight, maxTempHeight)
        return tempHeight
    }

    /**
     * 画一行歌词
     *
     * @param calcHeightOnly    是否只计算高度
     * @param yOffset           歌词中心 Y 坐标
     * @param yClipPercentage   垂直裁剪比例
     * @param scale             缩放比例
     * @param alpha             透明度
     * @param blurRadius        模糊半径 实现类似AppleMusic的歌词语句的模糊效果
     *
     * @return 实际绘制高度
     */
    private fun drawText(
        canvas: Canvas,
        staticLayout: StaticLayout,
        calcHeightOnly: Boolean = false,
        yOffset: Float,
        @FloatRange(from = 0.0, to = 1.0)
        yClipPercentage: Float = 1f,
        scale: Float = 1f,
        alpha: Float = 1f,
        blurRadius: Int = 0,
        callback: (minHeight: Float, maxHeight: Float) -> Unit = { _, _ -> }
    ): Float {
        if (staticLayout.lineCount == 0) {
            callback(0f, 0f)
            return 0f
        }
        if (calcHeightOnly) {
            callback(0f, staticLayout.height.toFloat())
            return staticLayout.height * yClipPercentage
        }
        val lineHeight = staticLayout.height.toFloat() / staticLayout.lineCount.toFloat()

        var yTemp = 0f                  // y轴临时偏移量
        var pivotYTemp: Float           // 缩放中心Y坐标
        var itemActualHeight: Float     // 单行实际绘制高度
        var actualHeight = 0f           // 实际绘制高度

        staticLayout.paint.alpha = (alpha * 255f).toInt()
        staticLayout.paint.maskFilter = blurMaskFilterExt.get(blurRadius)

        /**
         * 由于对StaticLayout整个缩放会使其中间的行间距也被缩放(通过TextPaint的textSize缩放则不会)，
         * 导致其真实渲染高度大于StaticLayout的height属性的值，同时也没有其他的接口能实现相同的缩放效果(对TextSize缩放会显得卡卡的)
         *
         * 所以通过Canvas的clipRect，来分别对StaticLayout的每一行文字进行缩放和绘制(StaticLayout的各行高度是一致的)
         */
        repeat(staticLayout.lineCount) {
            itemActualHeight = lineHeight * yClipPercentage
            pivotYTemp = yTemp + itemActualHeight - staticLayout.paint.descent()  // TextPaint修改textSize所实现的缩放效果应该就是descent线上的缩放(感觉效果差不多)

            canvas.save()
            canvas.translate(lrcPadding, yOffset)
            canvas.clipRect(-lrcPadding, yTemp, staticLayout.width.toFloat() + lrcPadding, yTemp + itemActualHeight)

            // 根据文字的gravity设置缩放基点坐标
            when (textGravity) {
                GRAVITY_LEFT -> canvas.scale(scale, scale, 0f, pivotYTemp)
                GRAVITY_RIGHT -> {
                    canvas.scale(scale, scale, staticLayout.width.toFloat(), pivotYTemp)
                }

                GRAVITY_CENTER -> {
                    canvas.scale(scale, scale, staticLayout.width / 2f, pivotYTemp)
                }
            }
            staticLayout.draw(canvas)
            canvas.restore()
            yTemp += itemActualHeight
            actualHeight += itemActualHeight
        }
        callback(0f, staticLayout.height.toFloat())
        return actualHeight
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            isTouching = false
            if (hasLrc() && !isFling) {
                // TODO 应该为Timeline独立设置一个Enable开关, 这样就可以不需要等待Timeline消失
                postDelayed(hideTimelineRunnable, TIMELINE_KEEP_TIME)
            }
        }
        return gestureDetector!!.onTouchEvent(event)
    }

    /**
     * 手势监听器
     */
    private val mSimpleOnGestureListener: SimpleOnGestureListener =
        object : SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent): Boolean {
                // 有歌词并且设置了 mOnPlayClickListener
                if (hasLrc() && onPlayClickListener != null) {
                    scroller!!.forceFinished(true)
                    removeCallbacks(hideTimelineRunnable)
                    isTouching = true
                    invalidate()
                    return true
                }
                return super.onDown(e)
            }

            override fun onScroll(
                e1: MotionEvent,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (hasLrc()) {
                    // 如果没显示 Timeline 的时候，distanceY 一段距离后再显示时间线
                    if (!isShowTimeline.value && abs(distanceY) >= 10) {
                        // 滚动显示时间线
                        isShowTimeline.value = true
                    }
                    mViewPortOffset += -distanceY
                    mViewPortOffset.coerceIn(getOffset(lyricEntryList.size - 1), getOffset(0))
                    invalidate()
                    return true
                }
                return super.onScroll(e1, e2, distanceX, distanceY)
            }

            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (hasLrc()) {
                    scroller!!.fling(
                        0, mViewPortOffset.toInt(), 0,
                        velocityY.toInt(), 0, 0,
                        getOffset(lyricEntryList.size - 1).toInt(),
                        getOffset(0).toInt()
                    )
                    isFling = true
                    return true
                }
                return super.onFling(e1, e2, velocityX, velocityY)
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!hasLrc() || !isShowTimeline.value || !e.insideOf(playDrawable?.bounds)) {
                    onSingerClickListener?.onClick()
                    return super.onSingleTapConfirmed(e)
                }

                val centerLine = centerLine
                val centerLineTime = lyricEntryList[centerLine].time
                // onPlayClick 消费了才更新 UI
                if (onPlayClickListener?.onPlayClick(centerLineTime) == true) {
                    isShowTimeline.value = false
                    removeCallbacks(hideTimelineRunnable)
                    smoothScrollTo(centerLine)
                    invalidate()
                    return true
                }
                return super.onSingleTapConfirmed(e)
            }
        }

    private val hideTimelineRunnable = Runnable {
        if (hasLrc() && isShowTimeline.value) {
            isShowTimeline.value = false
            smoothScrollTo(currentLine)
        }
    }

    override fun computeScroll() {
        if (scroller!!.computeScrollOffset()) {
            mViewPortOffset = scroller!!.currY.toFloat()
            invalidate()
        }
        if (isFling && scroller!!.isFinished) {
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
        if (!entryList.isNullOrEmpty()) {
            lyricEntryList.addAll(entryList)
        }
        lyricEntryList.sort()
        initEntryList()
        invalidate()
    }

    private fun initPlayDrawable() {
        val l = (timeTextWidth - drawableWidth) / 2
        val t = startOffset.toInt() - drawableWidth / 2
        val r = l + drawableWidth
        val b = t + drawableWidth
        playDrawable!!.setBounds(l, t, r, b)
    }

    private fun initEntryList() {
        if (!hasLrc() || width == 0) {
            return
        }
        /**
         * StaticLayout 根据初始化时传入的 TextSize 计算换行的位置
         * 如果 [currentTextSize] 与 [normalTextSize] 相差较大，
         * 则会导致歌词渲染时溢出边界，或行间距不足挤压在一起
         *
         * 故计算出可能的最大 TextSize 以后，用其初始化，使 StaticLayout 拥有足够的高度
         */
        lyricPaint.textSize = max(currentTextSize, normalTextSize)
        secondLyricPaint.textSize = lyricPaint.textSize * translateTextScaleValue
        for (lrcEntry in lyricEntryList) {
            lrcEntry.init(
                lyricPaint, secondLyricPaint,
                lrcWidth.toInt(), textGravity.toLayoutAlign()
            )
        }
        mCurrentOffset = startOffset
        mViewPortOffset = startOffset
    }

    private fun reset() {
        // TODO 待完善reset的逻辑
        scroller!!.forceFinished(true)
        isShowTimeline.value = false
        isTouching = false
        isFling = false
        removeCallbacks(hideTimelineRunnable)
        lyricEntryList.clear()
        mCurrentOffset = 0f
        mViewPortOffset = 0f
        currentLine = 0
        invalidate()
    }

    /**
     * 将中心行微调至正中心
     */
    private fun adjustCenter() {
        smoothScrollTo(currentLine)
    }

    /**
     * 平滑滚动过渡到某一行
     *
     * @param line 行号
     */
    private fun smoothScrollTo(line: Int) {
        val offset = getOffset(line)
        animateStartOffset = mCurrentOffset
        animateTargetOffset = offset
        progressSpringAnimator.animateToFinalPosition(offset)
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
     * 计算单个歌词元素的偏移量，用于控制歌词对其中线的位置
     *
     * 计算出来的歌词高度包含了分割线的高度，所以需要减去分割线的高度
     *
     * @param itemHeight        歌词元素的高度
     * @param dividerHeight     分割线的高度
     *
     * @return 歌词元素的偏移量
     */
    protected open fun calcOffsetOfItem(itemHeight: Float, dividerHeight: Float): Float {
        return (itemHeight - dividerHeight) * itemOffsetPercent
    }

    /**
     * 因为添加了 [translateDividerHeight] 用来间隔开歌词与翻译，
     * 所以直接从 [LyricEntry] 获取高度不可行，
     * 故使用该 [getLyricHeight] 方法来计算 [LyricEntry] 的高度
     */
    @Deprecated("不再单独计算歌词的高度，在绘制时计算并进行更新缓存，所见即所得")
    open fun getLyricHeight(line: Int): Int {
        var height = lyricEntryList[line].staticLayout?.height ?: return 0
        lyricEntryList[line].secondStaticLayout?.height?.let {
            height += (it + translateDividerHeight).toInt()
        }
        return height
    }

    /**
     * 获取歌词距离视图顶部的距离
     */
    private fun getOffset(line: Int): Float {
        return startOffset - (offsetKeeper[line] ?: 0f)
    }

    private fun getMinOffset(line: Int): Float {
        return startOffset - (minOffsetKeeper[line] ?: 0f)
    }

    private fun getMaxOffset(line: Int): Float {
        return startOffset - (maxOffsetKeeper[line] ?: 0f)
    }

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

    /**
     * 以下是公共部分
     * 用法见接口 [LyricViewXInterface]
     */

    override fun setSentenceDividerHeight(height: Float) {
        sentenceDividerHeight = height
        if (hasLrc()) {
            smoothScrollTo(currentLine)
        }
        postInvalidate()
    }

    override fun setTranslateDividerHeight(height: Float) {
        translateDividerHeight = height
        if (hasLrc()) {
            smoothScrollTo(currentLine)
        }
        postInvalidate()
    }

    override fun setHorizontalOffset(offset: Float) {
        horizontalOffset = offset
        initPlayDrawable()
        postInvalidate()
    }

    override fun setHorizontalOffsetPercent(percent: Float) {
        horizontalOffsetPercent = percent
        initPlayDrawable()
        postInvalidate()
    }

    override fun setTranslateTextScaleValue(scaleValue: Float) {
        translateTextScaleValue = scaleValue
        initEntryList()
        if (hasLrc()) {
            smoothScrollTo(currentLine)
        }
    }

    override fun setTextGravity(gravity: Int) {
        textGravity = gravity
        initEntryList()
        if (hasLrc()) {
            smoothScrollTo(currentLine)
        }
    }

    override fun setNormalColor(normalColor: Int) {
        normalTextColor = normalColor
        postInvalidate()
    }

    override fun setNormalTextSize(size: Float) {
        normalTextSize = size
        initEntryList()
        if (hasLrc()) {
            smoothScrollTo(currentLine)
        }
    }

    override fun setCurrentTextSize(size: Float) {
        currentTextSize = size
        initEntryList()
        if (hasLrc()) {
            smoothScrollTo(currentLine)
        }
    }

    override fun setCurrentColor(currentColor: Int) {
        currentTextColor = currentColor
        postInvalidate()
    }

    override fun setTimelineTextColor(timelineTextColor: Int) {
        this.timelineTextColor = timelineTextColor
        postInvalidate()
    }

    override fun setTimelineColor(timelineColor: Int) {
        this.timelineColor = timelineColor
        postInvalidate()
    }

    override fun setTimeTextColor(timeTextColor: Int) {
        this.timeTextColor = timeTextColor
        postInvalidate()
    }

    override fun setLabel(label: String) {
        runOnMain {
            defaultLabel = label
            this@LyricViewX.invalidate()
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

    override fun loadLyric(lyricEntries: List<LyricEntry>) {
        runOnMain {
            reset()
            onLrcLoaded(lyricEntries)
        }
    }

    override fun updateTime(time: Long, force: Boolean) {
        // 将方法的执行延后至 View 创建完成后执行
        readyHelper.whenReady {
            if (!it) return@whenReady
            if (hasLrc()) {
                val line = findShowLine(time)
                if (line != currentLine) {
                    runOnMain {
                        currentLine = line
                        smoothScrollTo(line)
                    }
                }
            }
        }
    }

    override fun setDraggable(draggable: Boolean, onPlayClickListener: OnPlayClickListener?) {
        this.onPlayClickListener = if (draggable) {
            requireNotNull(onPlayClickListener) { "if draggable == true, onPlayClickListener must not be null" }
            onPlayClickListener
        } else {
            null
        }
    }

    override fun setOnSingerClickListener(onSingerClickListener: OnSingleClickListener?) {
        this.onSingerClickListener = onSingerClickListener
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
        if (currentLine <= lyricEntryList.lastIndex) {
            return lyricEntryList[currentLine]
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
        postInvalidate()
    }

    override fun setDampingRatioForLyric(dampingRatio: Float) {
        dampingRatioForLyric = dampingRatio
        progressSpringAnimator.spring.dampingRatio = dampingRatio
    }

    override fun setDampingRatioForViewPort(dampingRatio: Float) {
        dampingRatioForViewPort = dampingRatio
        viewPortSpringAnimator.spring.dampingRatio = dampingRatio
    }

    override fun setStiffnessForLyric(stiffness: Float) {
        stiffnessForLyric = stiffness
        progressSpringAnimator.spring.stiffness = stiffness
    }

    override fun setStiffnessForViewPort(stiffness: Float) {
        stiffnessForViewPort = stiffness
        viewPortSpringAnimator.spring.stiffness = stiffness
    }

    override fun setPlayDrawable(drawable: Drawable) {
        playDrawable = drawable
    }

    override fun setIsDrawTranslation(isDrawTranslation: Boolean) {
        this.isDrawTranslation = isDrawTranslation
        postInvalidate()
    }

    override fun setIsEnableBlurEffect(isEnableBlurEffect: Boolean) {
        this.isEnableBlurEffect.value = isEnableBlurEffect
        postInvalidate()
    }

    override fun setItemOffsetPercent(itemOffsetPercent: Float) {
        this.itemOffsetPercent = itemOffsetPercent
        postInvalidate()
    }

    companion object {

        private const val TAG = "LyricViewX"

        // 时间线持续时间
        private const val TIMELINE_KEEP_TIME = 3 * DateUtils.SECOND_IN_MILLIS
    }
}