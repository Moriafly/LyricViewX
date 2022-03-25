package com.dirror.lyricviewx

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.text.TextUtils
import android.text.format.DateUtils
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
        var lyricText = lrcText
        if (TextUtils.isEmpty(lyricText)) {
            return null
        }
        if (lyricText.startsWith("\uFEFF")) {
            lyricText = lyricText.replace("\uFEFF", "")
        }
        val entryList: MutableList<LyricEntry> = ArrayList()
        val array = lyricText.split("\\n".toRegex()).toTypedArray()
        for (line in array) {
            val list = parseLine(line)
            if (list != null && !list.isEmpty()) {
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
        val times = lineMatcher.group(1)
        val text = lineMatcher.group(3)
        val entryList: MutableList<LyricEntry> = ArrayList()

        // [00:17.65]
        val timeMatcher = PATTERN_TIME.matcher(times)
        while (timeMatcher.find()) {
            val min = timeMatcher.group(1).toLong()
            val sec = timeMatcher.group(2).toLong()
            val milString = timeMatcher.group(3)
            var mil = milString.toLong()
            // 如果毫秒是两位数，需要乘以10
            if (milString.length == 2) {
                mil *= 10
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

}