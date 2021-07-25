package com.kr.gallery.pro

import androidx.multidex.MultiDexApplication
import com.github.ajalt.reprint.core.Reprint

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        Reprint.initialize(this)
    }
}
