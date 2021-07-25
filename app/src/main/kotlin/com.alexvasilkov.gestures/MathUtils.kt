package com.alexvasilkov.gestures

import android.graphics.Matrix
import androidx.annotation.Size

/**
 * Animation에 리용되는 변화값들을 계산하는 객체
 */
object MathUtils {
    private val tmpMatrix = Matrix()
    private val tmpMatrixInverse = Matrix()

    /**
     * 최소/최대 비교 연산 진행
     */
    fun restrict(value: Float, minValue: Float, maxValue: Float) = Math.max(minValue, Math.min(value, maxValue))

    /**
     * 시작, 끝, 인수를 리용하여 변화값 계산
     */
    private fun interpolate(start: Float, end: Float, factor: Float) = start + (end - start) * factor

    fun interpolate(out: State, start: State, end: State, factor: Float) {
        interpolate(out, start, start.x, start.y, end, end.x, end.y, factor)
    }

    /**
     * 시작/마감 state/중심점 들과 인수를 리용하여 변화된 state를 얻는다.
     */
    fun interpolate(out: State, start: State, startPivotX: Float, startPivotY: Float, end: State, endPivotX: Float, endPivotY: Float, factor: Float) {
        out.set(start)

        if (!State.equals(start.zoom, end.zoom)) {
            val zoom = interpolate(start.zoom, end.zoom, factor)
            out.zoomTo(zoom, startPivotX, startPivotY)
        }

        val dx = interpolate(0f, endPivotX - startPivotX, factor)
        val dy = interpolate(0f, endPivotY - startPivotY, factor)
        out.translateBy(dx, dy)
    }

    /**
     * 들어온 자리표에 시작/마감 state의 변화를 적용하여 마감 자리표를 얻고 point에 설정한다.
     */
    fun computeNewPosition(@Size(2) point: FloatArray, initialState: State, finalState: State) {
        initialState[tmpMatrix]
        tmpMatrix.invert(tmpMatrixInverse)
        tmpMatrixInverse.mapPoints(point)
        finalState[tmpMatrix]
        tmpMatrix.mapPoints(point)
    }
}
