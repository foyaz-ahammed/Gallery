package com.alexvasilkov.gestures

/**
 * 화상크기, 화면크기, 확대축소 등의 정보들을 관리하는 클라스
 */
class Settings {
    companion object {
        // 끌기하여 확대할때의 최대비률
        private const val MAX_ZOOM = 5f
        // 두번눌러 확대할때의 비률
        private const val DOUBLE_TAP_ZOOM = 3f
        // 더 확대 혹은 축소 시키려는 비률
        const val OVERZOOM_FACTOR = 2f
        const val ANIMATIONS_DURATION = 300L
    }

    // ImageView의 너비
    var viewportWidth = 0
    // ImageView의 높이
    var viewportHeight = 0
    // 화상의 너비
    var imageWidth = 0f
    // 화상의 높이
    var imageHeight = 0f
    // 촤대확대비률
    var maxZoom = MAX_ZOOM
    // 두번눌러 확대 비률
    var doubleTapZoom = DOUBLE_TAP_ZOOM
    // 확대축소 기능 여부
    var isZoomEnabled = true
    // 두번누르기의 기능여부
    var swallowDoubleTaps = false

    /**
     * @return 기능여부를 되돌린다.
     */
    fun getIsEnabled() = isZoomEnabled

    /**
     * ImageView의 크기를 설정한다.
     */
    fun setViewport(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
    }

    /**
     * 화상의 크기를 설정한다.
     */
    fun setImageSize(width: Float, height: Float) {
        imageWidth = width
        imageHeight = height
    }

    /**
     * @return 두번눌러 확대 기능여부를 되돌린다.
     */
    fun isDoubleTapEnabled() = isZoomEnabled

    /**
     * @return 화상크기가 설정되였는가 여부를 되돌린다.
     */
    fun hasImageSize() = imageWidth != 0f && imageHeight != 0f

    /**
     * @return ImageView 크기가 설정되였는가 여부를 되돌린다.
     */
    fun hasViewportSize() = viewportWidth != 0 && viewportHeight != 0
}
