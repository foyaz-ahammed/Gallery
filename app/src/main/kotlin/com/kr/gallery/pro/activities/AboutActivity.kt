package com.kr.gallery.pro.activities

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.kr.commons.extensions.*
import com.kr.gallery.pro.R
import kotlinx.android.synthetic.main.activity_about.*

/**
 * 정보화면의 activity
 * 프로그람의 판본, 개발팀 현시
 * @see SettingsActivity
 */
class AboutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(about_holder)
    }

    fun onBackSelect(view: View) {
        finish()
    }
}
