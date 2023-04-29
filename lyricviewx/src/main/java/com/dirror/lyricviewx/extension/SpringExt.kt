package com.dirror.lyricviewx.extension

import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation


object SpringExt {
    fun <T> of(obj: T): Builder<T> = Builder(obj)

    class Builder<T> internal constructor(private val target: T) {
        private var getFunc: (T.() -> Float)? = null
        private var setFunc: (T.(Float) -> Unit)? = null
        private var dampingRatio: Float? = null
        private var stiffness: Float? = null
        private var defaultValue: Float = 0f

        fun onGet(get: T.() -> Float): Builder<T> = apply { getFunc = get }
        fun onSet(set: T.(Float) -> Unit): Builder<T> = apply { setFunc = set }
        fun setStiffness(stiffness: Float): Builder<T> = apply { this.stiffness = stiffness }
        fun setDampingRatio(dampingRatio: Float): Builder<T> = apply { this.dampingRatio = dampingRatio }
        fun setDefaultValue(defaultValue: Float): Builder<T> = apply { this.defaultValue = defaultValue }

        fun build(): SpringAnimation {
            assert(getFunc != null) { "getFunc must not be null" }
            assert(setFunc != null) { "setFunc must not be null" }

            return SpringAnimation(target, object : FloatPropertyCompat<T>("") {
                override fun getValue(obj: T): Float = getFunc?.invoke(obj) ?: 0f
                override fun setValue(obj: T, value: Float) {
                    setFunc?.invoke(obj, value)
                }
            }, defaultValue).apply {
                dampingRatio?.let { spring.dampingRatio = it }
                stiffness?.let { spring.stiffness = it }
            }
        }
    }
}