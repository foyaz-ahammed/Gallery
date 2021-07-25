package com.kr.gallery.pro.views

interface SimpleValueAnimator {
    fun startAnimation(duration: Long)

    fun cancelAnimation()

    fun isAnimationStarted(): Boolean

    fun addAnimatorListener(animatorListener: SimpleValueAnimatorListener?)
}
