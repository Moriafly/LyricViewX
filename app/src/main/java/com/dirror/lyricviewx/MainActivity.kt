package com.dirror.lyricviewx

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val lyricViewX = findViewById<LyricViewX>(R.id.lyricViewX)

        val lyric = """
            [by:花丸的蜜柑面包]
            [00:00.00]作词 : Akira Sunset
            [00:00.27]作曲 : 馬場龍樹
            [00:00.54]编曲 : 縄田寿志
            [00:00.81]ギュッとかたく繋いだ手
            [00:00.81]手手相牵 心心相连
            [00:04.15]やがていつかは
            [00:04.15]终有一天
            [00:06.38]離す時が来るんだろう
            [00:06.38]离别将至
            [00:09.08]だけどもうI don't cry
            [00:09.08]但我已经 不再哭泣
            [00:12.05]顔あげて　笑顔を見せて
            [00:12.05]昂首挺胸 展露笑颜
            [00:17.06]最高の思い出作ろう
            [00:17.06]创造出那最美好的回忆吧
            [00:43.07]窓の外が真っ赤に染まって
            [00:43.07]窗外已被夕阳浸润
            [00:48.55]今日がまた暮れて行くよ
            [00:48.55]迟暮之时已经到来
            [00:53.29]茜空はすぐに消えるけれど
            [00:53.29]晚霞浸染之空终将消散
            [00:58.58]忘れられないほど綺麗だから
            [00:58.58]却终成了无法忘怀之美
            [01:03.50]今この時間（とき）の事を
            [01:03.50]此时此刻 所生之事
            [01:06.75]人は永遠って呼ぶのかな、たぶん
            [01:06.75]大概会被人们称之为 永远
            [01:13.81]みんなと紡いだ想いはこれからも
            [01:13.81]和大家一起编织的回忆
            [01:18.83]繋がっていくから
            [01:18.83]永远相连
            [01:25.80]ギュッとかたく握った手
            [01:25.80]那紧紧握住的双手
            [01:29.23]胸にあてて
            [01:29.23]誓言于胸前
            [01:31.93]心に誓おう　友情Forever
            [01:31.93]在心中默默起誓 友情Forever
            [01:37.13]ありがとう　出逢ってくれて
            [01:37.13]谢谢大家与我的相会
            [01:42.05]一番の宝物だよ
            [01:42.05]这将是最珍贵的宝物
            [02:08.15]あの日泣いてしまった事も
            [02:08.15]那日声泪俱下之事
            [02:13.54]今は笑って話せる
            [02:13.54]如今已成过往云烟
            [02:18.27]思い返せば　どんな時だって
            [02:18.27]回忆涌现 无论何时
            [02:24.03]隣で励ましてくれてたよね
            [02:24.03]你都在身旁鼓励着我
            [02:28.67]何気ない日常も
            [02:28.67]不经意的日常
            [02:31.83]いつか特別な青春の1ページ
            [02:31.83]不知何时也成为了青春中特别的一页
            [02:38.98]皆と過ごした日々はいつまでも
            [02:38.98]和大家一起度过的时日 不论何时都会
            [02:44.09]輝き続ける
            [02:44.09]熠熠生辉
            [02:50.87]ギュッとかたく繋いだ手
            [02:50.87]手手相牵 心心相连
            [02:54.31]やがていつかは
            [02:54.31]终有一天
            [02:56.91]離す時が来るんだろう
            [02:56.91]离别将至
            [02:59.33]だけどもうI don't cry だけどもうI don't cry だけどもうI don't cry
            [02:59.33]但我已经 不再哭泣
            [03:02.30]顔あげて　笑顔を見せて 顔あげて　笑顔を見せて 顔あげて　笑顔を見せて
            [03:02.30]昂首挺胸 展露笑颜
            [03:07.03]最高の思い出作ろう 最高の思い出作ろう 最高の思い出作ろう 最高の思い出作ろう
            [03:07.03]创造出那最美好的回忆吧
            [03:11.03]このままずっといれたらいいな
            [03:11.03]要是能一直这样该多好啊
            [03:21.52]でも分かってる　季節は巡る
            [03:21.52]但是我早已知晓 时光荏苒
            [03:29.23]後悔なんてしたくないから
            [03:29.23]因为不想去后悔
            [03:35.18]走れ　全力で
            [03:35.18]所以全力奔跑吧
            [03:58.21]ギュッとかたく握った手
            [03:58.21]那紧紧握住的双手
            [04:01.37]胸にあてて
            [04:01.37]誓言于胸前
            [04:03.97]心に誓おう　友情Forever
            [04:03.97]在心中默默起誓 友情Forever
            [04:09.36]ありがとう　出逢ってくれて
            [04:09.36]谢谢大家与我的相会
            [04:14.56]もうちょっと感じていたい
            [04:14.56]想将时间定格于此
            [04:19.20]この永遠の一瞬を
            [04:19.20]这永远的一瞬
        """.trimIndent()
        lyricViewX.loadLyric(lyric)
        lyricViewX.setTextGravity(GRAVITY_LEFT)
        lyricViewX.setNormalTextSize(80f)
        lyricViewX.setCurrentTextSize(100f)
        lyricViewX.setTranslateTextScaleValue(0.8f)
        lyricViewX.setHorizontalOffset(-200f)

        var position = 0L
        lyricViewX.setDraggable(true, object : OnPlayClickListener {
            override fun onPlayClick(time: Long): Boolean {
                position = time
                lyricViewX.updateTime(position)
                return true
            }
        })

        fun lyricUpdateLoop() {
            title = "LyricViewX: ${position / 1000L}"
            lyricViewX.updateTime(position)
            position += 200L
            lyricViewX.postDelayed({ lyricUpdateLoop() }, 200)
        }

        lyricViewX.postDelayed({ lyricUpdateLoop() }, 1000)
    }
}