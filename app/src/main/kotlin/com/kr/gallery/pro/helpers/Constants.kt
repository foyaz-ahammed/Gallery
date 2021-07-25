package com.kr.gallery.pro.helpers

// shared preferences
const val MEDIA_COLUMN_CNT = "media_column_cnt"
const val MEDIA_LANDSCAPE_COLUMN_CNT = "media_landscape_column_cnt"
const val EXTENDED_DETAILS = "extended_details"
const val GROUP_DIRECT_SUBFOLDERS = "group_direct_subfolders"
const val SHOW_WIDGET_FOLDER_NAME = "show_widget_folder_name"
const val LAST_EDITOR_DRAW_COLOR_NUM = "last_editor_draw_color_number"
const val LAST_EDITOR_BRUSH_SIZE = "last_editor_brush_size"
const val LAST_EDITOR_BRUSH_STYLE = "last_editor_brush_style"
const val LAST_EDITOR_BRUSH_COLOR = "last_editor_brush_color"

//App이 처음 기동했을때 한번만 리용되는 preference값들
const val FIRST_DATABASE_INSTANCE = "first_database_instance"
const val FIRST_MEDIA_ACTION = "first_media_action"

//Activity파괴시 상태저장
const val BOTTOM_BAR_STATE = "bottom_bar_state"
const val LAST_BOTTOM_BAR_STATE = "last_bottom_bar_state"
const val DIALOG_OPENED = "dialog_opened"
const val DIALOG_TYPE = "dialog_type"
const val SELECTED_KEYS = "selected_keys"
const val ALL_SELECTED = "all_selected"
const val IS_FULLSCREEN = "is_fullscreen"

const val TRANSFER_DIALOG = 100
const val DELETE_DIALOG = 101
const val RENAME_DIALOG = 102

// slideshow
const val SLIDESHOW_INTERVAL = "slideshow_interval"
const val SLIDESHOW_INCLUDE_VIDEOS = "slideshow_include_videos"
const val SLIDESHOW_INCLUDE_GIFS = "slideshow_include_gifs"
const val SLIDESHOW_RANDOM_ORDER = "slideshow_random_order"
const val SLIDESHOW_MOVE_BACKWARDS = "slideshow_move_backwards"
const val SLIDESHOW_ANIMATION = "slideshow_animation"
const val SLIDESHOW_LOOP = "loop_slideshow"
const val SLIDESHOW_DEFAULT_INTERVAL = 5
const val SLIDESHOW_SLIDE_DURATION = 500L
const val SLIDESHOW_FADE_DURATION = 4000L
const val SLIDESHOW_START_ON_ENTER = "slideshow_start_on_enter"

// slideshow animations
const val SLIDESHOW_ANIMATION_NONE = 0
const val SLIDESHOW_ANIMATION_SLIDE = 1
const val SLIDESHOW_ANIMATION_FADE = 2

const val MAX_COLUMN_COUNT = 20
const val MAX_CLOSE_DOWN_GESTURE_DURATION = 300
const val DRAG_THRESHOLD = 8
const val MIN_SKIP_LENGTH = 2000

const val MEDIA_IMAGE = 1
const val MEDIA_MOVIE = 2
const val DIRECTORY = "directory"
const val DIRECTORY_LIST = "directory_list"
const val DATE = "taken_date"
const val MEDIA_TYPE = "media_type"
const val SHOW_HIDDEN = "show_hidden"
const val MEDIUM = "medium"
const val PATH = "path"
const val POSITION = "position"
const val TRANSITION_KEY = "transition_key"
const val TIMESTAMP = "timestamp"
const val MEDIUM_PATH_HASH_LIST = "medium_path_hash_list"
const val EXTRA_CURRENT_TRANSITION_KEY = "extra_current_transition_key"
const val EXTRA_POSITION_ON_VIEWPAGER = "position_on_viewpager"
const val GET_IMAGE_INTENT = "get_image_intent"
const val SET_WALLPAPER_INTENT = "set_wallpaper_intent"
const val SHOULD_INIT_FRAGMENT = "should_init_fragment"
const val PORTRAIT_PATH = "portrait_path"
const val SKIP_AUTHENTICATION = "skip_authentication"

// extended details values
const val EXT_NAME = 1
const val EXT_PATH = 2
const val EXT_SIZE = 4
const val EXT_RESOLUTION = 8
const val EXT_LAST_MODIFIED = 16
const val EXT_DATE_TAKEN = 32
const val EXT_CAMERA_MODEL = 64
const val EXT_EXIF_PROPERTIES = 128
const val EXT_GPS = 2048

// media types
const val TYPE_IMAGES = 1
const val TYPE_VIDEOS = 2
const val TYPE_GIFS = 4
const val TYPE_RAWS = 8
const val TYPE_SVGS = 16

const val LOCATION_INTERNAL = 1
const val LOCATION_SD = 2
const val LOCATION_OTG = 3

// constants related to image quality
const val LOW_TILE_DPI = 160
const val NORMAL_TILE_DPI = 220
const val WEIRD_TILE_DPI = 240
const val HIGH_TILE_DPI = 280

const val DELETE_DIALOG_TAG = "delete_dialog"
const val PROPERTY_DIALOG_TAG = "property_dialog"

//림시적으로 화일을 지울때 보관하는 확장자
const val TEMP_DELETED_FILE_EXTENSION = "qkc"

// constants for action keys
const val KEY_SHARE = "android.mainactivity.bottom_share"
const val KEY_HIDE = "android.mainactivity.bottom_hide"
const val KEY_SHOW = "android.mainactivity.bottom_show"
const val KEY_AUTO_SHOW = "android.mainactivity.bottom_auto_show"
const val KEY_RENAME = "android.mainactivity.bottom_rename"
const val KEY_SET_PICTURE = "android.mainactivity.bottom_picture_set"
const val KEY_DELETE = "android.mainactivity.bottom_delete"
const val KEY_EDIT = "android.mainactivity.bottom_edit"
const val KEY_EXPAND = "android.mainactivity.bottom_expand"
const val KEY_ENCRYPT = "android.mainactivity.bottom_encrypt"
const val KEY_MOVE = "android.mainactivity.bottom_move"
