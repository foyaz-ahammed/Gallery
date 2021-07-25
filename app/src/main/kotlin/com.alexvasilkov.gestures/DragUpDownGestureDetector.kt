package com.alexvasilkov.gestures

import android.view.MotionEvent
import kotlin.math.abs

/**
 * 아래서 우로밀기를 감지하는 클라스
 */
class DragUpDownGestureDetector(private val listener: OnDragGestureListener) {

    private var lastY = 0f;
    private var lastX = 0f;

    // 끌기가 진행중이라는 여부 기발
    private var dragStarted = false;

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastY = event.y
                lastX = event.x
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragStarted && isVerticalSwipe(event.x, event.y)) dragStarted = true

                if (dragStarted) {
                    listener.onDrag(this, event.y - lastY)
                    lastY = event.y
                    return true
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                dragStarted = false
            }
        }

        return dragStarted
    }

    /**
     * @return 끌기가 수직움직임인가의 여부를 되돌린다.
     */
    private fun isVerticalSwipe(eventX: Float, eventY: Float): Boolean {
        if (lastY > 0) {
            if (abs(lastX - eventX) < abs(lastY - eventY)) return true
        }
        return false
    }
    interface OnDragGestureListener {
        fun onDrag(detector: DragUpDownGestureDetector, yDelta: Float)
    }
}
