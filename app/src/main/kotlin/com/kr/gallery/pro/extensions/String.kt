package com.kr.gallery.pro.extensions

import android.os.Environment
import com.kr.commons.extensions.*
import com.kr.gallery.pro.helpers.*
import java.io.File
import java.io.IOException

fun String.getImageType(): Int {
    return when {
        isVideoFast() -> TYPE_VIDEOS
        isGif() -> TYPE_GIFS
        isRawFast() -> TYPE_RAWS
        isSvg() -> TYPE_SVGS
        else -> TYPE_IMAGES
    }
}

// recognize /sdcard/DCIM as the same folder as /storage/emulated/0/DCIM
fun String.getDistinctPath(): String {
    return try {
        File(this).canonicalPath.toLowerCase()
    } catch (e: IOException) {
        toLowerCase()
    }
}

fun String.isDownloadsFolder() = equals(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString(), true)
