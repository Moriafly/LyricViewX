package com.dirror.lyricviewx

import android.animation.TimeInterpolator

/**
 * Salt Spring 动画插值器
 * [stiffness] 刚性，[dampingRatio] 阻尼（推荐 0.99F）
 * @author Moriafly
 */
internal class SaltSpringInterpolator(
    private val stiffness: Float,
    private var dampingRatio: Float,
    velocity: Float
) : TimeInterpolator {

    private var mVelocity: Float

    private var mDuration = 0f

    private var canGetDuration = true

    override fun getInterpolation(ratio: Float): Float {
        if (canGetDuration) {
            getDuration(ratio)
        }
        val starVal = 0f
        val endVal = 1f
        val mDeltaT = ratio * mDuration
        val lastDisplacement = ratio - endVal
        val mNaturalFreq = Math.sqrt(stiffness.toDouble()).toFloat()
        val mDampedFreq = (mNaturalFreq * Math.sqrt(1.0 - dampingRatio * dampingRatio)).toFloat()
        val sinCoeff =
            (1.0 / mDampedFreq * (dampingRatio * mNaturalFreq * lastDisplacement + mVelocity)).toFloat()
        val displacement = (Math.pow(
            Math.E,
            (-dampingRatio * mNaturalFreq * mDeltaT).toDouble()
        ) * (lastDisplacement * Math.cos(
            (mDampedFreq * mDeltaT).toDouble()
        ) + sinCoeff * Math.sin((mDampedFreq * mDeltaT).toDouble()))).toFloat()
        mVelocity = (displacement * -mNaturalFreq * dampingRatio
                + Math.pow(Math.E, (-dampingRatio * mNaturalFreq * mDeltaT).toDouble())
                * (-mDampedFreq * lastDisplacement * Math.sin((mDampedFreq * mDeltaT).toDouble())
                + mDampedFreq * sinCoeff * Math.cos((mDampedFreq * mDeltaT).toDouble()))).toFloat()
        val mValue = displacement + endVal
        // Log.e("mValue", mValue.toString())
        return mValue
    }

    fun setVelocityInSeconds(velocity: Float) {
        mVelocity = velocity / 1000f
    }

    fun setDampingRatio(dampingRatio: Float) {
        this.dampingRatio = dampingRatio
    }

    private fun getDuration(ratio: Float) {
        if (ratio != 0f) {
            val oneFrameRatio = ratio - 0
            val timeInMs = 1f / oneFrameRatio * (1000f / 60f)
            mDuration = timeInMs / 1000f
            canGetDuration = false
        }
    }

    init {
        mVelocity = velocity / 1000f
    }
}