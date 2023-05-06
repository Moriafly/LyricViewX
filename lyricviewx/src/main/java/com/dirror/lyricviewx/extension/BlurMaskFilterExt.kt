package com.dirror.lyricviewx.extension

import android.graphics.BlurMaskFilter
import android.util.SparseArray

class BlurMaskFilterExt {
    private val maskFilterCache = SparseArray<BlurMaskFilter>()

    fun get(radius: Int): BlurMaskFilter? {
        if (radius == 0 || radius > 25) return null

        return maskFilterCache[radius] ?: BlurMaskFilter(radius.toFloat(), BlurMaskFilter.Blur.NORMAL)
            .also { maskFilterCache.put(radius, it) }
    }
}