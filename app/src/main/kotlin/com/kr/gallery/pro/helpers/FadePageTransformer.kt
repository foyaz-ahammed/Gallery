package com.kr.gallery.pro.helpers

import android.view.View
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2

class FadePageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        view.translationX = view.width * -position

        view.alpha = if (position <= -1f || position >= 0.5f) {
            0f
        } else if (position == 0f) {
            1f
        } else if (position < 0.5f && position > 0f) 1f - position / 0.5f
        else if (position < 0 && position > -0.5f) 1f
        else 1f - (Math.abs(position) - 0.5f) / 0.5f

        view.scaleX = if (position <= -1f || position >= 1f) {
            1f
        } else if (position == 0f) {
            1f
        } else {
            if (position < 0) 1f - position / 2f
            else 1f
        }
        view.scaleY = view.scaleX
    }
}
