package com.kr.gallery.pro.custom

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

//Recycler view의 layout변화를 알려주는 layout manager이다
class NotifyingLinearLayoutManager(context: Context, var mCallback: OnLayoutCompleteCallback? = null):
        LinearLayoutManager(context, VERTICAL, false) {
    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        mCallback?.onLayoutComplete()
    }
}
