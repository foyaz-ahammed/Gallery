package com.alexvasilkov.gestures

import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.view.Gravity

object GravityUtils {
    private val tmpMatrix = Matrix()
    private val tmpRectF = RectF()
    private val tmpRect1 = Rect()
    private val tmpRect2 = Rect()

    fun getImagePosition(state: State, settings: Settings, out: Rect) {
        state[tmpMatrix]
        getImagePosition(tmpMatrix, settings, out)
    }

    /**
     * 화상의 경계사각형을 얻어 out에 설정한다.
     */
    fun getImagePosition(matrix: Matrix, settings: Settings, out: Rect) {
        // 화상크기 설정
        tmpRectF.set(0f, 0f, settings.imageWidth, settings.imageHeight)

        matrix.mapRect(tmpRectF)

        // 화상 너비, 높이
        val width = Math.round(tmpRectF.width())
        val height = Math.round(tmpRectF.height())

        // 경계사각형 얻기
        tmpRect1.set(0, 0, settings.viewportWidth, settings.viewportHeight)
        Gravity.apply(Gravity.CENTER, width, height, tmpRect1, out)
    }

    /**
     * 화면의 Rect를 얻어 out에 설정한다.
     */
    fun getMovementAreaPosition(settings: Settings, out: Rect) {
        tmpRect1.set(0, 0, settings.viewportWidth, settings.viewportHeight)
        Gravity.apply(Gravity.CENTER, settings.viewportWidth, settings.viewportHeight, tmpRect1, out)
    }

    /**
     * 화면의 중심점을 얻는다.
     */
    fun getDefaultPivot(settings: Settings, out: Point) {
        getMovementAreaPosition(settings, tmpRect2)
        Gravity.apply(Gravity.CENTER, 0, 0, tmpRect2, tmpRect1)
        out.set(tmpRect1.left, tmpRect1.top)
    }
}
