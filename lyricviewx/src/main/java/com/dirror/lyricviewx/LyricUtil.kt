package com.dirror.lyricviewx

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Rect
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.MotionEvent
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Pattern

/**
 * 工具类
 * 原 LrcUtils 转 Kotlin
 */
object LyricUtil {

    private val PATTERN_LINE = Pattern.compile("((\\[\\d\\d:\\d\\d\\.\\d{2,3}])+)(.+)")
    private val PATTERN_TIME = Pattern.compile("\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})]")
    private val argbEvaluator = ArgbEvaluator()

    /**
     * 从文件解析双语歌词
     */
    fun parseLrc(lrcFiles: Array<out File?>?): List<LyricEntry>? {
        if (lrcFiles == null || lrcFiles.size != 2 || lrcFiles[0] == null) {
            return null
        }
        val mainLrcFile = lrcFiles[0]
        val secondLrcFile = lrcFiles[1]
        val mainEntryList = parseLrc(mainLrcFile)
        val secondEntryList = parseLrc(secondLrcFile)
        if (mainEntryList != null && secondEntryList != null) {
            for (mainEntry in mainEntryList) {
                for (secondEntry in secondEntryList) {
                    if (mainEntry.time == secondEntry.time) {
                        mainEntry.secondText = secondEntry.text
                    }
                }
            }
        }
        return mainEntryList
    }

    /**
     * 从文件解析歌词
     */
    private fun parseLrc(lrcFile: File?): List<LyricEntry>? {
        if (lrcFile == null || !lrcFile.exists()) {
            return null
        }
        val entryList: MutableList<LyricEntry> = ArrayList()
        try {
            val br =
                BufferedReader(InputStreamReader(FileInputStream(lrcFile), StandardCharsets.UTF_8))
            var line: String
            while (br.readLine().also { line = it } != null) {
                val list = parseLine(line)
                if (list != null && list.isNotEmpty()) {
                    entryList.addAll(list)
                }
            }
            br.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        entryList.sort()
        return entryList
    }

    /**
     * 从文本解析双语歌词
     */
    fun parseLrc(lrcTexts: Array<out String?>?): List<LyricEntry>? {
        if (lrcTexts == null || lrcTexts.size != 2 || TextUtils.isEmpty(lrcTexts[0])) {
            return null
        }
        val mainLrcText = lrcTexts[0]
        val secondLrcText = lrcTexts[1]
        val mainEntryList = mainLrcText?.let { parseLrc(it) }

        /**
         * 当输入的secondLrcText为空时,按如下格式解析歌词
         * （音乐标签下载的第二种歌词格式）
         *
         *  [00:21.11]いつも待ち合わせより15分前集合
         *  [00:21.11]总会比相约时间早15分钟集合
         *  [00:28.32]駅の改札ぬける
         *  [00:28.32]穿过车站的检票口
         *  [00:31.39]ざわめきにわくわくだね
         *  [00:31.39]嘈杂声令内心兴奋不已
         *  [00:35.23]どこへ向かうかなんて
         *  [00:35.23]不在意接下来要去哪里
         */
        if (TextUtils.isEmpty(secondLrcText)) {
            var lastEntry: LyricEntry? = null
            return mainEntryList?.filter { now ->
                if (lastEntry == null) {
                    lastEntry = now
                    return@filter true
                }

                if (lastEntry!!.time == now.time) {
                    lastEntry!!.secondText = now.text
                    lastEntry = null
                    return@filter false
                }

                lastEntry = now
                true
            }
        }

        val secondEntryList = secondLrcText?.let { parseLrc(it) }
        if (mainEntryList != null && secondEntryList != null) {
            for (mainEntry in mainEntryList) {
                for (secondEntry in secondEntryList) {
                    if (mainEntry.time == secondEntry.time) {
                        mainEntry.secondText = secondEntry.text
                    }
                }
            }
        }
        return mainEntryList
    }

    /**
     * 从文本解析歌词
     */
    private fun parseLrc(lrcText: String): List<LyricEntry>? {
        var lyricText = lrcText.trim()
        if (TextUtils.isEmpty(lyricText)) return null

        if (lyricText.startsWith("\uFEFF")) {
            lyricText = lyricText.replace("\uFEFF", "")
        }

        // 针对传入 Language="Media Monkey Format"; Lyrics="......"; 的情况
        lyricText = lyricText.substringAfter("Lyrics=\"")
            .substringBeforeLast("\";")

        val entryList: MutableList<LyricEntry> = ArrayList()
        val array = lyricText.split("\\n".toRegex()).toTypedArray()
        for (line in array) {
            val list = parseLine(line)
            if (!list.isNullOrEmpty()) {
                entryList.addAll(list)
            }
        }
        entryList.sort()
        return entryList
    }

    /**
     * 获取网络文本，需要在工作线程中执行
     */
    fun getContentFromNetwork(url: String?, charset: String?): String? {
        var lrcText: String? = null
        try {
            val url = URL(url)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            if (conn.responseCode == 200) {
                val `is` = conn.inputStream
                val bos = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                var len: Int
                while (`is`.read(buffer).also { len = it } != -1) {
                    bos.write(buffer, 0, len)
                }
                `is`.close()
                bos.close()
                lrcText = bos.toString(charset)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lrcText
    }

    /**
     * 解析一行歌词
     */
    private fun parseLine(line: String): List<LyricEntry>? {
        var lyricLine = line
        if (TextUtils.isEmpty(lyricLine)) {
            return null
        }
        lyricLine = lyricLine.trim { it <= ' ' }
        // [00:17.65]让我掉下眼泪的
        val lineMatcher = PATTERN_LINE.matcher(lyricLine)
        if (!lineMatcher.matches()) {
            return null
        }
        val times = lineMatcher.group(1)!!
        val text = lineMatcher.group(3)!!
        val entryList: MutableList<LyricEntry> = ArrayList()

        // [00:17.65]
        val timeMatcher = PATTERN_TIME.matcher(times)
        while (timeMatcher.find()) {
            val min = timeMatcher.group(1)!!.toLong()
            val sec = timeMatcher.group(2)!!.toLong()
            val milString = timeMatcher.group(3)!!
            var mil = milString.toLong()
            // 如果毫秒是两位数，需要乘以 10，when 新增支持 1 - 6 位毫秒，很多获取的歌词存在不同的毫秒位数
            when (milString.length) {
                1 -> mil *= 100
                2 -> mil *= 10
                4 -> mil /= 10
                5 -> mil /= 100
                6 -> mil /= 1000
            }
            val time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil
            entryList.add(LyricEntry(time, text))
        }
        return entryList
    }

    /**
     * 转为[分:秒]
     */
    fun formatTime(milli: Long): String {
        val m = (milli / DateUtils.MINUTE_IN_MILLIS).toInt()
        val s = (milli / DateUtils.SECOND_IN_MILLIS % 60).toInt()
        val mm = String.format(Locale.getDefault(), "%02d", m)
        val ss = String.format(Locale.getDefault(), "%02d", s)
        return "$mm:$ss"
    }

    /**
     * BUG java.lang.NoSuchFieldException: No field sDurationScale in class Landroid/animation/ValueAnimator; #3
     */
    @SuppressLint("SoonBlockedPrivateApi")
    @Deprecated("")
    fun resetDurationScale() {
        try {
            val mField = ValueAnimator::class.java.getDeclaredField("sDurationScale")
            mField.isAccessible = true
            mField.setFloat(null, 1f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 结合fraction，计算两个值之间的比例
     */
    fun calcScaleValue(a: Float, b: Float, f: Float, reverse: Boolean = false): Float {
        if (b == 0f) return 1f
        return 1f + ((a - b) / b) * (if (reverse) 1f - f else f)
    }

    /**
     * 颜色值插值函数
     */
    fun lerpColor(a: Int, b: Int, f: Float): Int {
        return argbEvaluator.evaluate(f, a, b) as Int
    }

    /**
     * 简单的插值函数
     */
    fun lerp(from: Float, to: Float, fraction: Float): Float {
        return from + (to - from) * fraction
    }

    /**
     * 判断MotionEvent是否发生在Rect中
     */
    fun MotionEvent.insideOf(rect: Rect?): Boolean {
        rect ?: return false
        return rect.contains(x.toInt(), y.toInt())
    }

    fun normalize(min: Float, max: Float, value: Float, limit: Boolean = false): Float {
        if (min == max) return 1f
        return ((value - min) / (max - min)).let {
            if (limit) it.coerceIn(0f, 1f) else it
        }
    }
}