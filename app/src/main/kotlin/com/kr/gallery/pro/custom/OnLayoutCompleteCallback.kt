package com.kr.gallery.pro.custom

/**
 * RecyclerView에서 layout변화를 감지하기 위한 interface이다.
 * @see NotifyingLinearLayoutManager
 * @see NotifyingGridLayoutManager
 */
interface OnLayoutCompleteCallback {
    fun onLayoutComplete()
}
