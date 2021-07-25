package com.kr.gallery.pro.models

import android.graphics.Bitmap

data class FilterItem(var bitmap: Bitmap,
                      val filter: Pair<Int, String>)
