package com.alexvasilkov.gestures

import android.graphics.Matrix

/**
 * 위치, 확대축소비률, 회전각도, Matrix 등의 정보들을 관리하는 클라스
 */
class State {
    companion object {
        private const val EPSILON = 0.001f

        fun equals(v1: Float, v2: Float) = v1 >= v2 - EPSILON && v1 <= v2 + EPSILON

        fun compare(v1: Float) = if (v1 > 0f + EPSILON) 1 else if (v1 < 0f - EPSILON) -1 else 0
    }
    // 화상의 Matrix
    private val matrix = Matrix()
    // 화상의 Matrix를 보관하는 Float 배렬
    private val matrixValues = FloatArray(9)

    // 화상의 x 좌표
    var x = 0f
    // 화상의 y 좌표
    var y = 0f
    // 확대축소 비률
    var zoom = 1f
    // FileManager에서 화상보기로 열었을때 확대축소에 제한이 없어야 한다.
    var canZoomDown = false;

    /**
     * 파라메터로 들어온 Matrix에 현재의 Matrix를 설정하여 준다.
     */
    operator fun get(matrix: Matrix) {
        matrix.set(this.matrix)
    }

    /**
     * 화상을 현재위치에서 (dx, dy) 만큼 이동시킨다.
     */
    fun translateBy(dx: Float, dy: Float) {
        matrix.postTranslate(dx, dy)
        updateFromMatrix(false)
    }

    /**
     * 화상을 (x, y)의 위치로 이동시킨다.
     */
    fun translateTo(x: Float, y: Float) {
        matrix.postTranslate(-this.x + x, -this.y + y)
        updateFromMatrix(false)
    }

    /**
     * 화상을 (pivotX, pivotY)를 중심으로 현재보다 factor만큼 확대축소시킨다.
     */
    fun zoomBy(factor: Float, pivotX: Float, pivotY: Float) {
        if (zoom * factor >= 1f || canZoomDown) {
            // 화면보다 크게 확대시킬수 있다.
            matrix.postScale(factor, factor, pivotX, pivotY)
            updateFromMatrix(true)
        } else {
            // viewPager가 축소될때 화상크기는 화면에 맞게 1f로 설정하고
            // 축소효과는 Fragment 전체를 축소시킨다.
            if (zoom > 1f) {
                // 실례로 1.0034f에서 0.9983으로 넘어갈때 화상확대를 1f로 맞추어 준다
                matrix.postScale(1f / zoom, 1f / zoom, pivotX, pivotY)
                zoom = 1f
            } else {
                // matrix는 변경하지않고 state만 갱신하여 onStateChanged로 넘어간다
                zoom *= factor
            }
        }
    }

    /**
     * 화상을 (pivotX, pivotY)를 중심으로 초기보다 zoom만큼 확대축소시킨다.
     */
    fun zoomTo(zoom: Float, pivotX: Float, pivotY: Float) {
        if (this.zoom >= 1f || canZoomDown) {
            matrix.postScale(zoom / this.zoom, zoom / this.zoom, pivotX, pivotY)
            updateFromMatrix(true)
        } else {
            // viewPager가 축소될때 화상크기는 화면에 맞게 1f로 설정하고
            // 축소효과는 Fragment 전체를 축소시킨다.
            // matrix는 변경하지않고 state만 갱신하여 onStateChanged로 넘어간다
            this.zoom = zoom
        }
    }

    /**
     * 위치, 확대축소비률, 회전각도를 설정하고 Matrix도 그에 맞게 갱신한다.
     * @param x 설정하려는 화상의 x 좌표
     * @param y 설정하려는 화상의 y 좌표
     * @param zoom 설정하려는 화상의 확대축소비률
     */
    operator fun set(x: Float, y: Float, zoom: Float) {
        this.x = x
        this.y = y
        this.zoom = zoom

        matrix.reset()
        if (zoom != 1f) {
            matrix.postScale(zoom, zoom)
        }

        matrix.postTranslate(x, y)
    }

    /**
     * 파라메터로 받은 state의 값들을 현재의 값들에 설정한다.
     * @param other 복사하려는 state변수
     */
    fun set(other: State) {
        x = other.x
        y = other.y
        zoom = other.zoom
        canZoomDown = other.canZoomDown
        matrix.set(other.matrix)
    }

    /**
     * @return 현재의 값들로 새로운 State를 만들어 되돌린다.
     */
    fun copy(): State {
        val copy = State()
        copy.set(this)
        return copy
    }

    /**
     * 변화된 Matrix로부터 화상의 위치, 회전각도, 확대축소비률을 얻어낸다.
     * @param updateZoom Matrix로부터 확대축소비률을 얻어 변수를 갱신하겠는가 여부
     */
    private fun updateFromMatrix(updateZoom: Boolean) {
        matrix.getValues(matrixValues)
        x = matrixValues[2]
        y = matrixValues[5]
        if (updateZoom) {
            zoom = Math.hypot(matrixValues[1].toDouble(), matrixValues[4].toDouble()).toFloat()
        }
    }

    /**
     * @return 파라메터값과 현재의 state값이 일치한가의 여부를 되돌린다.
     */
    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }

        if (obj == null || javaClass != obj.javaClass) {
            return false
        }

        val state = obj as State?

        return equals(state!!.x, x) && equals(state.y, y) && equals(state.zoom, zoom)
    }

    override fun hashCode(): Int {
        var result = if (x != 0f) java.lang.Float.floatToIntBits(x) else 0
        result = 31 * result + if (y != 0f) java.lang.Float.floatToIntBits(y) else 0
        result = 31 * result + if (zoom != 0f) java.lang.Float.floatToIntBits(zoom) else 0
        return result
    }

    override fun toString() = "State(x=$x, y=$y, zoom=$zoom)"
}
