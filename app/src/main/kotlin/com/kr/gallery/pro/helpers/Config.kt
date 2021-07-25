package com.kr.gallery.pro.helpers

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import com.kr.commons.helpers.BaseConfig
import com.kr.gallery.pro.R

/**
 * Manage the settings preference values
 */
class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    fun isFirstDatabaseInstance(): Boolean = prefs.getBoolean(FIRST_DATABASE_INSTANCE, true)
    fun clearFirstDatabaseInstance() {
        prefs.edit().putBoolean(FIRST_DATABASE_INSTANCE, false).apply()
    }

    fun isFirstMediaAction(): Boolean = prefs.getBoolean(FIRST_MEDIA_ACTION, true)
    fun clearFirstMediaAction() {
        prefs.edit().putBoolean(FIRST_MEDIA_ACTION, false).apply()
    }

    var mediaColumnCnt: Int
        get() = prefs.getInt(getMediaColumnsField(), getDefaultMediaColumnCount())
        set(mediaColumnCnt) = prefs.edit().putInt(getMediaColumnsField(), mediaColumnCnt).apply()

    private fun getMediaColumnsField(): String {
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        return if (isPortrait) {
            MEDIA_COLUMN_CNT
        } else {
            MEDIA_LANDSCAPE_COLUMN_CNT
        }
    }

    private fun getDefaultMediaColumnCount() = context.resources.getInteger(R.integer.media_columns_vertical_scroll)

    var slideshowInterval: Int
        get() = prefs.getInt(SLIDESHOW_INTERVAL, SLIDESHOW_DEFAULT_INTERVAL)
        set(slideshowInterval) = prefs.edit().putInt(SLIDESHOW_INTERVAL, slideshowInterval).apply()

    var slideshowIncludeVideos: Boolean
        get() = prefs.getBoolean(SLIDESHOW_INCLUDE_VIDEOS, false)
        set(slideshowIncludeVideos) = prefs.edit().putBoolean(SLIDESHOW_INCLUDE_VIDEOS, slideshowIncludeVideos).apply()

    var slideshowIncludeGIFs: Boolean
        get() = prefs.getBoolean(SLIDESHOW_INCLUDE_GIFS, false)
        set(slideshowIncludeGIFs) = prefs.edit().putBoolean(SLIDESHOW_INCLUDE_GIFS, slideshowIncludeGIFs).apply()

    var slideshowRandomOrder: Boolean
        get() = prefs.getBoolean(SLIDESHOW_RANDOM_ORDER, false)
        set(slideshowRandomOrder) = prefs.edit().putBoolean(SLIDESHOW_RANDOM_ORDER, slideshowRandomOrder).apply()

    var slideshowMoveBackwards: Boolean
        get() = prefs.getBoolean(SLIDESHOW_MOVE_BACKWARDS, false)
        set(slideshowMoveBackwards) = prefs.edit().putBoolean(SLIDESHOW_MOVE_BACKWARDS, slideshowMoveBackwards).apply()

    var slideshowAnimation: Int
        get() = prefs.getInt(SLIDESHOW_ANIMATION, SLIDESHOW_ANIMATION_SLIDE)
        set(slideshowAnimation) = prefs.edit().putInt(SLIDESHOW_ANIMATION, slideshowAnimation).apply()

    var loopSlideshow: Boolean
        get() = prefs.getBoolean(SLIDESHOW_LOOP, false)
        set(loopSlideshow) = prefs.edit().putBoolean(SLIDESHOW_LOOP, loopSlideshow).apply()

    var extendedDetails: Int
        get() = prefs.getInt(EXTENDED_DETAILS, EXT_RESOLUTION or EXT_LAST_MODIFIED or EXT_EXIF_PROPERTIES)
        set(extendedDetails) = prefs.edit().putInt(EXTENDED_DETAILS, extendedDetails).apply()

    var groupDirectSubfolders: Boolean
        get() = prefs.getBoolean(GROUP_DIRECT_SUBFOLDERS, false)
        set(groupDirectSubfolders) = prefs.edit().putBoolean(GROUP_DIRECT_SUBFOLDERS, groupDirectSubfolders).apply()

    var showWidgetFolderName: Boolean
        get() = prefs.getBoolean(SHOW_WIDGET_FOLDER_NAME, true)
        set(showWidgetFolderName) = prefs.edit().putBoolean(SHOW_WIDGET_FOLDER_NAME, showWidgetFolderName).apply()

    var lastEditorDrawColorNum: Int
        get() = prefs.getInt(LAST_EDITOR_DRAW_COLOR_NUM, 0)
        set(lastEditorDrawColorNum) = prefs.edit().putInt(LAST_EDITOR_DRAW_COLOR_NUM, lastEditorDrawColorNum).apply()

    var lastEditorBrushSize: Int
        get() = prefs.getInt(LAST_EDITOR_BRUSH_SIZE, 50)
        set(lastEditorBrushSize) = prefs.edit().putInt(LAST_EDITOR_BRUSH_SIZE, lastEditorBrushSize).apply()

    var lastEditorBrushStyle: Int
        get() = prefs.getInt(LAST_EDITOR_BRUSH_STYLE, 1)
        set(lastEditorBrushStyle) = prefs.edit().putInt(LAST_EDITOR_BRUSH_STYLE, lastEditorBrushStyle).apply()

    var lastEditorBrushColor: Int
        get() = prefs.getInt(LAST_EDITOR_BRUSH_COLOR, Color.WHITE)
        set(lastEditorBrushColor) = prefs.edit().putInt(LAST_EDITOR_BRUSH_COLOR, lastEditorBrushColor).apply()
}
