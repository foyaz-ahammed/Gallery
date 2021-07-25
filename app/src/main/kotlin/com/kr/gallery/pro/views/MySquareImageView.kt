package com.kr.gallery.pro.views

import android.content.Context
import android.util.AttributeSet

class MySquareImageView : androidx.appcompat.widget.AppCompatImageView {
    var isList = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        if(isList)
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        else
            super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}
