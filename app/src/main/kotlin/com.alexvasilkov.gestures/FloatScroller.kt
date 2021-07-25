package com.alexvasilkov.gestures

import android.os.SystemClock
import android.view.animation.AccelerateDecelerateInterpolator

class FloatScroller {
    private val interpolator = AccelerateDecelerateInterpolator()
    // 계수 시작 시간
    private var startRtc = 0L
    // 계수 시작 값
    private var startValue = 0f
    // 계수 마감 값
    private var final = 0f
    // 계수가 끝났는가를 나타낸다.
    var isFinished = true
    // 현재의 계수값
    var curr = 0f

    /**
     * 강제로 끝낸다.
     */
    fun forceFinished() {
        isFinished = true
    }

    /**
     * 계수 시작 함수
     * @param startValue 계수 시작값
     * @param finalValue 계수 마감값
     */
    fun startScroll(startValue: Float, finalValue: Float) {
        isFinished = false
        startRtc = SystemClock.elapsedRealtime()

        this.startValue = startValue
        this.final = finalValue
        curr = startValue
    }

    /**
     * 계수값 계산
     */
    fun computeScroll() {
        if (isFinished) {
            return
        }

        // 경과시간 계산
        val elapsed = SystemClock.elapsedRealtime() - startRtc
        val duration = Settings.ANIMATIONS_DURATION
        if (elapsed >= duration) {
            // 경과시간이 초과되였으므로 계수 끝내기
            isFinished = true
            curr = final
            return
        }

        // 현재 계수값 계산
        val time = interpolator.getInterpolation(elapsed.toFloat() / duration)
        curr = interpolate(startValue, final, time)
    }

    private fun interpolate(x1: Float, x2: Float, state: Float) = x1 + (x2 - x1) * state
}
