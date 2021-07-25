package com.kr.gallery.pro.custom

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

//Recycler view의 layout변화를 알려주는 layout manager이다
class NotifyingGridLayoutManager : GridLayoutManager {
    private var mCallback: OnLayoutCompleteCallback? = null

    constructor(context: Context, spanCount: Int) : super(context, spanCount)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, spanCount: Int, orientation: Int, reverseLayout: Boolean) : super(context, spanCount, orientation, reverseLayout)

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        mCallback?.onLayoutComplete()
    }

    fun setLayoutCallback(callback: OnLayoutCompleteCallback) {
        mCallback = callback
    }
}
