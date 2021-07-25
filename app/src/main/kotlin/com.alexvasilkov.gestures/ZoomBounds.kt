package com.alexvasilkov.gestures

class ZoomBounds(private val settings: Settings) {

    var minZoom = 0f
    var maxZoom = 0f
    var fitZoom = 0f

    /**
     * 현재의 화상과 화면크기에 기초하여 확대축소비률을 설정한다.
     */
    fun set(): ZoomBounds {
        val imageWidth = settings.imageWidth
        val imageHeight = settings.imageHeight

        val areaWidth = settings.viewportWidth.toFloat()
        val areaHeight = settings.viewportHeight.toFloat()

        if (imageWidth == 0f || imageHeight == 0f || areaWidth == 0f || areaHeight == 0f) {
            fitZoom = 1f
            maxZoom = fitZoom
            minZoom = maxZoom
            return this
        }

        minZoom = fitZoom
        maxZoom = settings.maxZoom

        // 최소 확대축소비률 얻기
        fitZoom = Math.min(areaWidth / imageWidth, areaHeight / imageHeight)

        if (maxZoom <= 0f) {
            maxZoom = fitZoom
        }

        if (fitZoom > maxZoom) {
            maxZoom = fitZoom
        }

        if (minZoom > maxZoom) {
            minZoom = maxZoom
        }

        if (fitZoom < minZoom) {
            minZoom = fitZoom
        }

        return this
    }

    fun restrict(zoom: Float, extraZoom: Float) = MathUtils.restrict(zoom, minZoom / extraZoom, maxZoom * extraZoom)
}
