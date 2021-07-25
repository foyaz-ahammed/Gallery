package com.kr.gallery.pro.views

interface SimpleValueAnimatorListener {
    fun onAnimationStarted()

    fun onAnimationUpdated(scale: Float)

    fun onAnimationFinished()
}
