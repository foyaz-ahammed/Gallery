package com.alexvasilkov.gestures

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF

/**
 * State에 대한 초기화, 확대축소 등 조절을 진행하는 클라스
 */
class StateController internal constructor(private val settings: Settings) {
    companion object {
        private val tmpState = State()
        private val tmpRect = Rect()
        private val tmpRectF = RectF()
        private val tmpPoint = Point()
        private val tmpPointF = PointF()
    }

    private val zoomBounds = ZoomBounds(settings)
    private val movBounds = MovementBounds(settings)

    /**
     * state를 초기화하고 성공여부를 되돌린다.
     */
    fun resetState(state: State): Boolean {
        // state 초기화
        state[0f, 0f] = zoomBounds.set().fitZoom
        // 화상의 위치를 tmpRect에 담는다.
        GravityUtils.getImagePosition(state, settings, tmpRect)
        // 얻은 화상의 위치를 state에 설정한다.
        state.translateTo(tmpRect.left.toFloat(), tmpRect.top.toFloat())

        return settings.hasImageSize() && settings.hasViewportSize()
    }

    /**
     * 두번눌러 확대축소를 진행할때의 목표 비률을 얻어 state를 갱신하여 되돌린다.
     * @param state 현재의 state
     * @param pivotX 확대축소 중심 x
     * @param pivotY 확대축소 중심 y
     * @return 확대축소가 진행된 새로운 state를 되돌린다.
     */
    fun toggleMinMaxZoom(state: State, pivotX: Float, pivotY: Float): State {
        zoomBounds.set()
        // 최소 비률 얻기
        val minZoom = zoomBounds.fitZoom
        // 최대 비률 얻기
        val maxZoom = if (settings.doubleTapZoom > 0f) settings.doubleTapZoom else zoomBounds.maxZoom

        val middleZoom = 0.5f * (minZoom + maxZoom)
        // 목표 비률 얻기
        val targetZoom = if (state.zoom < middleZoom) maxZoom else minZoom

        val end = state.copy()
        end.zoomTo(targetZoom, pivotX, pivotY)
        return end
    }

    /**
     * 화상을 움직이거나 확대축소할때 이전 상태와 비교하여 떨리는 현상 방지
     * @return 결과 state를 되돌린다.
     */
    fun restrictStateBoundsCopy(state: State, prevState: State, pivotX: Float, pivotY: Float): State? {
        tmpState.set(state)
        val changed = restrictStateBounds(tmpState, prevState, pivotX, pivotY, false)
        return if (changed) tmpState.copy() else null
    }

    /**
     * 화상을 움직이거나 확대축소할때 이전 상태와 비교하여 떨리는 현상 방지
     * @return 변화가 있었는지 여부를 되돌린다.
     */
    fun restrictStateBounds(state: State, prevState: State?, pivotX: Float, pivotY: Float, allowOverzoom: Boolean): Boolean {
        var newPivotX = pivotX
        var newPivotY = pivotY
        if (java.lang.Float.isNaN(newPivotX) || java.lang.Float.isNaN(newPivotY)) {
            // 화면의 중심을 찾아 설정한다.
            GravityUtils.getDefaultPivot(settings, tmpPoint)
            newPivotX = tmpPoint.x.toFloat()
            newPivotY = tmpPoint.y.toFloat()
        }

        var isStateChanged = false

        // 현재 state의 비률로부터 최소, 최대사이에 놓이는 비률을 얻는다.
        zoomBounds.set()
        val minZoom = zoomBounds.minZoom
        val maxZoom = zoomBounds.maxZoom

        val extraZoom = if (allowOverzoom) Settings.OVERZOOM_FACTOR else 1f
        var zoom = zoomBounds.restrict(state.zoom, extraZoom)

        if (prevState != null) {
            zoom = applyZoomResilience(zoom, prevState.zoom, minZoom, maxZoom, extraZoom)
        }

        if (!State.equals(zoom, state.zoom)) {
            // 목표비률로 확대축소 진행
            state.zoomTo(zoom, newPivotX, newPivotY)
            isStateChanged = true
        }

        if (state.zoom >= 1f || state.canZoomDown) {
            // 확대하여 끌기할때 필요한 bound 계산
            val extraX = 0f
            val extraY = 0f

            movBounds.set(state)
            movBounds.restrict(state.x, state.y, extraX, extraY, tmpPointF)
            var newX = tmpPointF.x
            var newY = tmpPointF.y

            if (zoom < minZoom) {
                var factor = (extraZoom * zoom / minZoom - 1f) / (extraZoom - 1f)
                factor = Math.sqrt(factor.toDouble()).toFloat()

                movBounds.restrict(newX, newY, tmpPointF)
                val strictX = tmpPointF.x
                val strictY = tmpPointF.y

                newX = strictX + factor * (newX - strictX)
                newY = strictY + factor * (newY - strictY)
            }

            if (prevState != null) {
                movBounds.getExternalBounds(tmpRectF)
                newX = applyTranslationResilience(newX, prevState.x, tmpRectF.left, tmpRectF.right, extraX)
                newY = applyTranslationResilience(newY, prevState.y, tmpRectF.top, tmpRectF.bottom, extraY)
            }

            if (!State.equals(newX, state.x) || !State.equals(newY, state.y)) {
                state.translateTo(newX, newY)
                isStateChanged = true
            }
        }

        return isStateChanged
    }

    /**
     * 이전 상태와 확대축소를 비교하여 새로운 비률을 얻어 떨림을 방지한다.
     */
    private fun applyZoomResilience(zoom: Float, prevZoom: Float, minZoom: Float, maxZoom: Float, overzoom: Float): Float {
        if (overzoom == 1f) {
            return zoom
        }

        val minZoomOver = minZoom / overzoom
        val maxZoomOver = maxZoom * overzoom

        var resilience = 0f

        if (zoom < minZoom && zoom < prevZoom) {
            resilience = (minZoom - zoom) / (minZoom - minZoomOver)
        } else if (zoom > maxZoom && zoom > prevZoom) {
            resilience = (zoom - maxZoom) / (maxZoomOver - maxZoom)
        }

        return if (resilience == 0f) {
            zoom
        } else {
            resilience = Math.sqrt(resilience.toDouble()).toFloat()
            zoom + resilience * (prevZoom - zoom)
        }
    }

    /**
     * 이전상태와 움직임거리를 비교하여 새 거리를 얻어 떨림을 방지한다.
     */
    private fun applyTranslationResilience(value: Float, prevValue: Float, boundsMin: Float, boundsMax: Float, overscroll: Float): Float {
        if (overscroll == 0f) {
            return value
        }

        var resilience = 0f
        val avg = (value + prevValue) * 0.5f

        if (avg < boundsMin && value < prevValue) {
            resilience = (boundsMin - avg) / overscroll
        } else if (avg > boundsMax && value > prevValue) {
            resilience = (avg - boundsMax) / overscroll
        }

        return if (resilience == 0f) {
            value
        } else {
            if (resilience > 1f) {
                resilience = 1f
            }
            resilience = Math.sqrt(resilience.toDouble()).toFloat()
            value - resilience * (value - prevValue)
        }
    }

    /**
     * 화상을 좌우로 움직일수 있는 길이를 너비/높이로 한 Rect를 out에 설정한다.
     */
    fun getMovementArea(state: State, out: RectF) {
        movBounds.set(state).getExternalBounds(out)
    }
}
