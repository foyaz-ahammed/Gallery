package com.kr.gallery.pro.extensions

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.kr.gallery.pro.views.KrBottomSheet

/**
 * 기정적으로 Bottom sheet를 끌기하면 bottom sheet가 따라 움직인다.
 * 이 함수는 바로 그 동작을 없애주는것이다.
 * @param bottomBarState: Bottom sheet의 초기상태
 */
fun BottomSheetBehavior<KrBottomSheet>.disableDragging(bottomBarState: Int) {
    setBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
        private var mLastBottomState = bottomBarState

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if(newState == BottomSheetBehavior.STATE_DRAGGING){
                state = when (mLastBottomState) {
                    BottomSheetBehavior.STATE_HIDDEN -> BottomSheetBehavior.STATE_HIDDEN
                    BottomSheetBehavior.STATE_EXPANDED -> BottomSheetBehavior.STATE_EXPANDED
                    else -> BottomSheetBehavior.STATE_COLLAPSED
                }
            }
            mLastBottomState = state
        }
    })
}
