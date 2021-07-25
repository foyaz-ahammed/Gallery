package com.alexvasilkov.gestures

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF

class MovementBounds(private val settings: Settings) {
    companion object {
        private val tmpMatrix = Matrix()
        private val tmpPointArr = FloatArray(2)
        private val tmpRect = Rect()
        private val tmpRectF = RectF()
    }

    private val bounds = RectF()
    private var boundsPivotX = 0f
    private var boundsPivotY = 0f

    /**
     * 현재의 state에 기초하여 화상의 경계사각형을 계산한다.
     */
    fun set(state: State): MovementBounds {
        val area = tmpRectF
        GravityUtils.getMovementAreaPosition(settings, tmpRect)
        area.set(tmpRect)

        val pos = tmpRect
        boundsPivotY = 0f
        boundsPivotX = boundsPivotY

        state[tmpMatrix]

        GravityUtils.getImagePosition(tmpMatrix, settings, pos)

        calculateNormalBounds(area, pos)

        state[tmpMatrix]

        val imageRect = tmpRectF
        imageRect.set(0f, 0f, settings.imageWidth, settings.imageHeight)
        tmpMatrix.mapRect(imageRect)

        tmpPointArr[1] = 0f
        tmpPointArr[0] = tmpPointArr[1]
        tmpMatrix.mapPoints(tmpPointArr)

        bounds.offset(tmpPointArr[0] - imageRect.left, tmpPointArr[1] - imageRect.top)
        return this
    }

    private fun calculateNormalBounds(area: RectF, pos: Rect) {
        if (area.width() < pos.width()) {
            bounds.left = area.left - (pos.width() - area.width())
            bounds.right = area.left
        } else {
            bounds.right = pos.left.toFloat()
            bounds.left = bounds.right
        }

        if (area.height() < pos.height()) {
            bounds.top = area.top - (pos.height() - area.height())
            bounds.bottom = area.top
        } else {
            bounds.bottom = pos.top.toFloat()
            bounds.top = bounds.bottom
        }
    }

    fun getExternalBounds(out: RectF) {
        out.set(bounds)
    }

    fun restrict(x: Float, y: Float, extraX: Float, extraY: Float, out: PointF) {
        tmpPointArr[0] = x
        tmpPointArr[1] = y

        tmpPointArr[0] = MathUtils.restrict(tmpPointArr[0], bounds.left - extraX, bounds.right + extraX)
        tmpPointArr[1] = MathUtils.restrict(tmpPointArr[1], bounds.top - extraY, bounds.bottom + extraY)

        out.set(tmpPointArr[0], tmpPointArr[1])
    }

    fun restrict(x: Float, y: Float, out: PointF) {
        restrict(x, y, 0f, 0f, out)
    }
}
