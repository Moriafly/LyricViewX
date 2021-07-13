package com.dirror.lyricviewx

import android.animation.TimeInterpolator
import kotlin.math.pow

/**
 * Smooth 插值器
 * @author Moriafly
 */
@Deprecated("过时")
class SmoothInterpolator: TimeInterpolator {

    override fun getInterpolation(input: Float): Float {
        val a = 1.11571230005336
        val b = -1.99852071205059
        val c = 0.272428743837376
        val d = -1.15835562067601E-05
        return ((a - d) / (1.0 + (input.toDouble() / c).pow(b)) + d).toFloat()
    }

}