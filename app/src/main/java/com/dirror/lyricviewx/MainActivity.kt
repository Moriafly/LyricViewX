package com.dirror.lyricviewx

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val lyricViewX = findViewById<LyricViewX>(R.id.lyricViewX)

        val lyric = """
            [00:05.70] 原唱：一支榴莲
            [00:06.45] 作曲 : PSROSIE
            [00:07.52] 作词 : PSROSIE
            [00:09.02] 翻唱：酩九
            [00:23.97] 散落的月光穿过了云
            [00:33.61] 躲着人群
            [00:38.00] 铺成大海的鳞
            [00:41.56] 海浪打湿白裙
            [00:44.40] 试图推你回去
            [00:50.66] 海浪清洗血迹
            [00:52.97] 妄想温暖你
            [00:59.14] 往海的深处听
            [01:03.45] 谁的哀鸣在指引
            [01:08.65] 灵魂没入寂静
            [01:10.94] 无人将你吵醒
            [01:17.23] 你喜欢海风咸咸的气息
            [01:20.10] 踩着湿湿的沙砾
            [01:22.37] 你说人们的骨灰应该撒进海里
            [01:26.09] 你问我死后会去哪里
            [01:29.03] 有没有人爱你
            [01:31.09] 世界能否不再
            [01:35.27] 总爱对凉薄的人扯着笑脸
            [01:39.70] 岸上人们脸上都挂着无关
            [01:44.20] 人间毫无留恋
            [01:46.51] 一切散为烟
            [02:24.45] 散落的月光穿过了云
            [02:33.65] 躲着人群
            [02:38.07] 溜进海底
            [02:42.03] 海浪清洗血迹
            [02:44.64] 妄想温暖你
            [02:50.80] 灵魂没入寂静
            [02:53.27] 无人将你吵醒
            [02:59.60] 你喜欢海风咸咸的气息
            [03:02.25] 踩着湿湿的沙砾
            [03:04.60] 你说人们的骨灰应该撒进海里
            [03:08.16] 你问我死后会去哪里
            [03:11.07] 有没有人爱你
            [03:13.29] 世界已然将你抛弃
            [03:17.49] 总爱对凉薄的人扯着笑脸
            [03:21.90] 岸上人们脸上都挂着无关
            [03:26.32] 人间毫无留恋
            [03:28.53] 一切散为烟
            [03:34.70] 来不及来不及
            [03:39.30] 你曾笑着哭泣
            [03:43.47] 来不及来不及
            [03:47.75] 你颤抖的手臂
            [03:52.07] 来不及来不及
            [03:56.69] 无人将你打捞起
            [04:01.39] 来不及来不及
            [04:05.77]你明明讨厌窒息
        """.trimIndent()
        lyricViewX.loadLyric(lyric)
        lyricViewX.setDraggable(true, object : OnPlayClickListener {
            override fun onPlayClick(time: Long): Boolean {
                lyricViewX.updateTime(time)
                return true
            }
        })
    }


}