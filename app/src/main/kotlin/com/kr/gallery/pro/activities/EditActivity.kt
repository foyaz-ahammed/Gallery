package com.kr.gallery.pro.activities

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.Bitmap.CompressFormat
import android.graphics.Bitmap.createBitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.View.OnLongClickListener
import android.view.animation.*
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.kr.commons.dialogs.ColorPickerDialog
import com.kr.commons.extensions.*
import com.kr.commons.helpers.REAL_FILE_PATH
import com.kr.commons.helpers.RESULT_MEDIUM
import com.kr.commons.helpers.ensureBackgroundThread
import com.kr.commons.helpers.isNougatPlus
import com.kr.commons.models.FileDirItem
import com.kr.commons.views.MySeekBar
import com.kr.gallery.pro.R
import com.kr.gallery.pro.adapters.AdjustAdapter
import com.kr.gallery.pro.adapters.CropSelectorRVAdapter
import com.kr.gallery.pro.adapters.FiltersAdapter
import com.kr.gallery.pro.dialogs.SaveBeforeExitDialog
import com.kr.gallery.pro.dialogs.SaveChangesDialog
import com.kr.gallery.pro.extensions.config
import com.kr.gallery.pro.helpers.MEDIA_IMAGE
import com.kr.gallery.pro.helpers.Utils
import com.kr.gallery.pro.models.*
import com.kr.gallery.pro.models.FilterPack.Companion.getFilterPack
import com.kr.gallery.pro.views.*
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import com.yalantis.ucrop.util.FastBitmapDrawable
import com.yalantis.ucrop.util.RectUtils
import com.yalantis.ucrop.view.CropImageView
import com.yalantis.ucrop.view.CropImageView.FREE_ASPECT_RATIO
import com.yalantis.ucrop.view.TransformImageView.TransformImageListener
import doodle.*
import doodle.DoodleView.*
import kotlinx.android.synthetic.main.activity_edit.*
import kotlinx.android.synthetic.main.bottom_bar_container.*
import kotlinx.android.synthetic.main.bottom_close_check_bar.*
import kotlinx.android.synthetic.main.bottom_editor_adjust_actions.*
import kotlinx.android.synthetic.main.bottom_editor_crop_actions.*
import kotlinx.android.synthetic.main.bottom_editor_doodle_actions.*
import kotlinx.android.synthetic.main.bottom_editor_filter_actions.*
import kotlinx.android.synthetic.main.bottom_editor_more_actions.*
import kotlinx.android.synthetic.main.bottom_editor_mosaic_actions.*
import kotlinx.android.synthetic.main.bottom_editor_primary_actions.*
import kotlinx.android.synthetic.main.bottom_more_draw_color.*
import kotlinx.android.synthetic.main.bottom_more_draw_size.*
import kotlinx.android.synthetic.main.bottom_more_draw_style.*
import kotlinx.android.synthetic.main.doodle_bottom_size_color_bar.*
import kotlinx.android.synthetic.main.doodle_image_view.*
import kotlinx.android.synthetic.main.ucrop_view.*
import org.wysaid.nativePort.CGENativeLibrary
import org.wysaid.view.ImageGLSurfaceView
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.sqrt

/**
 * 화상편집화면의 activity
 */
class EditActivity : SimpleActivity(), CropImageView.OnCropImageCompleteListener, View.OnTouchListener {

    private val TAG = "EditActivity"

    private val ROTATE_WIDGET_SENSITIVITY_COEFFICIENT = 13f

    private val mInterpolator = LinearInterpolator()
    private val mAnimator = CustomValueAnimator(mInterpolator)

    private val CROP = "crop"

    // constants for bottom primary action groups
    private val PRIMARY_ACTION_CROP = 1
    private val PRIMARY_ACTION_FILTER = 2
    private val PRIMARY_ACTION_ADJUST = 3
    private val PRIMARY_ACTION_MORE = 4

    // More에서 doodle/mosaic/draw 어느 하나도 선택하지 않았을떄
    private val DOODLE_ACTION_NONE = 5
    //More에서 doodle을 선택했을떄
    private val DOODLE_ACTION_DOODLE = 6
    //More에서 mosaic를 선택했을떄
    private val DOODLE_ACTION_MOSAIC = 7
    //More에서 draw를 선택했을떄
    private val DOODLE_ACTION_DRAW = 8

    // Draw에서 색단추들의 색갈들
    // 색갈선택창에 의하여 갱신된다
    private var MORE_DRAW_COLOR_0 = Color.RED;
    private var MORE_DRAW_COLOR_1 = Color.GREEN;
    private var MORE_DRAW_COLOR_2 = Color.BLUE;
    private var MORE_DRAW_COLOR_3 = Color.BLACK;
    private var MORE_DRAW_COLOR_4 = Color.WHITE;

    private val DOODLE_SIZE_1 = 4
    private val DOODLE_SIZE_2 = 12
    private val DOODLE_SIZE_3 = 25

    // Doodle에서 색단추들의 색갈들
    private val color_ary = arrayOf(
            "#ffffff", "#cccccc", "#808080", "#382e2d", "#000000", "#7a0400",
            "#cc0001", "#ff3737", "#9d3729", "#ff7f00", "#ffa33a", "#f7c200",
            "#a3e400", "#7caa0a", "#077c38", "#175445", "#7fd7fb", "#009afa",
            "#0265cb", "#001a65", "#3e0067", "#75008c", "#ff2e8c", "#febad3"
    )

    val DOODLE_MOSAIC_LEVEL = "doodle_mosaic_level"
    val DOODLE_MOSAIC_SIZE = "doodle_mosaic_size"

    private var realFilePath = ""
    // 화상보관 경로
    private lateinit var saveUri: Uri
    private var uri: Uri? = null
    // 현재 선택된 화면을 나타내는 상수 보관
    private var currPrimaryAction = PRIMARY_ACTION_CROP
    // 현재 선택된 Doodle을 나타내는 상수 보관
    private var currDoodleAction = DOODLE_ACTION_NONE
    // 현재 설정된 자르기비률을 나타낸다. (-1, -1)은 자유비률이다
    private var currAspectRatio: Pair<Float, Float> = Pair(-1f, -1f)
    private var currMoreDrawStyle = MORE_DRAW_STYLE_RECT
    private var currMoreDrawColor = MORE_DRAW_COLOR_0
    private var currDoodleSizeSelection = DOODLE_SIZE_1
    // 외부에서 자르기요청으로 열렸는가의 여부
    private var isCropIntent = false

    private var oldExif: ExifInterface? = null
    // 편집중일때 마지막 결과 bitmap
    private var currentBitmap: Bitmap? = null
    // 6MP 이상의 화상을 편집보관할때의 원본화상
    private var originalBitmap: Bitmap? = null

    // Doodle
    val DEFAULT_MOSAIC_SIZE: Int = 30

    val RESULT_ERROR = -111

    private lateinit var mDoodle: IDoodle
    private lateinit var mDoodleView: DoodleView

    private lateinit var mDoodleParams: DoodleParams

    private lateinit var mTouchGestureListener: DoodleOnTouchGestureListener
    // 모자이크 그리기 세기 정도
    private var mMosaicLevel = -1

    lateinit var sharedPreferences: SharedPreferences
    lateinit var editor: SharedPreferences.Editor
    // doodle에서 색단추들의 단일선택을 위한 HashMap
    // 색과 ID가 들어간다
    private val mBtnColorIds: HashMap<Int,Int> = HashMap<Int,Int>()

    // 편집 조종체들을 포함하고 있는 View. 화면절환시 animation을 위하여 보관
    lateinit var preBottomAccordian: View
    // 이전 화면을 나타내는 상수를 보관
    var prePrimaryAction = -1;

    // Filter와 Adjust에서 이름과 값들의 Animation을 위한 Handler
    private var text_alert_handler = Handler()

    private var CURVE_CHANNEL_RGB = "RGB";
    private var CURVE_CHANNEL_R = "R";
    private var CURVE_CHANNEL_G = "G";
    private var CURVE_CHANNEL_B = "B";

    private var currentRGBChannel = CURVE_CHANNEL_RGB;

    // RGB curve의 점들을 림시 보관한다.
    private var curve_points_rgb = ArrayList<PointF>()
    private var curve_points_r = ArrayList<PointF>()
    private var curve_points_g = ArrayList<PointF>()
    private var curve_points_b = ArrayList<PointF>()

    // Adjust의 curve에서 리용.
    // 효과문자렬을 구성한다 e.g. "RGB(123.3, 34.5)(54.3, 45.8)"
    private var color_points_rgb = ""
    // 효과문자렬을 구성한다 e.g. "R(123.3, 34.5)(54.3, 45.8)"
    private var color_points_r = ""
    // 효과문자렬을 구성한다 e.g. "G(123.3, 34.5)(54.3, 45.8)"
    private var color_points_g = ""
    // 효과문자렬을 구성한다 e.g. "B(123.3, 34.5)(54.3, 45.8)"
    private var color_points_b = ""

    private var historyManager = HistoryManager(this)

    private var isAdjustApplied = false

    // 자르기를 진행한후 비교화상자르기를 진행할때 리용
    private val CROP_FOR_COMPARE = "crop_for_compare";
    // 자르기에서 Filter로 넘어갈때 리용
    private val CROP_FOR_FILTER = "crop_for_filter";
    // 자르기에서 Adjust로 넘어갈때 리용
    private val CROP_FOR_ADJUST = "crop_for_adjust";
    // 자르기에서 More로 넘어갈때 리용
    private val CROP_FOR_MORE = "crop_for_more";
    // 자르기화면에서 보관을 진행할때 리용
    private val CROP_FOR_SAVE = "crop_for_save";

    // 현재의 자르기가 어떤 목적의 자르기인가를 나타낸다
    private var cropForAction = CROP_FOR_COMPARE;

    // 자르기가 진행중인가를 나타낸다
    private var mCropInProgress = false;

    // downsample된 큰화상을 위한 history를 보관할때 리용
    private val ACTION_CROP = "action_crop";
    private val ACTION_FILTER = "action_filter";
    private val ACTION_ADJUST = "action_adjust";
    private val ACTION_DOODLE = "action_doodle";
    private val ACTION_MOSAIC = "action_mosaic";
    private val ACTION_DRAW = "action_draw";

    // downsample된 큰화상을 위한 history를 보관, undo할때에는 뒤에서부터 삭제
    private val appliedHistory = ArrayList<AppliedValueHistory>()
    // downsample된 큰화상을 위한 redo history를 보관, undo할때 추가된다
    private val appliedRedoHistory = ArrayList<AppliedValueHistory>()

    // 조절화면에 동적으로 추가되는 glSurfaceView
    private lateinit var adjust_view : ImageGLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.LOG) Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_edit)

        if (checkAppSideloading()) {
            return
        }

        initEditActivity()
    }

    private fun onDoodleReady() {
        // 화면 지우기
        mDoodleView.clear()
        updateDoodleUndoRedo()

        // 펜/모양/크기/색갈/형태 새로 적재
        if (currDoodleAction == DOODLE_ACTION_DOODLE) {
            mDoodle.pen = DoodlePen.BRUSH
            loadPreference()
        } else if (currDoodleAction == DOODLE_ACTION_MOSAIC) {
            mDoodle.pen = DoodlePen.MOSAIC
            loadMosaicPreference()
        } else if(currDoodleAction == DOODLE_ACTION_DRAW) {
            mDoodle.pen = DoodlePen.BRUSH
            updateDrawThickness(config.lastEditorBrushSize)
            updateDrawStyle(config.lastEditorBrushStyle)
            updateDrawColor(config.lastEditorBrushColor)
        }
        mDoodle.shape = DoodleShape.PAINTBRUSH
    }

    // doodle 초기화가 진행되였는가를 나타낸다
    // doodle/mosaic/draw 모두 doodle기능을 리용하므로 한번 doodle초기화를 진행하면 다시 할 필요가 없다.
    private var isDoodleInitialized = false

    /**
     * doodle 초기화 진행
     */
    private fun initDoodle() {
        if (isDoodleInitialized) {
            // doodle이 이미 초기화 진행되였으면 화상 교체
            mDoodleView.setNewBitmap(currentBitmap!!)
            onDoodleReady()
            return
        }

        isDoodleInitialized = true
        mDoodleParams = DoodleParams()

        // doodle의 색갈단추들의 id를 색갈과 함께 HashMap으로 넣는다.
        // 색갈단추를 누를때 단일선택을 진행하기 위하여 보관
        for (i in 1 until color_ary.size + 1) {
            val color = color_ary[i - 1]
            try {
                val id = resources.getIdentifier("btn_doodle_color_$i", "id", packageName)
                mBtnColorIds.put(Color.parseColor(color), id)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)!!
        editor = sharedPreferences.edit()

        // mosaic의 두께설정 흘림띠
        more_mosaic_thickness_seekbar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, b: Boolean) {
                setDoodleSize(progress)
                editor.putInt(DOODLE_MOSAIC_SIZE, progress)
                editor.apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        val bitmap = default_image_view.drawable.toBitmap()
         mDoodle = DoodleViewWrapper(this, bitmap, mDoodleParams.mOptimizeDrawing, object : IDoodleListener {
            override fun onSaved(doodle: IDoodle, bitmap: Bitmap, callback: Runnable) { // Save the picture in jpg format
            }

            fun onError(i: Int, msg: String?) {
                setResult(RESULT_ERROR)
                finish()
            }

            override fun onReady(doodle: IDoodle) {
                onDoodleReady()
                if (mDoodleParams.mZoomerScale <= 0) {
                    menu_edit!!.findItem(R.id.menu_doodle_magnifier).isEnabled = false
                }
                mDoodle.zoomerScale = mDoodleParams.mZoomerScale
                mTouchGestureListener.isSupportScaleItem = mDoodleParams.mSupportScaleItem
            }
        }).also({ mDoodleView = it })

        mTouchGestureListener = object : DoodleOnTouchGestureListener(mDoodleView, object : ISelectionListener {
            // save states before being selected
            var mLastPen: IDoodlePen? = null
            var mLastColor: IDoodleColor? = null
            var mSize: Float? = null
            var mIDoodleItemListener = IDoodleItemListener { property ->
                if (mTouchGestureListener.selectedItem == null) {
                    return@IDoodleItemListener
                }
                if (property == IDoodleItemListener.PROPERTY_SCALE) {
                    item_scale.text = ((mTouchGestureListener.selectedItem.scale * 100 + 0.5f).toInt()).toString() + "%"
                }
            }

            override fun onSelectedItem(doodle: IDoodle, selectableItem: IDoodleSelectableItem, selected: Boolean) {
                if (selected) {
                    if (mLastPen == null) {
                        mLastPen = mDoodle.getPen()
                    }
                    if (mLastColor == null) {
                        mLastColor = mDoodle.getColor()
                    }
                    if (mSize == null) {
                        mSize = mDoodle.getSize()
                    }
                    mDoodleView.isEditMode = true
                    mDoodle.pen = selectableItem.pen
                    mDoodle.color = selectableItem.color
                    mDoodle.size = selectableItem.size
                    doodle_selectable_edit_container.beVisible()
                    item_scale.text = ((selectableItem.scale * 100 + 0.5f).toInt()).toString() + "%"
                    selectableItem.addItemListener(mIDoodleItemListener)
                } else {
                    selectableItem.removeItemListener(mIDoodleItemListener)
                    if (mTouchGestureListener.selectedItem == null) { // nothing is selected. 当前没有选中任何一个item
                        if (mLastPen != null) {
                            mDoodle.pen = mLastPen
                            mLastPen = null
                        }
                        if (mLastColor != null) {
                            mDoodle.color = mLastColor
                            mLastColor = null
                        }
                        if (mSize != null) {
                            mDoodle.size = mSize!!
                            mSize = null
                        }
                        doodle_selectable_edit_container.beGone()
                    }
                }
            }
        }) {
            override fun setSupportScaleItem(supportScaleItem: Boolean) {
                super.setSupportScaleItem(supportScaleItem)
                if (supportScaleItem) {
                    item_scale.beVisible()
                } else {
                    item_scale.beGone()
                }
            }
        }

        val detector: IDoodleTouchDetector = DoodleTouchDetector(applicationContext, mTouchGestureListener)
        mDoodleView.defaultTouchDetector = detector

        mDoodle.setIsDrawableOutside(mDoodleParams.mIsDrawableOutside)
        doodle_view_container.removeAllViews()
        doodle_view_container.addView(mDoodleView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        mDoodle.doodleMinScale = mDoodleParams.mMinScale
        mDoodle.doodleMaxScale = mDoodleParams.mMaxScale

        initView()
    }

    /**
     * @return 색갈을 설정할수 있으면 true, 없으면 false
     */
    private fun canChangeColor(pen: IDoodlePen): Boolean {
        return pen !== DoodlePen.MOSAIC
    }

    private fun initView() {
        doodle_selectable_edit_container.beGone()
        item_scale.setOnLongClickListener(OnLongClickListener {
            if (mTouchGestureListener.selectedItem != null) {
                mTouchGestureListener.selectedItem.scale = 1f
            }
            true
        })
    }

    override fun onResume() {
        super.onResume()
        more_draw_thickness_seekbar.setColors(config.textColor, getAdjustedPrimaryColor(), config.backgroundColor)
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        historyManager.clearHistory()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (currDoodleAction != DOODLE_ACTION_NONE) {
            bottom_bar_close.performClick()
        } else {
            if (historyManager.canUndo() ||
                    (currPrimaryAction == PRIMARY_ACTION_CROP && gesture_crop_image_view.shouldCrop()) ||
                    isFilterApplied || isAdjustApplied) SaveBeforeExitDialog(this) {
                saveImage()
            }
            else finish()
        }
    }

    private fun initEditActivity() {
        if (intent.data == null) {
            toast(R.string.invalid_image_path)
            finish()
            return
        }

        uri = intent.data!!
        if (uri!!.scheme != "file" && uri!!.scheme != "content") {
            toast(R.string.unknown_file_location)
            finish()
            return
        }

        if (intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            realFilePath = intent.extras!!.getString(REAL_FILE_PATH)!!
            uri = when {
                isPathOnOTG(realFilePath) -> uri
                realFilePath.startsWith("file:/") -> Uri.parse(realFilePath)
                else -> Uri.fromFile(File(realFilePath))
            }
        } else {
            (getRealPathFromURI(uri!!))?.apply {
                realFilePath = this
                uri = Uri.fromFile(File(this))
            }
        }

        saveUri = when {
            intent.extras?.containsKey(MediaStore.EXTRA_OUTPUT) == true -> intent.extras!!.get(MediaStore.EXTRA_OUTPUT) as Uri
            else -> uri!!
        }

        isCropIntent = intent.extras?.get(CROP) == "true"
        if (isCropIntent) {
            bottom_editor_primary_actions.beGone()
            (bottom_editor_crop_actions.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1)
        }

        btn_compare.setOnTouchListener(this)

        preBottomAccordian = bottom_editor_crop_actions

        // 조절화면의 ImageGLSurfaceView를 동적으로 생성
        adjust_view = ImageGLSurfaceView(this, null)
        adjust_view_container.addView(adjust_view, 0)

        // 처음으로 자르기화면 선택
        bottomCropClicked()
        setupBottomActions()

        if (bottom_actions_adjust_rv.adapter == null) {
            generateAdjustRVAdapter()
        }
        if (crop_rv.adapter == null ) {
            crop_rv.layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.HORIZONTAL, false)
            generateCropRVAdapter()
        }

        // set ucrop
        gesture_crop_image_view.setTransformImageListener(mImageListener)
        // 자르기의 수평계 scroll listener 설정
        straight_ruler.setScrollingListener(object : HorizontalProgressWheelView.ScrollingListener{
            override fun onScroll(delta: Float, totalDistance: Float) {
                gesture_crop_image_view.postRotate(delta / ROTATE_WIDGET_SENSITIVITY_COEFFICIENT)
                gesture_crop_image_view.setImageToWrapOriginBounds()

                updateAspectRatio()

                if (gesture_crop_image_view.currentAngle == 0f) rotate_reset.beGone()
                else rotate_reset.beVisible()
            }

            override fun onScrollEnd() {
                crop_view_overlay.setShowCropText(false);
            }

            override fun onScrollStart() {
                gesture_crop_image_view.cancelAllAnimations()

                crop_view_overlay.setShowCropText(true);
            }

        })
    }

    private val mImageListener: TransformImageListener = object : TransformImageListener {
        override fun onRotate(currentAngle: Float) {
        }

        override fun onScaleBegin() {
            crop_view_overlay.setShowCropText(true)
        }

        override fun onScaleEnd() {
            crop_view_overlay.setShowCropText(false)
        }

        override fun onScale(currentScale: Float) {
            crop_view_overlay.setCurrentImageScale(currentScale);
        }

        override fun onLoadComplete(result: Bitmap) {

        }

        override fun onLoadFailure(e: java.lang.Exception) {
            finish()
        }
    }

    /**
     * Compare단추의 누르기 기능
     */
    override fun onTouch(view: View?, evt: MotionEvent?): Boolean {
        when (evt?.action) {
            MotionEvent.ACTION_DOWN -> {
                // 비교화상 보이기
                if (currPrimaryAction == PRIMARY_ACTION_FILTER || currPrimaryAction == PRIMARY_ACTION_MORE) {
                    default_image_view_container.beGone()
                    compare_image_view.beVisible()
                } else if (currPrimaryAction == PRIMARY_ACTION_ADJUST) {
                    adjust_view.beGone()
                    compare_image_view.beVisible()
                }
            }
            MotionEvent.ACTION_UP -> {
                // 비교화상 숨기기
                if (currPrimaryAction == PRIMARY_ACTION_FILTER || currPrimaryAction == PRIMARY_ACTION_MORE) {
                    default_image_view_container.beVisible()
                    compare_image_view.beGone()
                } else if (currPrimaryAction == PRIMARY_ACTION_ADJUST) {
                    adjust_view.beVisible()
                    compare_image_view.beGone()
                }
            }
        }
        return false;
    }

    private var menu_edit: Menu ?= null
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit, menu)

        if (menu != null) {
            menu_edit = menu
        };

        return super.onCreateOptionsMenu(menu)
    }

    /**
     * 메뉴항목 기능설정
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Reset 메뉴
            R.id.menu_reset -> {
                // 초기화상을 history 화일에서 불러들인다
                val originBmp = historyManager.getNativeImage()
                // 화상 history 화일들 지우기
                historyManager.resetHistory()
                updateUndoRedoButton()

                currentBitmap = createBitmap(originBmp)
                compare_image_view.setImageBitmap(originBmp)

                // 보관된 편집 history 지우기
                appliedHistory.clear()
                appliedRedoHistory.clear()

                isFilterApplied = false
                isAdjustApplied = false

                when (currPrimaryAction) {
                    PRIMARY_ACTION_CROP -> {
                        // 자르기화면에서 Reset하였을때 화상설정
                        crop_view_overlay.setOriginCropRect(null)
                        gesture_crop_image_view.setImageBitmap(currentBitmap)
                        gesture_crop_image_view.requestLayout()
                        // 첫번째 자유비률 선택
                        (crop_rv.adapter as CropSelectorRVAdapter).selectItem(0, false)

                        // 화상이 기울어졌을때 생기는 Reset단추를 눌러 화상 바로 세우기
                        rotate_reset.performClick()
                    }
                    PRIMARY_ACTION_FILTER -> {
                        // Filter화면에서 Reset하였을때
                        default_image_view.setImageBitmap(currentBitmap)
                        updateFilterThumb()
                    }
                    PRIMARY_ACTION_ADJUST -> {
                        // Adjust화면에서 Reset하였을때
                        resetAdjustSavedValues()
                        setAdjustImage(currentBitmap!!)
                    }
                    PRIMARY_ACTION_MORE -> {
                        // More화면에서 Reset하였을떄
                        default_image_view.setImageBitmap(currentBitmap)
                    }
                }
            }

            // Save 메뉴
            R.id.menu_save -> {
                saveImage()
            }

            // Undo 메뉴(단추)
            R.id.menu_undo -> {
                // undo를 진행하였을때 편집된 화상[0]과 비교화상[1]을 얻는다
                val list = historyManager.undo() ?: return true

                currentBitmap = list[0]
                val originBmp = list[1]
                compare_image_view.setImageBitmap(originBmp)

                // 편집 history
                if (appliedHistory.size > 0) {
                    val history = appliedHistory[appliedHistory.size - 1]
                    appliedHistory.removeAt(appliedHistory.size - 1)
                    appliedRedoHistory.add(history)
                }

                when (currPrimaryAction) {
                    PRIMARY_ACTION_CROP -> {
                        // 자르기화면일때
                        crop_view_overlay.setOriginCropRect(null)
                        gesture_crop_image_view.setImageBitmap(currentBitmap)
                        gesture_crop_image_view.requestLayout()

                        // 화상 바로 세우기
                        val angleToZero = -gesture_crop_image_view.currentAngle
                        gesture_crop_image_view.postRotate(angleToZero)
                    }
                    PRIMARY_ACTION_FILTER -> {
                        default_image_view.setImageBitmap(currentBitmap)
                        updateFilterThumb()
                    }
                    PRIMARY_ACTION_ADJUST -> {
                        resetAdjustSavedValues()
                        setAdjustImage(currentBitmap!!)
                    }
                    PRIMARY_ACTION_MORE -> default_image_view.setImageBitmap(currentBitmap)
                }

                updateUndoRedoButton()
            }

            // Redo 메뉴(단추)
            R.id.menu_redo -> {
                // undo를 진행하였을때 편집된 화상[0]과 비교화상[1]을 얻는다
                val list = historyManager.redo() ?: return true

                currentBitmap = list[0]
                val originBmp = list[1]
                compare_image_view.setImageBitmap(originBmp)

                // 편집 history
                if (appliedRedoHistory.size > 0) {
                    val history = appliedRedoHistory[appliedRedoHistory.size - 1]
                    appliedRedoHistory.removeAt(appliedRedoHistory.size - 1)
                    appliedHistory.add(history)
                }

                when (currPrimaryAction) {
                    PRIMARY_ACTION_CROP -> {
                        // 자르기화면일때
                        crop_view_overlay.setOriginCropRect(null)
                        gesture_crop_image_view.setImageBitmap(currentBitmap)
                        gesture_crop_image_view.requestLayout()

                        // 화상 바로 세우기
                        val angleToZero = -gesture_crop_image_view.currentAngle
                        gesture_crop_image_view.postRotate(angleToZero)
                    }
                    PRIMARY_ACTION_FILTER -> {
                        default_image_view.setImageBitmap(currentBitmap)
                        updateFilterThumb()
                    }
                    PRIMARY_ACTION_ADJUST -> {
                        resetAdjustSavedValues()
                        setAdjustImage(currentBitmap!!)
                    }
                    PRIMARY_ACTION_MORE -> default_image_view.setImageBitmap(currentBitmap)
                }

                updateUndoRedoButton()
            }

            // doodle/mosaic/draw 에서의 Undo 메뉴(단추)
            R.id.menu_doodle_undo -> {
                mDoodle.undo()
                updateDoodleUndoRedo()
            }

            // doodle/mosaic/draw 에서의 Redo 메뉴(단추)
            R.id.menu_doodle_redo -> {
                mDoodle.redo()
                updateDoodleUndoRedo()
            }

            // doodle/mosaic/draw 에서의 Edit 메뉴(단추)
            R.id.menu_doodle_edit -> {
                mDoodleView.isEditMode = !mDoodleView.isEditMode
            }

            // doodle/mosaic/draw 에서의 Magnifier 메뉴(단추)
            R.id.menu_doodle_magnifier->{
                mDoodleView.enableZoomer(!mDoodleView.isEnableZoomer)

                val magnifierIcon = ContextCompat.getDrawable(this, R.drawable.baseline_search_24)!!
                if (mDoodleView.isEnableZoomer) {
                    magnifierIcon.colorFilter = PorterDuffColorFilter(getColor(R.color.text_selected_color), PorterDuff.Mode.SRC_IN)
                    menu_edit!!.findItem(R.id.menu_doodle_magnifier).icon = magnifierIcon
                } else menu_edit!!.findItem(R.id.menu_doodle_magnifier).icon = magnifierIcon
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    /**
     * 자르기 시작함수
     */
    private fun startCrop() {
        // 자르기 진행중 기발 True 설정
        mCropInProgress = true;

        if (scaleRateWithMaster > 1f) {
            // 큰화상을 downsample하여 적재한후 편집보관시에 편집반복을 위하여 변수들 보관
            val history = AppliedValueHistory()
            history.historyType = ACTION_CROP
            history.cropAngle = gesture_crop_image_view.currentAngle
            history.cropScaleX = gesture_crop_image_view.currentScaleX / gesture_crop_image_view.initialScale
            history.cropScaleY = gesture_crop_image_view.currentScaleY / gesture_crop_image_view.initialScale
            history.cropRectInWrapper = Rect(gesture_crop_image_view.cropRectInWrapper)
            appliedHistory.add(history)
            appliedRedoHistory.clear()
        }

        // progress spinner 현시
        progress_spinner.smoothToShow()
        // currentBitmap에서 자르기 진행
        gesture_crop_image_view.getCroppedImageAsync(currentBitmap)
    }

    /**
     *  Filter 화면 적재
     */
    private fun loadFilterImageView() {
        if (prePrimaryAction == PRIMARY_ACTION_CROP) {
            // Crop에서 넘어온 후 자르기를 진행해야 하는지 검사
            if (gesture_crop_image_view.shouldCrop()) {
                cropForAction = CROP_FOR_FILTER
                startCrop()
            } else {
                //e.g. 아래에서 releaseCropView를 호출하면 currentBitmap이 없어지므로 새로 생성
                currentBitmap = createBitmap(currentBitmap!!)
                default_image_view.setImageBitmap(currentBitmap)
                updateFilterThumb()
                updateUI()
                releaseCropView()
            }
        } else{
            val preBitmap = currentBitmap
            if (prePrimaryAction == PRIMARY_ACTION_ADJUST) {
                // Adjust에서 넘어온 경우
                adjust_view.getResultBitmap {
                    currentBitmap = it
                    updateFilterThumb()
                    if (preBitmap != currentBitmap) preBitmap!!.recycle()
                    runOnUiThread{
                        default_image_view.setImageBitmap(currentBitmap)
                        updateUI()
                        if (isAdjustApplied) {
                            if (scaleRateWithMaster > 1f) {
                                // 큰화상을 downsample하여 적재한후 편집보관시에 편집반복을 위하여 변수들 보관
                                var adjustStr = ""
                                for (adjustItem in appliedAdjusts) adjustStr += adjustItem.mConfigStr
                                val history = AppliedValueHistory()
                                history.historyType = ACTION_ADJUST
                                history.adjustConfigStr = adjustStr
                                appliedHistory.add(history)
                                appliedRedoHistory.clear()
                            }

                            makeHistory(currentBitmap!!)
                        }
                    }
                }
                // Adjust의 glSurfaceView 새로 생성
                recreateAdjustView()
            } else if (prePrimaryAction == PRIMARY_ACTION_MORE) {
                // More에서 넘어온 경우 같은 ImageView를 리용하므로 다시 화상을 설정해줄 필요 없다
                currentBitmap = default_image_view.drawable.toBitmap()
                updateFilterThumb()
                if (preBitmap != currentBitmap) preBitmap!!.recycle()
                updateUI()
            }
        }
    }

    /**
     * Filter ImageView에 설정된 비트맵을 recycle한다
     */
    private fun releaseFilterView() {
        val imageDrawable: Drawable = default_image_view.drawable
        default_image_view.setImageDrawable(null)
        if (imageDrawable != null && imageDrawable is BitmapDrawable) {
            val bitmapDrawable: BitmapDrawable = imageDrawable as BitmapDrawable
            if (!bitmapDrawable.bitmap.isRecycled) bitmapDrawable.bitmap.recycle()
        }
    }

    /**
     * Crop ImageView에 설정된 비트맵을 recycle한다
     */
    private fun releaseCropView() {
        val imageDrawable: Drawable = gesture_crop_image_view.drawable
        gesture_crop_image_view.setImageDrawable(null)
        if (imageDrawable != null && imageDrawable is FastBitmapDrawable) {
            val bitmapDrawable: FastBitmapDrawable = imageDrawable as FastBitmapDrawable
            if (!bitmapDrawable.bitmap.isRecycled) bitmapDrawable.bitmap.recycle()
        }
    }

    /**
     * 비교 ImageView에 설정된 비트맵을 recycle한다
     */
    private fun releaseCompareView() {
        val imageDrawable: Drawable = compare_image_view.drawable
        gesture_crop_image_view.setImageDrawable(null)
        if (imageDrawable != null && imageDrawable is BitmapDrawable) {
            val bitmapDrawable: BitmapDrawable = imageDrawable as BitmapDrawable
            if (!bitmapDrawable.bitmap.isRecycled) bitmapDrawable.bitmap.recycle()
        }
    }

    // Adjust의 glSurfaceView를 다시 생성하여 추가한다
    // 실험결과 glSurfaceView를 다시 생성 & 추가하여 리용하는것이 메모리절약에 더 좋다
    private fun recreateAdjustView() {
        ensureBackgroundThread {
            adjust_view.onPause()
            adjust_view.release()
            adjust_view = ImageGLSurfaceView(this, null)
            adjust_view.displayMode = ImageGLSurfaceView.DisplayMode.DISPLAY_ASPECT_FIT
            runOnUiThread {
                adjust_view_container.removeViewAt(0)
                adjust_view_container.addView(adjust_view, 0)
            }
        }
    }

    /**
     * 현재화상의 thumb화상을 얻고 그로부터 효과들의 미리보기화상들을 생성한다
     */
    private fun updateFilterThumb() {
        ensureBackgroundThread {
            val thumbSquare = getFilterThumb()
            runOnUiThread {
                val adapter = bottom_actions_filter_rv.adapter
                (adapter as FiltersAdapter).updateFilterThumb(thumbSquare)
                adapter.notifyDataSetChanged()
            }
        }
    }

    /**
     * 현재화상의 thumb화상 생성.
     */
    private fun getFilterThumb(): Bitmap {

        val thumbnailSize = resources.getDimension(R.dimen.bottom_filters_thumbnail_size).toInt();

        var newWidth = thumbnailSize
        var newHeight = thumbnailSize
        if (currentBitmap!!.width <= currentBitmap!!.height) newHeight = (thumbnailSize.toFloat() / currentBitmap!!.width * currentBitmap!!.height).toInt()
        else newWidth = (thumbnailSize.toFloat() / currentBitmap!!.height * currentBitmap!!.width).toInt()

        val thumbBmp = Bitmap.createScaledBitmap(currentBitmap!!, newWidth, newHeight, true);
        val thumbWidth = thumbBmp.width
        val thumbHeight = thumbBmp.height
        val thumbSquare: Bitmap?
        if (thumbHeight > thumbWidth) thumbSquare = Bitmap.createBitmap(thumbBmp, 0, (thumbHeight - thumbWidth) / 2, thumbWidth, thumbWidth);
        else thumbSquare = Bitmap.createBitmap(thumbBmp, (thumbWidth - thumbHeight) / 2, 0, thumbHeight, thumbHeight);

        if (thumbBmp != thumbSquare) thumbBmp.recycle()

        return thumbSquare
    }

    // 적용된 AdjustConfig 보관
    var appliedAdjusts = ArrayList<AdjustConfig>()
    /**
     *  Adjust 화면 적재
     */
    private fun loadAdjustImageView() {
        isAdjustApplied = false
        if (prePrimaryAction == PRIMARY_ACTION_CROP) {
            // Crop에서 넘어온 경우 자르기를 진행해야 하는지 검사
            if (gesture_crop_image_view.shouldCrop()) {
                cropForAction = CROP_FOR_ADJUST
                startCrop()
            } else {
                //e.g. 아래에서 releaseCropView를 호출하면 currentBitmap이 없어지므로 새로 생성
                currentBitmap = createBitmap(currentBitmap!!)
                releaseCropView()
                setAdjustImage(currentBitmap!!)
                updateUI()
            }
        } else {
            val preBitmap = currentBitmap
            if (prePrimaryAction == PRIMARY_ACTION_FILTER) {
                // Filter에서 넘어온 경우
                currentBitmap = createBitmap(default_image_view.drawable.toBitmap())
                releaseFilterView()
                setAdjustImage(currentBitmap!!)
                if (isFilterApplied) {
                    if (scaleRateWithMaster > 1f) {
                        // 큰화상을 downsample하여 적재한후 편집보관시에 편집반복을 위하여 변수들 보관
                        val history = AppliedValueHistory()
                        history.historyType = ACTION_FILTER
                        history.filterConfigStr = (bottom_actions_filter_rv.adapter as FiltersAdapter).getCurrentFilter().filter.second
                        appliedHistory.add(history)
                        appliedRedoHistory.clear()
                    }

                    makeHistory(currentBitmap!!)
                }
                if (preBitmap != currentBitmap) preBitmap!!.recycle()

            } else if (prePrimaryAction == PRIMARY_ACTION_MORE) {
                // More에서 넘어온 경우
                currentBitmap = createBitmap(default_image_view.drawable.toBitmap())
                releaseFilterView()
                setAdjustImage(currentBitmap!!)
                if (preBitmap != currentBitmap) preBitmap!!.recycle()
            }
            updateUI()
        }

        // Adjust의 Curve기능을 위한 canvas 만들기
        if (!rgb_curve_canvas.isInitialized) {
            rgb_curve_canvas.initCanvasSpliner(dpToPx(350) - 20, dpToPx(400) - 20)

            val channels = arrayOf(CURVE_CHANNEL_RGB, CURVE_CHANNEL_R, CURVE_CHANNEL_G, CURVE_CHANNEL_B)

            rgb_channel_spinner.setItems(channels.toList())
            rgb_channel_spinner.setOnSpinnerItemSelectedListener(object : OnSpinnerItemSelectedListener<String?> {
                override fun onItemSelected(oldIndex: Int, @Nullable oldItem: String?, newIndex: Int, newItem: String?) {
                    // 선택된 channel에 따라 보관되였던 점들을 canvas에 그리기
                    currentRGBChannel = channels[newIndex]

                    when (currentRGBChannel) {
                        CURVE_CHANNEL_RGB -> if (curve_points_rgb.isNotEmpty()) rgb_curve_canvas.replacePoints(curve_points_rgb)
                        else rgb_curve_canvas.resetPoints()
                        CURVE_CHANNEL_R -> if (curve_points_r.isNotEmpty()) rgb_curve_canvas.replacePoints(curve_points_r)
                        else rgb_curve_canvas.resetPoints()
                        CURVE_CHANNEL_G -> if (curve_points_g.isNotEmpty())  rgb_curve_canvas.replacePoints(curve_points_g)
                        else rgb_curve_canvas.resetPoints()
                        CURVE_CHANNEL_B -> if (curve_points_b.isNotEmpty())  rgb_curve_canvas.replacePoints(curve_points_b)
                        else rgb_curve_canvas.resetPoints()
                    }
                }
            })
        }

        // Adjust 적용목록 지우기
        appliedAdjusts.clear()
        // 효과 지우기
        adjust_view.setFilterWithConfig("")
        // 효과들의 표준값 설정, 첫번째 효과 선택
        (bottom_actions_adjust_rv.adapter as AdjustAdapter).resetItems()
        (bottom_actions_adjust_rv.adapter as AdjustAdapter).selectItem(0)

        // curve점들을 지우기
        curve_points_rgb.clear()
        curve_points_r.clear()
        curve_points_g.clear()
        curve_points_b.clear()
        // curve의 첫번째 channel 선택
        rgb_channel_spinner.selectItemByIndex(0)
        rgb_curve_canvas.resetPoints()
    }
    fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    /**
     * 파라메터로 넘어온 bitmap을 Adjust에 설정하고 0번째 item 선택
     */
    private fun setAdjustImage(bitmap: Bitmap) {
        adjust_view.setImageBitmap(bitmap)
        (bottom_actions_adjust_rv.adapter as AdjustAdapter).selectItem(0)
    }

    /**
     * More 화면 적재
     */
    private fun loadMoreImageView() {
        if (prePrimaryAction == PRIMARY_ACTION_CROP) {
            // Crop에서 넘어온 경우 자르기를 진행해야 하는지 검사
            if (gesture_crop_image_view.shouldCrop()) {
                cropForAction = CROP_FOR_MORE
                startCrop()
            } else {
                //e.g. 아래에서 releaseCropView를 호출하면 currentBitmap이 없어지므로 새로 생성
                currentBitmap = createBitmap(currentBitmap!!)
                default_image_view.setImageBitmap(currentBitmap)
                updateUI()
                releaseCropView()
            }
        } else {
            val preBitmap = currentBitmap
            if (prePrimaryAction == PRIMARY_ACTION_ADJUST) {
                // Adjust에서 넘어온 경우
                adjust_view.getResultBitmap {
                    currentBitmap = it
                    if (preBitmap != currentBitmap) preBitmap!!.recycle()
                    runOnUiThread {
                        default_image_view.setImageBitmap(currentBitmap)
                        updateUI()
                        if (isAdjustApplied) {
                            if (scaleRateWithMaster > 1f) {
                                // 큰화상을 downsample하여 적재한후 편집보관시에 편집반복을 위하여 변수들 보관
                                var adjustStr = ""
                                for (adjustItem in appliedAdjusts) adjustStr += adjustItem.mConfigStr
                                val history = AppliedValueHistory()
                                history.historyType = ACTION_ADJUST
                                history.adjustConfigStr = adjustStr
                                appliedHistory.add(history)
                                appliedRedoHistory.clear()
                            }
                            makeHistory(currentBitmap!!)
                        }
                    }
                }
                // Adjust의 glSurfaceView 새로 생성
                recreateAdjustView()
            } else if (prePrimaryAction == PRIMARY_ACTION_FILTER) {
                // Filter에서 넘어온 경우
                // Filter와 More는 같은 ImageView를 리용하므로 다시 설정할 필요가 없다
                currentBitmap = default_image_view.drawable.toBitmap()
                if (isFilterApplied) {
                    if (scaleRateWithMaster > 1f) {
                        // 큰화상을 downsample하여 적재한후 편집보관시에 편집반복을 위하여 변수들 보관
                        val history = AppliedValueHistory()
                        history.historyType = ACTION_FILTER
                        history.filterConfigStr = (bottom_actions_filter_rv.adapter as FiltersAdapter).getCurrentFilter().filter.second
                        appliedHistory.add(history)
                        appliedRedoHistory.clear()
                    }

                    makeHistory(currentBitmap!!)
                }
                if (preBitmap != currentBitmap) preBitmap!!.recycle()
                updateUI()
            }
        }

        val thisActivity = this
        btn_doodle.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                // More에서 doodle 선택하였을때
                if (mCropInProgress) return

                currDoodleAction = DOODLE_ACTION_DOODLE
                // show doodle window
                initDoodle()

                doodle_image_view.beVisible()
                default_image_view_container.beGone()

                bottom_editor_more_actions.beGone()
                bottom_editor_primary_actions.beGone()
                btn_compare.beGone()

                bottom_editor_doodle_actions.beVisible()
                bottom_close_check_bar.beVisible()
                bottom_bar_check_close_name.text = getString(R.string.doodle)

                draw_spinner_container.beGone()

                controlBarAnim(bottom_bar_container);
                mDoodle.setBrushBMPStyle(1)                   // normal brush

                // Edit창의 메뉴 감추고 doodle메뉴 보이기
                menu_edit!!.setGroupVisible(R.id.edit_menu_group, false)
                menu_edit!!.setGroupVisible(R.id.doodle_menu_group, true)
                bottom_bar_close.setOnClickListener {
                    // 곱하기기호(취소)단추를 눌렀을때 그려진것이 있으면 대화창 현시
                    if (mDoodleView.itemCount > 0) {
                        SaveChangesDialog(thisActivity, {
                            // 취소
                            doodleCancel()
                        }, {
                            // 보관
                            bottom_bar_check.performClick()
                        })
                    } else doodleCancel()
                }

                bottom_bar_check.setOnClickListener {
                    // 체크기호(확인)단추를 눌렀을때
                    currDoodleAction = DOODLE_ACTION_NONE
                    bottom_editor_doodle_actions.beGone()
                    bottom_close_check_bar.beGone()

                    bottom_editor_more_actions.beVisible()
                    bottom_editor_primary_actions.beVisible()
                    btn_compare.beVisible()

                    if (mDoodleView.isEnableZoomer) menu_edit!!.performIdentifierAction (R.id.menu_doodle_magnifier, 0)

                    controlBarAnim(bottom_bar_container);

                    // 그려진것이 있으면 결과화상 설정
                    if (mDoodleView.itemCount > 0) {
                        // 편집중의 item들을 그리기
                        mDoodleView.drawPendingItems()

                        // 편집중이면 편집단추 수동으로 누르기
                        if (mDoodleView.isEditMode) menu_edit!!.performIdentifierAction (R.id.menu_doodle_edit, 0)

                        val temp = currentBitmap
                        currentBitmap = createBitmap(mDoodleView.doodleBitmap)
                        if (temp != currentBitmap) temp!!.recycle()
                        mDoodleView.doodleBitmap.recycle()
                        default_image_view.setImageBitmap(currentBitmap)
                        // history 보관
                        makeHistory(currentBitmap!!)

                        if (scaleRateWithMaster > 1f) {
                            // 큰화상을 downsample하여 적재한후 편집보관시에 편집반복을 위하여 변수들 보관
                            val history = AppliedValueHistory()
                            history.historyType = ACTION_DOODLE
                            history.doodleItems = ArrayList(mDoodle.allItem)
                            appliedHistory.add(history)
                            appliedRedoHistory.clear()
                        }
                    }
                    doodle_image_view.beGone()
                    default_image_view_container.beVisible()

                    // doodle메뉴 감추고 Edit창의 메뉴 보이기
                    menu_edit!!.setGroupVisible(R.id.edit_menu_group, true)
                    menu_edit!!.setGroupVisible(R.id.doodle_menu_group, false)
                }
            }
        });

        btn_mosaic.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                // More에서 mosaic 선택하였을때
                if (mCropInProgress) return
                currDoodleAction = DOODLE_ACTION_MOSAIC
                // show doodle window
                initDoodle()

                doodle_image_view.beVisible()
                default_image_view_container.beGone()

                bottom_editor_more_actions.beGone()
                bottom_editor_primary_actions.beGone()
                btn_compare.beGone()

                more_mosaic_thickness_seekbar.setColors(config.textColor, getAdjustedPrimaryColor(), config.backgroundColor)
                bottom_editor_mosaic_actions.beVisible()
                bottom_close_check_bar.beVisible()
                bottom_bar_check_close_name.text = getString(R.string.doodle_bar_mosaic)
                draw_spinner_container.beGone()

                controlBarAnim(bottom_bar_container);
                mDoodle.setBrushBMPStyle(1)                          // normal brush

                // Edit창의 메뉴 감추고 doodle메뉴 보이기
                menu_edit!!.setGroupVisible(R.id.edit_menu_group, false)
                menu_edit!!.setGroupVisible(R.id.doodle_menu_group, true)
                bottom_bar_close.setOnClickListener {
                    // 곱하기기호(취소)단추를 눌렀을때 그려진것이 있으면 대화창 현시
                    if (mDoodleView.itemCount > 0) {
                        SaveChangesDialog(thisActivity, {
                            // 취소
                            mosaicCancel()
                        }, {
                            // 보관
                            bottom_bar_check.performClick()
                        })
                    } else mosaicCancel()
                }

                bottom_bar_check.setOnClickListener {
                    // 체크기호(확인)단추를 눌렀을때
                    currDoodleAction = DOODLE_ACTION_NONE
                    bottom_editor_mosaic_actions.beGone()
                    bottom_close_check_bar.beGone()

                    bottom_editor_more_actions.beVisible()
                    bottom_editor_primary_actions.beVisible()
                    btn_compare.beVisible()

                    if (mDoodleView.isEnableZoomer) menu_edit!!.performIdentifierAction (R.id.menu_doodle_magnifier, 0)

                    controlBarAnim(bottom_bar_container);

                    // 그려진것이 있으면 결과화상 설정
                    if (mDoodleView.itemCount > 0) {
                        // 편집중의 item들을 그리기
                        mDoodleView.drawPendingItems()
                        // 편집중이면 편집단추 수동으로 누르기
                        if (mDoodleView.isEditMode) menu_edit!!.performIdentifierAction (R.id.menu_doodle_edit, 0)

                        val temp = currentBitmap
                        currentBitmap = createBitmap(mDoodleView.doodleBitmap)
                        if (temp != currentBitmap) temp!!.recycle()
                        mDoodleView.doodleBitmap.recycle()
                        default_image_view.setImageBitmap(currentBitmap)
                        // history 보관
                        makeHistory(currentBitmap!!)

                        if (scaleRateWithMaster > 1f) {
                            // 큰화상을 downsample하여 적재한후 편집보관시에 편집반복을 위하여 변수들 보관
                            val history = AppliedValueHistory()
                            history.historyType = ACTION_MOSAIC
                            history.mosaicItems = ArrayList(mDoodle.allItem)
                            appliedHistory.add(history)
                            appliedRedoHistory.clear()
                        }
                    }

                    doodle_image_view.beGone()
                    default_image_view_container.beVisible()
                    // doodle메뉴 감추고 Edit창의 메뉴 보이기
                    menu_edit!!.setGroupVisible(R.id.edit_menu_group, true)
                    menu_edit!!.setGroupVisible(R.id.doodle_menu_group, false)
                }
            }
        })

        btn_draw.setOnClickListener(object : View.OnClickListener{
            override fun onClick(view: View?) {
                // draw
                if (mCropInProgress) return
                currDoodleAction = DOODLE_ACTION_DRAW
                // show doodle window
                initDoodle()

                doodle_image_view.beVisible()

                default_image_view_container.beGone()

                bottom_editor_more_actions.beGone()
                bottom_editor_primary_actions.beGone()
                btn_compare.beGone()

                bottom_close_check_bar.beVisible()
                bottom_bar_check_close_name.beGone()
                draw_spinner_container.beVisible()

                controlBarAnim(bottom_bar_container);
                // Edit창의 메뉴 감추고 doodle메뉴 보이기
                menu_edit!!.setGroupVisible(R.id.edit_menu_group, false)
                menu_edit!!.setGroupVisible(R.id.doodle_menu_group, true)
                // set draw-menu select listener
                val menuArray = resources.getStringArray(R.array.more_draw_menu)
                val staticAdapter = ArrayAdapter.createFromResource(view!!.context, R.array.more_draw_menu,
                        android.R.layout.simple_spinner_item)
                staticAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                bottom_bar_menu?.adapter = staticAdapter;

                var last_spinner_pos = 0
                // call setSelection, because need one selection at first.
                // if not call, automatically call 0
                bottom_bar_menu.setSelection(0)
                bottom_bar_menu?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}

                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position < 3) {
                            last_spinner_pos = position
                            arrayOf(bottom_more_draw_style, bottom_more_draw_size, bottom_more_draw_color).forEach {
                                it?.beGone()
                            }
                        }
                        when (position) {
                            0 -> {
                                // style selected
                                bottom_more_draw_style.beVisible()
                                // set saved style
                                currMoreDrawStyle = config.lastEditorBrushStyle
                                highlightDrawStyleButtonBorder()
                                spinner_text.text = menuArray[0]
                            }
                            1 -> {
                                // size selected
                                bottom_more_draw_size.beVisible()
                                more_draw_thickness_seekbar.setColors(config.textColor, getAdjustedPrimaryColor(), config.backgroundColor)
                                more_draw_thickness_seekbar.progress = config.lastEditorBrushSize
                                spinner_text.text = menuArray[1]
                            }
                            2 -> {
                                // color selected
                                bottom_more_draw_color.beVisible()

                                // select saved color
                                when (config.lastEditorDrawColorNum) {
                                    0 -> {
                                        currMoreDrawColor = MORE_DRAW_COLOR_0
                                    }
                                    1 -> {
                                        currMoreDrawColor = MORE_DRAW_COLOR_1
                                    }
                                    2 -> {
                                        currMoreDrawColor = MORE_DRAW_COLOR_2
                                    }
                                    3 -> {
                                        currMoreDrawColor = MORE_DRAW_COLOR_3
                                    }
                                    4 -> {
                                        currMoreDrawColor = MORE_DRAW_COLOR_4
                                    }
                                }
                                highlightDrawColorButtonBorder()
                                spinner_text.text = menuArray[2]
                            }
                            3 -> {
                                // clear
                                mDoodle.clear()
                                bottom_bar_menu.setSelection(last_spinner_pos)
                            }
                        }
                    }
                }

                // set saved brush color/size
                when (config.lastEditorDrawColorNum) {
                    0 -> {
                        updateDrawColor(MORE_DRAW_COLOR_0)
                    }
                    1 -> {
                        updateDrawColor(MORE_DRAW_COLOR_1)
                    }
                    2 -> {
                        updateDrawColor(MORE_DRAW_COLOR_2)
                    }
                    3 -> {
                        updateDrawColor(MORE_DRAW_COLOR_3)
                    }
                    4 -> {
                        updateDrawColor(MORE_DRAW_COLOR_4)
                    }
                }

                bottom_bar_close.setOnClickListener {
                    // 곱하기기호(취소)단추를 눌렀을때 그려진것이 있으면 대화창 현시
                    if (mDoodleView.itemCount > 0) {
                        SaveChangesDialog(thisActivity, {
                            // 취소
                            drawCancel()
                        }, {
                            // 보관
                            bottom_bar_check.performClick()
                        })
                    } else drawCancel()
                }

                bottom_bar_check.setOnClickListener {
                    // 체크기호(확인)단추를 눌렀을때
                    currDoodleAction = DOODLE_ACTION_NONE
                    bottom_close_check_bar.beGone()
                    bottom_more_draw_style.beGone()
                    bottom_more_draw_size.beGone()
                    bottom_more_draw_color.beGone()

                    bottom_editor_more_actions.beVisible()
                    bottom_editor_primary_actions.beVisible()
                    btn_compare.beVisible()

                    if (mDoodleView.isEnableZoomer) menu_edit!!.performIdentifierAction (R.id.menu_doodle_magnifier, 0)

                    controlBarAnim(bottom_bar_container);

                    // 그려진것이 있으면 결과화상 설정
                    if (mDoodleView.itemCount > 0) {
                        // 편집중의 item을 그리기
                        mDoodleView.drawPendingItems()
                        // 편집중이면 편집단추 수동으로 누르기
                        if (mDoodleView.isEditMode) menu_edit!!.performIdentifierAction (R.id.menu_doodle_edit, 0)

                        val temp = currentBitmap
                        currentBitmap = createBitmap(mDoodleView.doodleBitmap)
                        if (temp != currentBitmap) temp!!.recycle()
                        mDoodleView.doodleBitmap.recycle()
                        default_image_view.setImageBitmap(currentBitmap)
                        // history 보관
                        makeHistory(currentBitmap!!)
                        if (scaleRateWithMaster > 1f) {
                            // 큰화상을 downsample하여 적재한후 편집보관시에 편집반복을 위하여 변수들 보관
                            val history = AppliedValueHistory()
                            history.historyType = ACTION_DRAW
                            history.drawItems = ArrayList(mDoodle.allItem)
                            appliedHistory.add(history)
                            appliedRedoHistory.clear()
                        }
                    }

                    doodle_image_view.beGone()
                    default_image_view_container.beVisible()
                    // doodle메뉴 감추고 Edit창의 메뉴 보이기
                    menu_edit!!.setGroupVisible(R.id.edit_menu_group, true)
                    menu_edit!!.setGroupVisible(R.id.doodle_menu_group, false)
                }
            }
        })
    }

    /**
     * doodle을 보관취소하였을때
     */
    private fun doodleCancel() {
        currDoodleAction = DOODLE_ACTION_NONE
        bottom_editor_doodle_actions.beGone()
        bottom_close_check_bar.beGone()

        bottom_editor_more_actions.beVisible()
        bottom_editor_primary_actions.beVisible()
        btn_compare.beVisible()

        doodle_image_view.beGone()
        default_image_view_container.beVisible()

        if (mDoodleView.isEditMode) menu_edit!!.performIdentifierAction(R.id.menu_doodle_edit, 0)
        if (mDoodleView.isEnableZoomer) menu_edit!!.performIdentifierAction(R.id.menu_doodle_magnifier, 0)

        controlBarAnim(bottom_bar_container);
        menu_edit!!.setGroupVisible(R.id.edit_menu_group, true)
        menu_edit!!.setGroupVisible(R.id.doodle_menu_group, false)
    }

    /**
     * mosaic를 보관취소하였을때
     */
    private fun mosaicCancel() {
        currDoodleAction = DOODLE_ACTION_NONE
        bottom_editor_mosaic_actions.beGone()
        bottom_close_check_bar.beGone()

        bottom_editor_more_actions.beVisible()
        bottom_editor_primary_actions.beVisible()
        btn_compare.beVisible()

        doodle_image_view.beGone()
        default_image_view_container.beVisible()

        if (mDoodleView.isEditMode) menu_edit!!.performIdentifierAction (R.id.menu_doodle_edit, 0)
        if (mDoodleView.isEnableZoomer) menu_edit!!.performIdentifierAction (R.id.menu_doodle_magnifier, 0)

        controlBarAnim(bottom_bar_container);
        menu_edit!!.setGroupVisible(R.id.edit_menu_group, true)
        menu_edit!!.setGroupVisible(R.id.doodle_menu_group, false)
    }

    /**
     * draw를 보관취소하였을때
     */
    private fun drawCancel() {
        currDoodleAction = DOODLE_ACTION_NONE
        bottom_close_check_bar.beGone()
        bottom_more_draw_style.beGone()
        bottom_more_draw_size.beGone()
        bottom_more_draw_color.beGone()

        bottom_editor_more_actions.beVisible()
        bottom_editor_primary_actions.beVisible()
        btn_compare.beVisible()

        if (mDoodleView.isEditMode) menu_edit!!.performIdentifierAction (R.id.menu_doodle_edit, 0)
        if (mDoodleView.isEnableZoomer) menu_edit!!.performIdentifierAction (R.id.menu_doodle_magnifier, 0)

        controlBarAnim(bottom_bar_container);

        doodle_image_view.beGone()
        default_image_view_container.beVisible()
        menu_edit!!.setGroupVisible(R.id.edit_menu_group, true)
        menu_edit!!.setGroupVisible(R.id.doodle_menu_group, false)
    }

    /**
     * 자르기 화면 적재
     */
    private fun loadCropImageView() {
        if (currentBitmap == null) {
            // 초기화상을 불러들이기 전이다
            progress_spinner.smoothToShow()
            loadBitmapFromUri()
            gesture_crop_image_view.setOnCropImageCompleteListener(this@EditActivity)
            gesture_crop_image_view.setKeepAspectRatio(false);
            updateUI()
        } else  {
            val preBitmap = currentBitmap
            if (prePrimaryAction == PRIMARY_ACTION_FILTER) {
                //Filter에서 넘어온 후
                currentBitmap = createBitmap(default_image_view.drawable.toBitmap())
                if (isFilterApplied) {
                    if (scaleRateWithMaster > 1f) {
                        // 큰화상을 downsample하여 적재한후 편집보관시에 편집반복을 위하여 변수들 보관
                        val history = AppliedValueHistory()
                        history.historyType = ACTION_FILTER
                        history.filterConfigStr = (bottom_actions_filter_rv.adapter as FiltersAdapter).getCurrentFilter().filter.second
                        appliedHistory.add(history)
                        appliedRedoHistory.clear()
                    }

                    makeHistory(currentBitmap!!)
                }
                gesture_crop_image_view.setImageBitmap(currentBitmap)
                updateUI()
                if (preBitmap != currentBitmap) preBitmap!!.recycle()
                releaseFilterView()
            } else if (prePrimaryAction == PRIMARY_ACTION_ADJUST) {
                // Adjust에서 넘어온 후
                adjust_view.getResultBitmap {
                    currentBitmap = it
                    if (preBitmap != currentBitmap) preBitmap!!.recycle()
                    runOnUiThread {
                        gesture_crop_image_view.setImageBitmap(currentBitmap)
                        updateUI()
                        if (isAdjustApplied) {
                            if (scaleRateWithMaster > 1f) {
                                // 큰화상을 downsample하여 적재한후 편집보관시에 편집반복을 위하여 변수들 보관
                                var adjustStr = ""
                                for (adjustItem in appliedAdjusts) adjustStr += adjustItem.mConfigStr
                                val history = AppliedValueHistory()
                                history.historyType = ACTION_ADJUST
                                history.adjustConfigStr = adjustStr
                                appliedHistory.add(history)
                                appliedRedoHistory.clear()
                            }

                            makeHistory(currentBitmap!!)
                        }
                    }
                }
                recreateAdjustView()
            } else if (prePrimaryAction == PRIMARY_ACTION_MORE){
                // More에서 넘어온 후
                currentBitmap = createBitmap(default_image_view.drawable.toBitmap())
                gesture_crop_image_view.setImageBitmap(currentBitmap)
                updateUI()
                if (preBitmap != currentBitmap) preBitmap!!.recycle()
                releaseFilterView()
            }
            crop_view_overlay.setOriginCropRect(null)                           // CropImageView onLayout() -> onImageLaidOut()이 호출되면서 setOriginCropRect가 다시 설정된다.
            // 자유비률 설정
            (crop_rv.adapter as CropSelectorRVAdapter).selectItem(0, false)
        }

        // 자르기화면에서 화상이 기울어졌을때 나타나는 Reset단추의 click listener
        rotate_reset.setOnClickListener {
            val resetAngle = getResetAngle();
            // 화상의 확대축소비률 초기화
            if (gesture_crop_image_view.isPortrait) crop_view_overlay.setCurrentImageScale(gesture_crop_image_view.originScalePortrait)
            else crop_view_overlay.setCurrentImageScale(gesture_crop_image_view.originScaleLandscape)

            // 화상을 바로 돌리기
            gesture_crop_image_view.postRotate(resetAngle)
            // 화상이 초기령역을 포함하도록 한다
            gesture_crop_image_view.setImageToWrapOriginBounds()
            rotate_reset.beGone()
            straight_ruler.reset()

            // 자르기구역을 현재 비률로 재설정
            updateAspectRatio()
        }
    }

    /**
     * Reset단추를 눌렀을때 돌아가야할 각도 구하기
     */
    private fun getResetAngle(): Float {
        var resetAngle = gesture_crop_image_view.currentAngle;
        val scaleX = gesture_crop_image_view.currentScaleX;
        val scaleY = gesture_crop_image_view.currentScaleY;

        if (gesture_crop_image_view.isPortrait) {
            if (scaleX > 0) {
                if (scaleY > 0) resetAngle *= -1
            } else {
                resetAngle = if (scaleY > 0) {
                    if (resetAngle > 0) resetAngle - 180f
                    else 180f + resetAngle
                } else {
                    if (resetAngle > 0) 180f - resetAngle
                    else -180f - resetAngle
                }
            }
        } else {
            if (scaleX > 0) {
                resetAngle = if (scaleY > 0) {
                    if (resetAngle > 0) 90f - resetAngle
                    else -90f - resetAngle
                } else {
                    if (resetAngle > 0) resetAngle - 90f
                    else 90f + resetAngle
                }
            } else {
                resetAngle = if (scaleY > 0) {
                    if (resetAngle > 0) resetAngle - 90f
                    else 90f + resetAngle
                } else {
                    if (resetAngle > 0) 90f - resetAngle
                    else -90f - resetAngle
                }
            }
        }
        return resetAngle;
    }

    // 원본화상과의 비률이다
    // 화상의 크기가 6MP을 넘으면 1f보다 크고 작으면 1f
    private var scaleRateWithMaster = 1f

    /**
     * Glide를 리용하여 초기 화상을 uri로부터 적재한다
     */
    private fun loadBitmapFromUri() {
        // save original bitmap
        if (currentBitmap == null) {

            // 화일경로를 리용하여 화상의 크기를 얻는다
            val fileDirItem = realFilePath.let { FileDirItem(it, realFilePath.getFilenameFromPath(), getIsPathDirectory(realFilePath)) }
            val size = fileDirItem.getResolution(this)

            // Glide 설정
            val options = RequestOptions()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)

            if (size!!.x * size.y > 6000000) {
                val squreRate = size.x * size.y / 6000000f
                scaleRateWithMaster = sqrt(squreRate.toDouble()).toFloat()
                options.override((size.x / scaleRateWithMaster).toInt(), (size.y / scaleRateWithMaster).toInt())
            } else options.downsample(DownsampleStrategy.NONE)

            ensureBackgroundThread {
                // Glide 적재
                Glide.with(this)
                    .asBitmap()
                    .apply(options)
                    .load(uri).listener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                            showErrorToast(e.toString())
                            return false
                        }

                        override fun onResourceReady(originBmp: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            currentBitmap = originBmp!!

                            runOnUiThread {
                                // 화상을 적재한후 Filter항목들의 RecyclerView Adapter 생성
                                // 여기서 화상 thumb를 생성하기 때문이다
                                if (bottom_actions_filter_rv.adapter == null) {
                                    generateFilterRVAdapter()
                                }

                                // 첫화상을 history로 보관
                                makeHistory(currentBitmap!!)
                                // 비교화상 설정
                                compare_image_view.setImageBitmap(createBitmap(originBmp))
                                // 자르기화상 설정
                                gesture_crop_image_view.setImageBitmap(currentBitmap)
                                // Adjust의 현시방식 설정
                                adjust_view.displayMode = ImageGLSurfaceView.DisplayMode.DISPLAY_ASPECT_FIT

                                progress_spinner.smoothToHide()
                                // 화상이 적재되는 동안 다른 동작을 하지 못하게 하였던 투명View를 없애기
                                bottom_bar_cover.beGone()
                            }

                            return false
                        }
                    }).submit().get()
            }
        }
    }

    /**
     * 화상을 보관하기 위한 준비 처리 진행
     * 편집한 화상이 6MP 보다 큰 화상일때에는 원본화상을 적재하고 다시그리기 함수를 호출한다
     */
    @TargetApi(Build.VERSION_CODES.N)
    private fun saveImage() {
        // 화상이 보관되는 동안 다른 동작을 하지 못하게 투명View로 덮기
        bottom_bar_cover.beVisible()
        var inputStream: InputStream? = null
        try {
            if (isNougatPlus()) {
                inputStream = contentResolver.openInputStream(uri!!)
                oldExif = ExifInterface(inputStream!!)
            }
        } catch (e: Exception) {
        } finally {
            inputStream?.close()
        }

        if (scaleRateWithMaster == 1f) {
            if (currPrimaryAction == PRIMARY_ACTION_CROP) {
                // 자르기 화면일때에는 먼저 자르기를 진행한다
                cropForAction = CROP_FOR_SAVE
                gesture_crop_image_view.getCroppedImageAsync(gesture_crop_image_view.viewBitmap)
            } else if (currPrimaryAction == PRIMARY_ACTION_FILTER || currPrimaryAction == PRIMARY_ACTION_MORE) {
                val bitmap = default_image_view.drawable.toBitmap()
                saveBitmap(bitmap!!)
            } else if (currPrimaryAction == PRIMARY_ACTION_ADJUST) {
                adjust_view.getResultBitmap { saveBitmap(it) }
            }
        } else {
            // 화상이 downsample 되였다
            if (currPrimaryAction == PRIMARY_ACTION_CROP) {
                // 큰화상을 downsample하여 적재한후 편집보관시에 편집반복을 위하여 변수들 보관
                val history = AppliedValueHistory()
                history.historyType = ACTION_CROP
                history.cropAngle = gesture_crop_image_view.currentAngle
                history.cropScaleX = gesture_crop_image_view.currentScaleX / gesture_crop_image_view.initialScale
                history.cropScaleY = gesture_crop_image_view.currentScaleY / gesture_crop_image_view.initialScale
                history.cropRectInWrapper = Rect(gesture_crop_image_view.cropRectInWrapper)
                appliedHistory.add(history)
                appliedRedoHistory.clear()
            } else if(currPrimaryAction == PRIMARY_ACTION_FILTER) {
                if (isFilterApplied) {
                    // 큰화상을 downsample하여 적재한후 편집보관시에 편집반복을 위하여 변수들 보관
                    val history = AppliedValueHistory()
                    history.historyType = ACTION_FILTER
                    history.filterConfigStr = (bottom_actions_filter_rv.adapter as FiltersAdapter).getCurrentFilter().filter.second
                    appliedHistory.add(history)
                    appliedRedoHistory.clear()
                }
            } else if(currPrimaryAction == PRIMARY_ACTION_ADJUST) {
                if (isAdjustApplied) {
                    // 큰화상을 downsample하여 적재한후 편집보관시에 편집반복을 위하여 변수들 보관
                    var adjustStr = ""
                    for( adjustItem in appliedAdjusts) adjustStr += adjustItem.mConfigStr
                    val history = AppliedValueHistory()
                    history.historyType = ACTION_ADJUST
                    history.adjustConfigStr = adjustStr
                    appliedHistory.add(history)
                    appliedRedoHistory.clear()
                }
            }
            progress_spinner.smoothToShow()
            ensureBackgroundThread {
                // downsample을 하지않은 화상을 그대로 적재한다
                val options = RequestOptions()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .downsample(DownsampleStrategy.NONE)

                Glide.with(this)
                        .asBitmap()
                        .apply(options)
                        .load(uri).listener(object : RequestListener<Bitmap> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                                showErrorToast(e.toString())
                                return false
                            }

                            override fun onResourceReady(originBmp: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                originalBitmap = originBmp
                                repeatHistoryForMaster()
                                return false
                            }
                        }).submit().get()
            }
        }
    }

    /**
     * 보관된 {@link appliedHistory}를 리용하여 원본화상에서 편집 진행
     */
    private fun repeatHistoryForMaster() {
        if (appliedHistory.size == 0) {
            saveBitmap(originalBitmap!!)
            return
        }
        val history = appliedHistory[0]

        when(history.historyType)  {
            ACTION_CROP-> {
                val cropListener = CropImageView.OnCropImageCompleteListener { result ->
                    originalBitmap = result

                    // 편집할 history가 있으면 계속하고 없으면 보관
                    if (appliedHistory.size > 1) {
                        appliedHistory.removeAt(0)
                        repeatHistoryForMaster()
                    } else saveBitmap(originalBitmap!!)
                }

                // 자르기구역 생성
                val rect = history.cropRectInWrapper
                rect.set(
                        (rect.left * scaleRateWithMaster).toInt(),
                        (rect.top * scaleRateWithMaster).toInt(),
                        (rect.right * scaleRateWithMaster).toInt(),
                        (rect.bottom * scaleRateWithMaster).toInt())

                val angle = history.cropAngle
                val scaleX = history.cropScaleX
                val scaleY = history.cropScaleY

                // 자르기 진행후의 callback설정
                gesture_crop_image_view.setOnCropImageCompleteListener(cropListener)
                // 자르기 진행
                gesture_crop_image_view.getMasterCroppedImageAsync(originalBitmap, rect, angle, scaleX, scaleY)
            }
            ACTION_FILTER -> {
                // Filter 적용
                val ruleString = history.filterConfigStr
                originalBitmap = CGENativeLibrary.filterImage_MultipleEffects(originalBitmap, ruleString, 1f)

                // 편집할 history가 있으면 계속하고 없으면 보관
                if (appliedHistory.size > 1) {
                    appliedHistory.removeAt(0)
                    repeatHistoryForMaster()
                } else saveBitmap(originalBitmap!!)
            }

            ACTION_ADJUST -> {
                // Adjust 적용
                val ruleString = history.adjustConfigStr
                originalBitmap = CGENativeLibrary.filterImage_MultipleEffects(originalBitmap, ruleString, 1f)

                // 편집할 history가 있으면 계속하고 없으면 보관
                if (appliedHistory.size > 1) {
                    appliedHistory.removeAt(0)
                    repeatHistoryForMaster()
                } else saveBitmap(originalBitmap!!)
            }

            ACTION_DOODLE -> {
                val mDoodleBitmapCanvas = Canvas(originalBitmap!!)

                // doodleItems 를 순환하면서 canvas에 다시 그려주기
                val items = history.doodleItems
                for (item in items) {
                    val location = item.location

                    // item의 boundingRect에서 location은 top-left, pivot는 중심점이다.
                    val width = (item.pivotX - location.x) * 2
                    val height = (item.pivotY - location.y) * 2
                    item.setLocation(location.x * scaleRateWithMaster + width * scaleRateWithMaster / 2 - width / 2, location.y * scaleRateWithMaster + height * scaleRateWithMaster / 2 - height / 2, true)
                    item.pivotX = item.location.x + width / 2
                    item.pivotY = item.location.y + height / 2
                    item.scale = item.scale * scaleRateWithMaster
                    item.draw(mDoodleBitmapCanvas) // set current brush bitmap
                }

                // 편집할 history가 있으면 계속하고 없으면 보관
                if (appliedHistory.size > 1) {
                    appliedHistory.removeAt(0)
                    repeatHistoryForMaster()
                } else saveBitmap(originalBitmap!!)
            }
            ACTION_MOSAIC -> {
                val mDoodleBitmapCanvas = Canvas(originalBitmap!!)

                // doodleItems 를 순환하면서 canvas에 다시 그려주기
                val items = history.mosaicItems
                for (item in items) {
                    val location = item.location

                    // item의 boundingRect에서 location은 top-left, pivot는 중심점이다.
                    val width = (item.pivotX - location.x) * 2
                    val height = (item.pivotY - location.y) * 2

                    item.setLocation(location.x * scaleRateWithMaster + width * scaleRateWithMaster / 2 - width / 2, location.y * scaleRateWithMaster + height * scaleRateWithMaster / 2 - height / 2, true)
                    item.pivotX = item.location.x + width / 2
                    item.pivotY = item.location.y + height / 2
                    item.scale = item.scale * scaleRateWithMaster
                    item.draw(mDoodleBitmapCanvas) // set current brush bitmap
                }

                // 편집할 history가 있으면 계속하고 없으면 보관
                if (appliedHistory.size > 1) {
                    appliedHistory.removeAt(0)
                    repeatHistoryForMaster()
                } else saveBitmap(originalBitmap!!)
            }
            ACTION_DRAW -> {
                val mDoodleBitmapCanvas = Canvas(originalBitmap!!)

                // doodleItems 를 순환하면서 canvas에 다시 그려주기
                val items = history.drawItems
                for (item in items) {
                    val location = item.location

                    // item의 boundingRect에서 location은 top-left, pivot는 중심점이다.
                    val width = (item.pivotX - location.x) * 2
                    val height = (item.pivotY - location.y) * 2

                    item.setLocation(location.x * scaleRateWithMaster + width * scaleRateWithMaster / 2 - width / 2, location.y * scaleRateWithMaster + height * scaleRateWithMaster / 2 - height / 2, true)
                    item.pivotX = item.location.x + width / 2
                    item.pivotY = item.location.y + height / 2
                    item.scale = item.scale * scaleRateWithMaster
                    item.draw(mDoodleBitmapCanvas) // set current brush bitmap
                }

                // 편집할 history가 있으면 계속하고 없으면 보관
                if (appliedHistory.size > 1) {
                    appliedHistory.removeAt(0)
                    repeatHistoryForMaster()
                } else saveBitmap(originalBitmap!!)
            }
        }
    }

    /**
     * 새로운 보관경로를 생성하고 OutputStream 생성함수를 (saveBitmapToFile) 호출한다
     * @param bitmap 보관하려는 bitmap
     */
    private fun saveBitmap(bitmap: Bitmap) {
        val savePath = getNewFilePath()
        ensureBackgroundThread {
            try {
                saveBitmapToFile(bitmap, savePath)
            } catch (e: OutOfMemoryError) {
                toast(R.string.out_of_memory_error)
            }
        }
    }

    private fun setupBottomActions() {
        setupPrimaryActionButtons()
        setupCropActionButtons()
        setupMoreDrawButtons()
    }

    /**
     *  Crop/Filter/Adjust/More tab들의 listener 설정
     */
    private fun setupPrimaryActionButtons() {
        bottom_primary_crop.setOnClickListener {
            bottomCropClicked()
        }

        bottom_primary_filter.setOnClickListener {
            bottomFilterClicked()
        }

        bottom_primary_adjust.setOnClickListener {
            bottomAdjustClicked()
        }

        bottom_primary_more.setOnClickListener {
            bottomMoreClicked()
        }
    }

    /**
     *  Filter 선택
     */
    private fun bottomFilterClicked() {
        // Filter가 선택되였다는 기발 설정
        currPrimaryAction =  PRIMARY_ACTION_FILTER
        updatePrimaryActionButtons()
    }

    /**
     *  Crop 선택
     */
    private fun bottomCropClicked() {
        // Crop이 선택되였다는 기발 설정
        currPrimaryAction = PRIMARY_ACTION_CROP
        updatePrimaryActionButtons()
    }

    /**
     *  Adjust 선택
     */
    private fun bottomAdjustClicked() {
        // Adjust 선택되였다는 기발 설정
        currPrimaryAction =  PRIMARY_ACTION_ADJUST
        updatePrimaryActionButtons()
    }

    /**
     *  More 선택
     */
    private fun bottomMoreClicked() {
        // More가 선택되였다는 기발 설정
        currPrimaryAction = PRIMARY_ACTION_MORE
        updatePrimaryActionButtons()
    }

    // Crop의 회전 혹은 뒤집기동작이 진행중인가를 나탸낸다
    var mIsRotateOrFlipAnimating = false;
    /**
     *  Crop의 회전/뒤집기 동작 설정
     */
    private fun setupCropActionButtons() {
        // Crop의 회전단추를 눌렀을때
        crop_rotate.setOnClickListener {
            if (!mIsRotateOrFlipAnimating) {
                var deltaScale = 1f;
                var deltaX = 0f;
                var deltaY = 0f;
                val startOriginRect = RectF()
                val endOriginRect = RectF()

                val aspectRatio = if (currAspectRatio.first < 0 && currAspectRatio.second < 0 ) { -1f }
                else { currAspectRatio.first / currAspectRatio.second }

                mAnimator.addAnimatorListener(object : SimpleValueAnimatorListener {
                    override fun onAnimationStarted() {
                        // 현재 화상의 matrix 보관
                        gesture_crop_image_view.tempMatrix()

                        // 확대/축소되여야할 delta계산
                        val currentScale = gesture_crop_image_view.currentScale
                        if (gesture_crop_image_view.isPortrait) deltaScale = (gesture_crop_image_view.originScalePortrait / currentScale) - 1
                        else deltaScale = (gesture_crop_image_view.originScaleLandscape / currentScale) - 1

                        // 수직/수평으로 움직여야할 delta계산
                        val initialCenter = RectUtils.getCenterFromRect(gesture_crop_image_view.maxRect)
                        val currentCenter = gesture_crop_image_view.currentImageCenter

                        deltaX = (initialCenter[0] - currentCenter[0] - gesture_crop_image_view.paddingLeft)
                        deltaY = (initialCenter[1] - currentCenter[1] - gesture_crop_image_view.paddingTop)

                        // 자르기구역 animation의 시작구역 설정
                        startOriginRect.set(crop_view_overlay.noPaddingCropViewRect)
                        // 자르기구역 animation의 마감구역 계산에 리용되는 origin(최대) 구역 설정
                        if (gesture_crop_image_view.isPortrait) endOriginRect.set(gesture_crop_image_view.originRectLandscape)
                        else endOriginRect.set(gesture_crop_image_view.originRectPortrait)

                        // 회전 진행중 기발 true 설정
                        mIsRotateOrFlipAnimating = true;
                    }

                    override fun onAnimationUpdated(scale: Float) {
                        // 회전진행
                        gesture_crop_image_view.rotate(scale);

                        // 확대/축소 진행
                        gesture_crop_image_view.scale(1f + deltaScale * scale, 1f + deltaScale * scale)

                        // 수직/수평 움직이기 진행
                        gesture_crop_image_view.postTranslate(deltaX, deltaY)

                        // 자르기구역의 animation 진행
                        crop_view_overlay.animateCropViewRect(startOriginRect, endOriginRect, scale, aspectRatio, true, false)
                    }

                    override fun onAnimationFinished() {
                        gesture_crop_image_view.orientationChanged()

                        // 회전후 초기 구역과 자르기구역을 다시 설정
                        if (gesture_crop_image_view.isPortrait) crop_view_overlay.setOriginCropRect(gesture_crop_image_view.originRectPortrait)
                        else crop_view_overlay.setOriginCropRect(gesture_crop_image_view.originRectLandscape)

                        // 선택되여 있는 자르기비률로 령역 재설정
                        updateAspectRatio()

                        // 회전 진행중 기발 false 설정
                        mIsRotateOrFlipAnimating = false
                    }
                })
                mAnimator.startAnimation(200)
            }
        }

        // crop의 뒤집기단추를 눌렀을때
        crop_flip.setOnClickListener {
            if (!mIsRotateOrFlipAnimating) {
                var flipX = false
                var deltaScale = 1f
                var deltaX = 0f;
                var deltaY = 0f;

                val startOriginRect = RectF()
                val endOriginRect = RectF()
                val aspectRatio = if (currAspectRatio.first < 0 && currAspectRatio.second < 0 ) { -1f }
                else { currAspectRatio.first / currAspectRatio.second }

                mAnimator.addAnimatorListener(object : SimpleValueAnimatorListener {
                    override fun onAnimationStarted() {
                        // 뒤집기 진행중 기발 true 설정
                        mIsRotateOrFlipAnimating = true;
                        // 현재 화상의 matrix 보관
                        gesture_crop_image_view.tempMatrix()

                        // 회전축 결정
                        flipX = (gesture_crop_image_view.currentScaleX > 0 && gesture_crop_image_view.currentScaleY > 0) ||
                                (gesture_crop_image_view.currentScaleX < 0 && gesture_crop_image_view.currentScaleY < 0)

                        // 확대/축소되여야할 delta계산
                        deltaScale = if (gesture_crop_image_view.isPortrait) gesture_crop_image_view.findScaleToWrapRectBound(gesture_crop_image_view.originRectPortrait) - 1
                        else gesture_crop_image_view.findScaleToWrapRectBound(gesture_crop_image_view.originRectLandscape) - 1

                        // 회전축에 따라 수직/수평으로 움직여야할 delta계산
                        val initialCenter = RectUtils.getCenterFromRect(gesture_crop_image_view.maxRect)
                        val currentCenter = gesture_crop_image_view.currentImageCenter

                        if (flipX) {
                            deltaX = - initialCenter[0] + currentCenter[0] + gesture_crop_image_view.paddingLeft
                            deltaY = initialCenter[1] - currentCenter[1] - gesture_crop_image_view.paddingTop
                        } else {
                            deltaX = initialCenter[0] - currentCenter[0] - gesture_crop_image_view.paddingLeft
                            deltaY = -initialCenter[1] + currentCenter[1] + gesture_crop_image_view.paddingTop
                        }

                        // 자르기구역 animation의 시작구역 설정
                        startOriginRect.set(crop_view_overlay.noPaddingCropViewRect)
                        // 자르기구역 animation의 마감구역 계산에 리용되는 origin(최대) 구역 설정
                        if (gesture_crop_image_view.isPortrait) endOriginRect.set(gesture_crop_image_view.originRectPortrait)
                        else endOriginRect.set(gesture_crop_image_view.originRectLandscape)
                    }

                    override fun onAnimationUpdated(scale: Float) {
                        gesture_crop_image_view.resetMatrixByTemp();
                        if (flipX) gesture_crop_image_view.scale(1 - (2 + deltaScale) * scale, 1f + deltaScale * scale)
                        else gesture_crop_image_view.scale(1f + deltaScale * scale, 1 - (2 + deltaScale) * scale)

                        gesture_crop_image_view.postTranslate(deltaX * scale * (deltaScale + 1), deltaY * scale * (deltaScale + 1))

                        // animate crop rect
                        crop_view_overlay.animateCropViewRect(startOriginRect, endOriginRect, scale, aspectRatio, true, false)
                    }

                    override fun onAnimationFinished() {
                        // 선택되여 있는 자르기비률로 령역 재설정
                        updateAspectRatio()

                        // 수평맞추기의 눈금을 Flip된데 맞게 설정해야 한다.
                        val resetAngle = getResetAngle();
                        straight_ruler.setValue(-resetAngle);

                        // 뒤집기 진행중 기발 false 설정
                        mIsRotateOrFlipAnimating = false
                    }
                })
                mAnimator.startAnimation(300)
            }
        }
    }

    /**
     *  Draw의 단추들의 listener 설정
     */
    private fun setupMoreDrawButtons() {
        more_draw_style_rect.setOnClickListener {
            currMoreDrawStyle = MORE_DRAW_STYLE_RECT
            updateDrawStyle(currMoreDrawStyle)
            highlightDrawStyleButtonBorder()
        }
        more_draw_style_circle.setOnClickListener {
            currMoreDrawStyle = MORE_DRAW_STYLE_CIRCLE
            updateDrawStyle(currMoreDrawStyle)
            highlightDrawStyleButtonBorder()
        }
        more_draw_style_blur_circle.setOnClickListener {
            currMoreDrawStyle = MORE_DRAW_STYLE_BLUR_CIRCLE
            updateDrawStyle(currMoreDrawStyle)
            highlightDrawStyleButtonBorder()
        }
        more_draw_style_blur_brush.setOnClickListener {
            currMoreDrawStyle = MORE_DRAW_STYLE_BLUR_BRUSH
            updateDrawStyle(currMoreDrawStyle)
            highlightDrawStyleButtonBorder()
        }
        more_draw_style_blur_dots.setOnClickListener {
            currMoreDrawStyle = MORE_DRAW_STYLE_BLUR_DOTS
            updateDrawStyle(currMoreDrawStyle)
            highlightDrawStyleButtonBorder()
        }

        // draw의 그리기두께 설정 흘림띠
        more_draw_thickness_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, i: Int, p2: Boolean) {
                updateDrawThickness(i)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                // 두께 정도를 표현하는 원 보여주기
                more_draw_thickness_circle.beVisible()
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                // 두께 정도를 표현하는 원 숨기기
                more_draw_thickness_circle.beGone()
            }
        })

        // draw의 1번째 색갈단추
        more_draw_color_red.setOnClickListener {
            config.lastEditorDrawColorNum = 0;
            updateDrawColor(MORE_DRAW_COLOR_0)
            highlightDrawColorButtonBorder()
        }
        // draw의 2번째 색갈단추
        more_draw_color_green.setOnClickListener {
            config.lastEditorDrawColorNum = 1;
            updateDrawColor(MORE_DRAW_COLOR_1)
            highlightDrawColorButtonBorder()
        }
        // draw의 3번째 색갈단추
        more_draw_color_blue.setOnClickListener {
            config.lastEditorDrawColorNum = 2;
            updateDrawColor(MORE_DRAW_COLOR_2)
            highlightDrawColorButtonBorder()
        }
        // draw의 4번째 색갈단추
        more_draw_color_black.setOnClickListener {
            config.lastEditorDrawColorNum = 3;
            updateDrawColor(MORE_DRAW_COLOR_3)
            highlightDrawColorButtonBorder()
        }
        // draw의 5번째 색갈단추
        more_draw_color_white.setOnClickListener {
            config.lastEditorDrawColorNum = 4;
            updateDrawColor(MORE_DRAW_COLOR_4)
            highlightDrawColorButtonBorder()
        }

        // draw의 색갈선택대화창 열기 단추
        more_draw_color_picker.setOnClickListener {
            openColorPicker()
        }
    }

    /**
     * Draw에서 선택된 형태단추의 배경을 흰색으로 하여 경계를 두드러지게 한다
     */
    @SuppressLint("ResourceType")
    private fun highlightDrawStyleButtonBorder() {
        // 모든 형태단추 배경을 투명하게
        arrayOf(more_draw_style_rect, more_draw_style_circle, more_draw_style_blur_circle, more_draw_style_blur_brush, more_draw_style_blur_dots).forEach {
            it?.setBackgroundResource(Color.TRANSPARENT)
        }

        // 마지막으로 선택되였던 형태단추 선택
        val currentAspectRatioButton = when (currMoreDrawStyle) {
            MORE_DRAW_STYLE_RECT -> more_draw_style_rect
            MORE_DRAW_STYLE_CIRCLE -> more_draw_style_circle
            MORE_DRAW_STYLE_BLUR_CIRCLE -> more_draw_style_blur_circle
            MORE_DRAW_STYLE_BLUR_BRUSH -> more_draw_style_blur_brush
            else -> more_draw_style_blur_dots
        }
        // 선택된 형태단추의 배경을 흰색으로
        currentAspectRatioButton?.setBackgroundResource(R.drawable.button_wh_border_background)
    }

    /**
     * Draw에서 선택된 색단추의 배경을 흰색으로 하여 경계를 두드러지게 한다
     */
    @SuppressLint("ResourceType")
    private fun highlightDrawColorButtonBorder() {
        // 모든 색단추 배경을 투명하게
        arrayOf(more_draw_color_red, more_draw_color_green, more_draw_color_blue, more_draw_color_black, more_draw_color_white).forEach {
            it?.setBackgroundResource(Color.TRANSPARENT)
        }

        // 마지막으로 선택되였던 색단추 선택
        val currentDrawColorButton = when (config.lastEditorDrawColorNum) {
            0-> more_draw_color_red
            1 -> more_draw_color_green
            2 -> more_draw_color_blue
            3 -> more_draw_color_black
            else -> more_draw_color_white
        }
        // 선택된 색단추의 배경을 흰색으로
        currentDrawColorButton?.setBackgroundResource(R.drawable.button_wh_border_background)
    }

    /**
     * Draw에서 색갈선택창 열기
     * 색갈선택창에서 색갈을 선택하면 현재 선택되여있는 색단추의 색갈을 갱신한다
     */
    private fun openColorPicker() {
        ColorPickerDialog(this, currMoreDrawColor) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                updateDrawColor(color)

                when (config.lastEditorDrawColorNum) {
                    0->{
                        more_draw_color_red.applyColorFilter(color)
                        MORE_DRAW_COLOR_0 = color
                    }
                    1->{
                        more_draw_color_green.applyColorFilter(color)
                        MORE_DRAW_COLOR_1 = color
                    }
                    2->{
                        more_draw_color_blue.applyColorFilter(color)
                        MORE_DRAW_COLOR_2 = color
                    }
                    3->{
                        more_draw_color_black.applyColorFilter(color)
                        MORE_DRAW_COLOR_3 = color
                    }
                    4->{
                        more_draw_color_white.applyColorFilter(color)
                        MORE_DRAW_COLOR_4 = color
                    }
                }
            }
        }
    }

    /**
     *  선택한 화면에 따라 화면적재함수 호출
     */
    @SuppressLint("ResourceType")
    private fun updatePrimaryActionButtons() {
        // 현재화면을 다시 선택하거나, 자르기가 진행중이면 되돌리기
        if (currPrimaryAction == prePrimaryAction || mCropInProgress) return

        // 경우에 따르는 화면 적재
        if (currPrimaryAction == PRIMARY_ACTION_CROP) {
            loadCropImageView()
        } else if (currPrimaryAction == PRIMARY_ACTION_FILTER) {
            loadFilterImageView()
        } else if (currPrimaryAction == PRIMARY_ACTION_ADJUST) {
            loadAdjustImageView()
        } else if (currPrimaryAction == PRIMARY_ACTION_MORE) {
            loadMoreImageView()
        }
    }

    /**
     * Crop/Filter/Adjust/More 화면이 바뀔때마다 호출되여 UI 갱신
     */
    private fun updateUI() {
        if (currPrimaryAction == PRIMARY_ACTION_CROP) {
            ucrop_view.beVisible()
            default_image_view_container.beGone()
            adjust_view_container.beInvisible()
            doodle_image_view.beGone()
            rotate_reset.beGone()

            txt_adjust_alert.beGone()
            txt_filter_alert.beGone()

            straight_ruler.setValue(0f)
        } else if (currPrimaryAction == PRIMARY_ACTION_FILTER) {
            default_image_view_container.beVisible()
            doodle_image_view.beGone()
            adjust_view_container.beInvisible()
            ucrop_view.beGone()

            txt_adjust_alert.beGone()
        } else if (currPrimaryAction == PRIMARY_ACTION_ADJUST) {
            adjust_view_container.beVisible()
            adjust_view.beVisible()
            default_image_view_container.beGone()
            ucrop_view.beGone()
            doodle_image_view.beGone()
            rgb_curve_canvas.beGone()
            rgb_channel_spinner.beGone()

            txt_filter_alert.beGone()
        } else if (currPrimaryAction == PRIMARY_ACTION_MORE) {
            default_image_view_container.beVisible()
            doodle_image_view.beGone()
            ucrop_view.beGone()
            adjust_view_container.beInvisible()

            txt_adjust_alert.beGone()
            txt_filter_alert.beGone()
        }

        // 선택된 화면단추를 얻고 isSelected기발을 설정하여 선택색갈이 표현되게 한다(푸른색)
        val currentPrimaryActionButton = when (currPrimaryAction) {
            PRIMARY_ACTION_FILTER -> bottom_primary_filter
            PRIMARY_ACTION_CROP -> bottom_primary_crop
            PRIMARY_ACTION_ADJUST -> bottom_primary_adjust
            PRIMARY_ACTION_MORE -> bottom_primary_more
            else -> null
        }

        arrayOf(bottom_primary_filter, bottom_primary_crop, bottom_primary_adjust, bottom_primary_more).forEach {
            it?.isSelected = false
        }
        currentPrimaryActionButton?.isSelected = true

        preBottomAccordian.beGone()
        // 비교단추는 자르기화면에서 보이지 않는다
        btn_compare.beGone()
        when (currPrimaryAction) {
            PRIMARY_ACTION_CROP -> {
                bottom_editor_crop_actions.beVisible()
                preBottomAccordian = bottom_editor_crop_actions;
                prePrimaryAction = PRIMARY_ACTION_CROP
            }
            PRIMARY_ACTION_FILTER -> {
                bottom_editor_filter_actions.beVisible()
                btn_compare.beVisible()
                preBottomAccordian = bottom_editor_filter_actions;
                prePrimaryAction = PRIMARY_ACTION_FILTER
            }
            PRIMARY_ACTION_ADJUST -> {
                bottom_editor_adjust_actions.beVisible()
                btn_compare.beVisible()
                preBottomAccordian = bottom_editor_adjust_actions;
                prePrimaryAction = PRIMARY_ACTION_ADJUST
            }
            PRIMARY_ACTION_MORE -> {
                bottom_editor_more_actions.beVisible()
                btn_compare.beVisible()
                preBottomAccordian = bottom_editor_more_actions;
                prePrimaryAction = PRIMARY_ACTION_MORE
            }
        }

        // 조정 View의 animation진행
        controlBarAnim(bottom_bar_container);
    }

    /**
     * 화상의 아래부분에 있는 조정 View의 animation 진행
     * @param view  animation이 진행되는 View
     */
    private fun controlBarAnim(view: View) {
        // set bottom bars animation
        val fadeIn = AlphaAnimation(0f, 1f);
        val transIn = TranslateAnimation(0f, 0f, 100f, 0f)

        val animInSet = AnimationSet(true)
        animInSet.addAnimation(fadeIn)
        animInSet.addAnimation(transIn)
        animInSet.duration = 300

        view.startAnimation(animInSet)
    }

    // filter가 적용되였는가를 나타낸다
    private var isFilterApplied = false

    /**
     * Filter에서 리용되는 RecyclerView Adapter를 생성한다
     */
    private fun generateFilterRVAdapter() {
        ensureBackgroundThread {
            val thumbSquare = getFilterThumb()
            runOnUiThread {
                val filterItems = ArrayList<FilterItem>()

                // 첫번째로 원본 FilterItem 추가
                val noFilter = Pair(R.string.original, "")
                filterItems.add(FilterItem(thumbSquare, noFilter))

                // filterItems 목록에 FilterItem들을 추가
                getFilterPack()!!.forEach {
                    val filterItem = FilterItem(thumbSquare, it)
                    filterItems.add(filterItem)
                }

                // Filter RecyclerView Adapter 생성
                val adapter = FiltersAdapter(applicationContext, filterItems) {
                    // click listener
                    if (!mCropInProgress) {
                        // filter항목의 이름 animation 진행
                        itemNameAlertAnim(txt_filter_alert, getString(filterItems[it].filter.first))
                        isFilterApplied = it > 0
                        applyFilter(filterItems[it])
                    }
                }

                bottom_actions_filter_rv.adapter = adapter
                adapter.notifyDataSetChanged()
            }
        }
    }

    /**
     * Filter/Adjust의 항목들을 누를때 나타나는 이름 animation 진행
     * @param textView  animation이 적용되는 TextView
     * @param filterName  현시하는 이름/값
     */
    private fun itemNameAlertAnim(textView: TextView, filterName: String) {
        val scaleUp = ScaleAnimation(0.5f, 1f, 0.5f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f )
        val fadeIn = AlphaAnimation(0f, 1f)

        val animSet = AnimationSet(true)
        animSet.addAnimation(scaleUp)
        animSet.addAnimation(fadeIn)
        animSet.duration = 200

        animSet.setAnimationListener(object: Animation.AnimationListener{
            override fun onAnimationRepeat(p0: Animation?) { }

            override fun onAnimationEnd(p0: Animation?) {
                text_alert_handler.postDelayed(Runnable {
                    textView.beGone()
                }, 800)
            }

            override fun onAnimationStart(p0: Animation?) {
                textView.text = filterName
                textView.beVisible()
            }
        })
        text_alert_handler.removeCallbacksAndMessages(null)
        textView.clearAnimation()
        textView.beGone()
        textView.startAnimation(animSet)
    }

    /**
     * Adjust에서 리용되는 RecyclerView Adapter를 생성한다
     */
    private fun generateAdjustRVAdapter() {
        ensureBackgroundThread {
            runOnUiThread {

                // Adjust 목록 생성
                val adjustList: ArrayList<AdjustConfig> = ArrayList()
                adjustList.add( AdjustConfig(R.string.Exposure, R.drawable.ic_adjust_exposure_selector, "adjust", "exposure", -2f, 2f, 0f) )
                adjustList.add( AdjustConfig(R.string.Contrast, R.drawable.ic_adjust_contrast_selector, "adjust", "contrast", 0.1f, 3f, 1f) )
                adjustList.add( AdjustConfig(R.string.Saturation, R.drawable.ic_adjust_exposure_selector, "adjust", "saturation", 0f, 3f, 1f) )
                adjustList.add( AdjustConfig(R.string.Shadow, R.drawable.ic_adjust_shadow_selector, "adjust", "shadow", -200f, 100f, 0f) )
                adjustList.add( AdjustConfig(R.string.Highlight, R.drawable.ic_adjust_highlights_selector, "adjust", "highlight", -100f, 200f, 0f) )
                adjustList.add( AdjustConfig(R.string.Hue, R.drawable.ic_adjust_hue_selector, "adjust", "hue", -5f, 5f, 0f) )         // 0 - 359, 0
                adjustList.add( AdjustConfig(R.string.Temperature, R.drawable.vector_adjust_temperature, "adjust", "whitebalance", -0.8f, 0.8f, 0f) )         // 0 - 359, 0
                adjustList.add( AdjustConfig(R.string.Posterize, R.drawable.ic_adjust_posterize_selector, "adjust", "posterize", 1f, 256f, 30f) )   // 1 - 256, 10
                adjustList.add( AdjustConfig(R.string.Vibrance, R.drawable.ic_gallery_adjust_vibrance, "adjust", "vibrance", -1f, 1f, 0f) )     // any

                adjustList.add( AdjustConfig(R.string.Curve, R.drawable.ic_adjust_curves_selector, "curve", "curve", 0f, 0f, 0f) )
//                adjustList.add( AdjustConfig(R.string.Edge, R.drawable.ic_adjust_bwfilter_selector, "style", "edge", 0f, 10f, 5f) )

                val adapter = AdjustAdapter(applicationContext, adjustList) {
                    if (!mCropInProgress) {
                        val adjustItem = adjustList.get(it)
                        // adjust이름 animation 진행
                        itemNameAlertAnim(txt_adjust_alert, getString(adjustItem.mNameResID))

                        updateAdjustSeekbar(adjustItem)
                    }
                }

                bottom_actions_adjust_rv.adapter = adapter
                adapter.notifyDataSetChanged()
            }
        }
    }

    /**
     * 자르기화면에서 리용되는 비률선택 RecyclerView Adapter 생성 (e.g. 16:9, 1:1, 4:3, etc)
     */
    private fun generateCropRVAdapter() {
        ensureBackgroundThread {
            runOnUiThread {
                val width: Int = realScreenSize.x
                val height: Int = realScreenSize.y

                val cropItemList: ArrayList<CropSelectItem> = ArrayList()
                cropItemList.add(CropSelectItem(R.drawable.baseline_image_aspect_ratio_white_48, -1f, -1f))
                cropItemList.add(CropSelectItem(R.drawable.baseline_crop_portrait_white_48, width.toFloat(), height.toFloat()))
                cropItemList.add(CropSelectItem(R.drawable.baseline_crop_landscape_white_48, height.toFloat(), width.toFloat()))
                cropItemList.add(CropSelectItem(R.drawable.baseline_crop_1_1_white_48, 1f, 1f))
                cropItemList.add(CropSelectItem(R.drawable.baseline_crop_16_9_white_48, 16f, 9f))
                cropItemList.add(CropSelectItem(R.drawable.baseline_crop_9_16_white_48, 9f, 16f))
                cropItemList.add(CropSelectItem(R.drawable.baseline_crop_4_3_white_48, 4f, 3f))
                cropItemList.add(CropSelectItem(R.drawable.baseline_crop_3_4_white_48, 3f, 4f))
                cropItemList.add(CropSelectItem(R.drawable.baseline_crop_3_2_white_48, 3f, 2f))
                cropItemList.add(CropSelectItem(R.drawable.baseline_crop_2_3_white_48, 2f, 3f))

                val adapter = CropSelectorRVAdapter(applicationContext, cropItemList) {pos: Int, animate: Boolean ->
                    // pos: RecyclerView에서 선택된 비률 위치
                    // animate: 자르기구역의 animation이 진행되여야 하는가를 나타낸다

                    val cropItem = cropItemList.get(pos)
                    currAspectRatio = Pair(cropItem.x, cropItem.y)

                    if (animate) {
                        // 자르기구역의 animation 진행
                        if (!mIsRotateOrFlipAnimating) {
                            val startOriginRect = RectF()
                            val endOriginRect = RectF()
                            val aspectRatio = if (currAspectRatio.first < 0 && currAspectRatio.second < 0) {
                                -1f
                            } else {
                                currAspectRatio.first / currAspectRatio.second
                            }
                            mAnimator.addAnimatorListener(object : SimpleValueAnimatorListener {
                                override fun onAnimationStarted() {
                                    // 자르기구역 animation의 시작구역 설정
                                    startOriginRect.set(crop_view_overlay.noPaddingCropViewRect)
                                    // 자르기구역 animation의 마감구역 계산에 리용되는 origin(최대) 구역 설정
                                    if (gesture_crop_image_view.isPortrait) endOriginRect.set(gesture_crop_image_view.originRectPortrait)
                                    else endOriginRect.set(gesture_crop_image_view.originRectLandscape)

                                    mIsRotateOrFlipAnimating = true;
                                }

                                override fun onAnimationUpdated(scale: Float) {
                                    // animate crop rect
                                    crop_view_overlay.animateCropViewRect(startOriginRect, endOriginRect, scale, aspectRatio, true, false)
                                }

                                override fun onAnimationFinished() {
                                    mIsRotateOrFlipAnimating = false
                                    updateAspectRatio()
                                }
                            })
                            mAnimator.startAnimation(200)
                        }
                    } else updateAspectRatio()
                }

                crop_rv.adapter = adapter
                adapter.notifyDataSetChanged()
            }
        }
    }

    /**
     * Adjust를 적용할때 보관된 변수들 초기화, 화면효과지우기
     */
    private fun resetAdjustSavedValues() {
        // clear saved adjust items
        appliedAdjusts.clear()
        (bottom_actions_adjust_rv.adapter as AdjustAdapter).resetItems()
        setAdjustConfigStr()
        isAdjustApplied = false

        // reset rgb curve saved values for chart
        curve_points_rgb.clear()
        curve_points_r.clear()
        curve_points_g.clear()
        curve_points_b.clear()
        rgb_curve_canvas.resetPoints()
    }

    /**
     * Adjust의 항목을 눌렀을때 나타나는 Seekbar 갱신, onSeekChangelistener 설정
     * @param adjustItem  선택된 adjustItem
     */
    private fun updateAdjustSeekbar(adjustItem: AdjustConfig) {

        if (adjustItem.mTypeName == "curve") {
            rgb_curve_canvas.beVisible()
            rgb_channel_spinner.beVisible()

            // curve의 점들을 움직일때의 listener
            rgb_curve_canvas.setPointUpdateListener(object : CanvasSpliner.PointMoveListener {
                // curve의 점을 움직이고 있을때
                override fun onCurvePointUpdate(canvasPoints: List<PointF>, curvePoints: List<PointF>) {
                    when (currentRGBChannel) {
                        CURVE_CHANNEL_RGB -> {
                            // RGB channel 효과문자렬 구성
                            color_points_rgb = "RGB"
                            for(point in curvePoints) {
                                color_points_rgb += "(" + (255 * point.x).toInt() + ", " + (255 * point.y).toInt() + ") "
                            }
                            // channel이 변할때 점들을 교체하기 위하여 보관
                            curve_points_rgb = ArrayList(canvasPoints)
                        };
                        CURVE_CHANNEL_R -> {
                            // R channel 효과문자렬 구성
                            color_points_r = "R"
                            for(point in curvePoints) {
                                color_points_r += "(" + (255 * point.x).toInt() + ", " + (255 * point.y).toInt() + ") "
                            }
                            // channel이 변할때 점들을 교체하기 위하여 보관
                            curve_points_r = ArrayList(canvasPoints)
                        };
                        CURVE_CHANNEL_G -> {
                            // G channel 효과문자렬 구성
                            color_points_g = "G"
                            for(point in curvePoints) {
                                color_points_g += "(" + (255 * point.x).toInt() + ", " + (255 * point.y).toInt() + ") "
                            }
                            // channel이 변할때 점들을 교체하기 위하여 보관
                            curve_points_g = ArrayList(canvasPoints)
                        };
                        CURVE_CHANNEL_B -> {
                            // B channel 효과문자렬 구성
                            color_points_b = "B"
                            for(point in curvePoints) {
                                color_points_b += "(" + (255 * point.x).toInt() + ", " + (255 * point.y).toInt() + ") "
                            }
                            // channel이 변할때 점들을 교체하기 위하여 보관
                            curve_points_b = ArrayList(canvasPoints)
                        };
                    }

                    adjustItem.mConfigStr = "@curve " + color_points_rgb + color_points_r + color_points_g + color_points_b

                    if (appliedAdjusts.indexOf(adjustItem) == -1) appliedAdjusts.add(adjustItem)
                    setAdjustConfigStr()
                }

                // curve의 점을 움직이다 놓았을때
                override fun onPointUp() {
                    // 결과 변화가 있었는지를 검사한다
                    var unChanged = true;

                    val width = rgb_curve_canvas.splinerWidth.toFloat()
                    val height = rgb_curve_canvas.splinerHeight.toFloat()

                    if (curve_points_rgb.size == 2) {
                        unChanged = unChanged && curve_points_rgb[0].x == 1f && curve_points_rgb[0].y == 0f &&
                                curve_points_rgb[1].x == width && curve_points_rgb[1].y == height
                    } else unChanged = unChanged && curve_points_rgb.size == 0

                    if (curve_points_r.size == 2) {
                        unChanged = unChanged && curve_points_r[0].x == 1f && curve_points_r[0].y == 0f &&
                                curve_points_r[1].x == width && curve_points_r[1].y == height
                    } else unChanged = unChanged && curve_points_r.size == 0

                    if (curve_points_g.size == 2) {
                        unChanged = unChanged && curve_points_g[0].x == 1f && curve_points_g[0].y == 0f &&
                                curve_points_g[1].x == width && curve_points_g[1].y == height
                    } else unChanged = unChanged && curve_points_g.size == 0

                    if (curve_points_b.size == 2) {
                        unChanged = unChanged &&  curve_points_b[0].x == 1f && curve_points_b[0].y == 0f &&
                                curve_points_b[1].x == width && curve_points_b[1].y == height
                    } else unChanged = unChanged && curve_points_b.size == 0

                    // 변화가 없으면 appliedAdjusts에서 삭제
                    if (unChanged) appliedAdjusts.remove(adjustItem)

                    // adjust가 적용되였다는 기발 설정
                    isAdjustApplied = appliedAdjusts.size > 0
                }
            })
        } else {
            rgb_curve_canvas.beGone()
            rgb_channel_spinner.beGone()
        }

        // 흘림띠가 없는 adjust에 대해서는 최소/최대값을 같게 주었다 (e.g. Curve)
        adjust_seek_bar.beGoneIf(adjustItem.minValue == adjustItem.maxValue)

        adjust_seek_bar.onSeekChangeListener = null
        adjust_seek_bar.min = -50f
        adjust_seek_bar.max = 50f
        adjust_seek_bar.setProgress( adjustItem.slierIntensity * (adjust_seek_bar.max - adjust_seek_bar.min) + adjust_seek_bar.min )
        adjust_seek_bar.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams?) {
                synchronized(this) {
                    val progress = seekParams!!.progress

                    // 흘림띠값의 animation진행
                    itemNameAlertAnim(txt_adjust_alert, progress.toString())

                    val intensity = (progress - adjust_seek_bar.min) / (adjust_seek_bar.max - adjust_seek_bar.min)
                    adjustItem.updateIntensity(intensity);

                    // 흘림띠값이 adjustItem의 표준값이면 appliedAdjusts에서 삭제하고 화상 갱신
                    if (adjustItem.intensity == adjustItem.originvalue) {
                        appliedAdjusts.remove(adjustItem)
                        setAdjustConfigStr()
                        return;
                    } else {
                        // 이미 추가된 adjustItem인가를 검사하고 아니면 추가하고 화상 갱신
                        if (appliedAdjusts.indexOf(adjustItem) == -1) {
                            Log.d("from", "add");
                            appliedAdjusts.add(adjustItem)
                            setAdjustConfigStr()
                            return;
                        }
                    }
                    // 추가된 adjustItem들이 있으면 화상 갱신
                    if (appliedAdjusts.size > 0) {
                        if (adjustItem.mFuncName == "shadowhighlight" || adjustItem.mFuncName == "curve" || adjustItem.mFuncName == "whitebalance") {
                            setAdjustConfigStr()
                        } else {
                            val index = appliedAdjusts.indexOf(adjustItem)
                            if (index != -1) adjust_view.setFilterIntensityForIndex(adjustItem.intensity, index)
                        }
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) { }

            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {
                // adjust가 적용되였는지 검사
                isAdjustApplied = appliedAdjusts.size > 0
            }
        }
    }

    /**
     * 적용된 adjust들의 문자렬들을 합하여 적용
     * 일부 adjust들은 개별적으로 적용(setFilterIntensityForIndex)이 불가능하고 setFilterWithConfig를 통하여 적용가능하다
     */
    fun setAdjustConfigStr() {
        var groupAdjustStr = ""
        for ( adjustItem in appliedAdjusts) {
            groupAdjustStr += adjustItem.mConfigStr
        }
        adjust_view.setFilterWithConfig(groupAdjustStr)
    }

    // filter가 적용된 마지막 결과 bitmap
    var lastFilteredBmp : Bitmap? = null

    /**
     * 파라메터로 들어온 filterItem을 적용하여 화면갱신
     */
    private fun applyFilter(filterItem: FilterItem) {
        if (lastFilteredBmp != null && lastFilteredBmp != currentBitmap) lastFilteredBmp!!.recycle()

        val ruleString = filterItem.filter.second
        val filteredBmp = CGENativeLibrary.filterImage_MultipleEffects(currentBitmap!!, ruleString, 1f)
        default_image_view.setImageBitmap(filteredBmp)
        lastFilteredBmp = filteredBmp
    }

    /**
     * 선택된 자르기 비률로 갱신
     */
    private fun updateAspectRatio() {
        if (currAspectRatio!!.first < 0 && currAspectRatio!!.second < 0 ) {
            // 이 경우는 자유비률이다
            gesture_crop_image_view.setKeepAspectRatio(false);
            gesture_crop_image_view.targetAspectRatio = FREE_ASPECT_RATIO
        } else {
            // 이 경우는 고정비률이다 (e.g. 16:9, 3:2, etc)
            gesture_crop_image_view.setKeepAspectRatio(true);
            gesture_crop_image_view.targetAspectRatio = currAspectRatio!!.first / currAspectRatio!!.second
        }
    }

    /**
     * Draw의 그리기색갈을 설정, preference에 보관
     */
    private fun updateDrawColor(color: Int) {
        currMoreDrawColor = color
        config.lastEditorBrushColor = color
        setDoodleColor(color)
    }

    /**
     * Draw의 그리기형태 설정, preference에 보관
     */
    private fun updateDrawStyle(style: Int) {
        config.lastEditorBrushStyle = style
        mDoodle.setBrushBMPStyle(style)
    }

    /**
     * Draw의 그리기두께 설정, preference에 보관
     */
    private fun updateDrawThickness(percent: Int) {
        config.lastEditorBrushSize = percent
        val width = resources.getDimension(R.dimen.full_brush_size) * (percent / 100f)
        setDoodleSize(width.toInt());

        val scale = Math.max(0.03f, percent / 100f)
        more_draw_thickness_circle.scaleX = scale
        more_draw_thickness_circle.scaleY = scale
    }

    /**
     * 자르기 진행후의 callback
     * @param result  자르기 결과정보
     */
    override fun onCropImageComplete(resultBmp: Bitmap) {
        if (resultBmp != null) {
            if (isCropIntent) {
                if (saveUri.scheme == "file") {
                    saveBitmapToFile(resultBmp, saveUri.path!!)
                } else {
                    var inputStream: InputStream? = null
                    var outputStream: OutputStream? = null
                    try {
                        val stream = ByteArrayOutputStream()
                        resultBmp.compress(CompressFormat.JPEG, 100, stream)
                        inputStream = ByteArrayInputStream(stream.toByteArray())
                        outputStream = contentResolver.openOutputStream(saveUri)
                        inputStream.copyTo(outputStream!!)
                    } finally {
                        inputStream?.close()
                        outputStream?.close()
                    }

                    Intent().apply {
                        data = saveUri
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setResult(RESULT_OK, this)
                    }
                    finish()
                }
            } else {
                if (cropForAction == CROP_FOR_SAVE) {
                    // save cropped bitmap
                    saveBitmap(resultBmp)
                    return;
                }
                if (cropForAction == CROP_FOR_COMPARE) {
                    releaseCompareView()
                    compare_image_view.setImageBitmap(resultBmp)
                    historyManager.addCompareHistory(resultBmp)
                } else {
                    currentBitmap!!.recycle()
                    currentBitmap = resultBmp
                    mCropInProgress = false;
                    progress_spinner.smoothToHide()

                    if (cropForAction == CROP_FOR_FILTER) {
                        default_image_view.setImageBitmap(resultBmp)
                        updateFilterThumb()
                    } else if (cropForAction == CROP_FOR_ADJUST) {
                        setAdjustImage(resultBmp)
                    } else if (cropForAction == CROP_FOR_MORE) {
                        default_image_view.setImageBitmap(resultBmp)
                    }
                    releaseCropView()

                    updateUI()

                    // save history
                    makeHistory(resultBmp);

                    // 비교화상 자르기 진행
                    cropForAction = CROP_FOR_COMPARE
                    gesture_crop_image_view.getCroppedImageAsync(compare_image_view.drawable.convertToBitmap())
                }
            }
        } else {
            toast(getString(R.string.image_editing_failed))
        }
    }

    /**
     * HistoryManager의 history 보관함수를 호출하고 Undo/Redo 메뉴갱신함수 호출
     * @param history  history로 보관하려는 bitmap
     */
    private fun makeHistory(history: Bitmap) {
        historyManager.addHistory(history);
        updateUndoRedoButton()
    }

    /**
     * Undo/Redo 메뉴들의 활성상태 갱신
     */
    private fun updateUndoRedoButton() {
        if (menu_edit != null ) {
            menu_edit!!.findItem(R.id.menu_undo).isEnabled = historyManager.canUndo()
            menu_edit!!.findItem(R.id.menu_redo).isEnabled = historyManager.canRedo()
        }
    }

    /**
     * 새로운 보관경로를 생성한다
     */
    private fun getNewFilePath(): String {
        val folderPath: String = if(realFilePath != ""){
            realFilePath.getParentPath()
        }
        else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/Camera").toString()
        }

        val simpleDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "IMG_" + simpleDateFormat.format(Date(System.currentTimeMillis())) + ".jpg"

        val file = File(folderPath, fileName);
        return file.absolutePath
    }

    /**
     * 화일경로를 리용하여 OutputStream을 생성하고 화상보관함수 호출
     * @param bitmap  보관하려는 bitmap
     * @param path  보관경로
     */
    private fun saveBitmapToFile(bitmap: Bitmap, path: String) {
        try {
            ensureBackgroundThread {
                val file = File(path)
                val fileDirItem = FileDirItem(path, path.getFilenameFromPath())
                getFileOutputStream(fileDirItem, true) {
                    if (it != null) {
                        saveBitmap(file, bitmap, it)
                    } else {
                        toast(R.string.image_editing_failed)
                    }
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        } catch (e: OutOfMemoryError) {
            toast(R.string.out_of_memory_error)
        }
    }

    /**
     * 화일쓰기부분 <br/>
     * 화상을 보관하고 Activity를 끝낸다
     * @param file : 보관되는 화일
     * @param bitmap : 보관하려는 bitmap
     * @param out : 보관에 리용되는 OutputStream
     */
    @TargetApi(Build.VERSION_CODES.N)
    private fun saveBitmap(file: File, bitmap: Bitmap, out: OutputStream) {
        bitmap.compress(file.absolutePath.getCompressionFormat(), 90, out)
        out.close()

        if(file.exists() && file.length() > 0 && file.canRead()) {
            val path = file.absolutePath
            val paths = arrayListOf(path)
            rescanPaths(paths) {
                //Medium객체를 만들어서 그것을 intent의 extra자료로 보낸다.
                val modified = file.lastModified()
                val newMedium = Medium(null, path.getFilenameFromPath(), path, path.getParentPath(), modified, modified, Utils.formatDate(modified), file.length(), MEDIA_IMAGE, 0, 0)
                intent.putExtra(RESULT_MEDIUM, newMedium)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    fun onClick(v: View) {
        if (v.id == R.id.doodle_selectable_remove) {
            // 편집중 지우기 단추
            mDoodle.removeItem(mTouchGestureListener.selectedItem)
            mTouchGestureListener.selectedItem = null
            updateDoodleUndoRedo()
        } else if (v.id == R.id.doodle_selectable_top) {
            // 편집중 앞으로 내오기 단추 (z-order)
            mDoodle.topItem(mTouchGestureListener.selectedItem)
        } else if (v.id == R.id.doodle_selectable_bottom) {
            // 편집중 뒤로 가져가기 단추 (z-order)
            mDoodle.bottomItem(mTouchGestureListener.selectedItem)
        } else if (v.id == R.id.btn_paint_brush) {
            // doodle의 붓 그리기 설정
            mDoodle.shape = DoodleShape.PAINTBRUSH
            loadPreference()
        } else if (v.id == R.id.btn_arrow) {
            // doodle의 화살표 그리기 설정
            mDoodle.shape = DoodleShape.ARROW
            loadPreference()
        } else if (v.id == R.id.btn_holl_circle) {
            // doodle의 원 그리기 설정
            mDoodle.shape = DoodleShape.HOLLOW_CIRCLE
            loadPreference()
        } else if (v.id == R.id.btn_holl_rect) {
            // doodle의 사각형 그리기 설정
            mDoodle.shape = DoodleShape.HOLLOW_RECT
            loadPreference()
        } else if (v.id == R.id.btn_doodle_size) {
            // doodle의 두께를 갱신
            if (currDoodleSizeSelection == DOODLE_SIZE_1) currDoodleSizeSelection = DOODLE_SIZE_2
            else if (currDoodleSizeSelection == DOODLE_SIZE_2) currDoodleSizeSelection = DOODLE_SIZE_3
            else if (currDoodleSizeSelection == DOODLE_SIZE_3) currDoodleSizeSelection = DOODLE_SIZE_1

            setDoodleSize(currDoodleSizeSelection)
            // preference에 보관
            editor.putInt(prefKeyBuilder("size"), currDoodleSizeSelection)
            editor.apply()
        } else if (v.id == R.id.btn_mosaic_level_1) {
            // mosaic의 정도1 선택
            v.isSelected = true
            btn_mosaic_level_2.isSelected = false
            btn_mosaic_level_3.isSelected = false
            // mosaic의 정도1 설정
            mMosaicLevel = DoodlePath.MOSAIC_LEVEL_1
            mDoodle.color = DoodlePath.getMosaicColor(mDoodle, mMosaicLevel)
            if (mTouchGestureListener.selectedItem != null) {
                // 편집중 선택되여 있는 item이 있으면 item의 색갈을 갱신
                mTouchGestureListener.selectedItem.color = mDoodle.color.copy()
            }
            // preference에 보관
            editor.putInt(DOODLE_MOSAIC_LEVEL, DOODLE_SIZE_1)
            editor.apply()
        } else if (v.id == R.id.btn_mosaic_level_2) {
            // mosaic의 정도2 선택
            v.isSelected = true
            btn_mosaic_level_1.isSelected = false
            btn_mosaic_level_3.isSelected = false
            // mosaic의 정도2 설정
            mMosaicLevel = DoodlePath.MOSAIC_LEVEL_2
            mDoodle.color = DoodlePath.getMosaicColor(mDoodle, mMosaicLevel)
            if (mTouchGestureListener.selectedItem != null) {
                // 편집중 선택되여 있는 item이 있으면 item의 색갈을 갱신
                mTouchGestureListener.selectedItem.color = mDoodle.color.copy()
            }
            // preference에 보관
            editor.putInt(DOODLE_MOSAIC_LEVEL, DOODLE_SIZE_2)
            editor.apply()
        } else if (v.id == R.id.btn_mosaic_level_3) {
            // mosaic의 정도3 선택
            v.isSelected = true
            btn_mosaic_level_1.isSelected = false
            btn_mosaic_level_2.isSelected = false
            // mosaic의 정도3 설정
            mMosaicLevel = DoodlePath.MOSAIC_LEVEL_3
            mDoodle.color = DoodlePath.getMosaicColor(mDoodle, mMosaicLevel)
            if (mTouchGestureListener.selectedItem != null) {
                // 편집중 선택되여 있는 item이 있으면 item의 색갈을 갱신
                mTouchGestureListener.selectedItem.color = mDoodle.color.copy()
            }
            // preference에 보관
            editor.putInt(DOODLE_MOSAIC_LEVEL, DOODLE_SIZE_3)
            editor.apply()
        }
        if (v.tag != null) {
            val tag = v.tag.toString()
            if (tag.substring(0, 12) == "doodle_color") {
                // doodle의 색갈단추 설정
                // doodle 색갈 설정
                val color = tag.substring(13)
                setDoodleColor(color)
                // doodle 색갈 보관
                editor.putString(prefKeyBuilder("color"), color)
                editor.apply()
            }
        }
    }

    /**
     * doodle의 Undo/Redo 메뉴의 활성상태를 갱신
     */
    private fun updateDoodleUndoRedo() {
        menu_edit!!.findItem(R.id.menu_doodle_undo).isEnabled = mDoodle.itemCount > 0
        menu_edit!!.findItem(R.id.menu_doodle_redo).isEnabled = mDoodle.redoItemCount > 0
    }

    /**
     * Doodle의 그리기 두께를 설정한다.<br/>
     * 화살인경우 좀 더 두꺼워야 잘 보인다.
     * @param newSize 그리기 두께
     */
    private fun setDoodleSize(newSize: Int) {
        var size = calcDoodlePaintSize(newSize.toFloat())

        if (mDoodle.shape == DoodleShape.ARROW) {
            // 화살표 그리기인 경우 크기를 크게 해줘야 화면에 잘 나타난다.
            // setSize() 에서도 일치시켜야 한다.
            size *= if (newSize == DOODLE_SIZE_1) 2f
            else 1.5f
        }

        if (mDoodle.size != size) {
            mDoodle.size = size
            // 편집중 선택되여 있는 item이 있으면 item의 두께를 갱신한다
            if (mTouchGestureListener.selectedItem != null) {
                mTouchGestureListener.selectedItem.size = size
            }
        }
    }

    /**
     * 화상크기에 따라 실지 그리기 두께를 계산하여 반환한다.
     * @param newSize 새로 설정하려는 그리기 두께
     */
    private fun calcDoodlePaintSize(newSize: Float) : Float {
        // 화상크기에 비례하게 계산한다.
        return currentBitmap!!.width * 2f / realScreenSize.x * newSize
    }

    /**
     * Doodle의 색갈을 설정한다
     * @param color 색갈을 나타내는 hex문자렬
     */
    private fun setDoodleColor(color: String) {
        mDoodle.color = DoodleColor(Color.parseColor(color))
    }

    /**
     * Doodle의 색갈을 설정한다
     * @param color 설정하려는 색갈 (e.g. Color.RED)
     */
    private fun setDoodleColor(color: Int) {
        mDoodle.color = DoodleColor(color)
    }

    /**
     * 붓종류에 따른 보관된 preference를 얻기위한 key를 돌려준다
     */
    fun prefKeyBuilder(suffix: String?): String? {
        if (!mDoodleView.isEditMode) {
            val shape = mDoodle.shape
            val builder = java.lang.StringBuilder("doodle_")
            if (shape === DoodleShape.PAINTBRUSH) builder.append("paint_brush_")
            else if (shape === DoodleShape.HOLLOW_RECT) builder.append("rect_")
            else if (shape === DoodleShape.HOLLOW_CIRCLE) builder.append("circle_")
            else if (shape === DoodleShape.ARROW) builder.append("arrow_")
            return builder.append(suffix).toString()
        } else return ""
    }

    /**
     * 선택된 형태에 따라 preference로 보관된 Doodle 크기와 색갈을 얻어 설정한다
     */
    private fun loadPreference() {
        val size = sharedPreferences.getInt(prefKeyBuilder("size"), DOODLE_SIZE_1)
        setDoodleSize(size)
        val color = sharedPreferences.getString(prefKeyBuilder("color"), color_ary[0])!!
        setDoodleColor(color)
    }

    /**
     * preference로 보관된 mosaic 크기와 정도를 얻어 설정한다
     */
    private fun loadMosaicPreference() {
        // mosaic의 정도를 얻어 설정
        val level = sharedPreferences.getInt(DOODLE_MOSAIC_LEVEL, DoodlePath.MOSAIC_LEVEL_1)
        btn_mosaic_level_1.isSelected = false
        btn_mosaic_level_2.isSelected = false
        btn_mosaic_level_3.isSelected = false
        when (level) {
            DOODLE_SIZE_1 -> btn_mosaic_level_1.isSelected = true
            DOODLE_SIZE_2 -> btn_mosaic_level_2.isSelected = true
            DOODLE_SIZE_3 -> btn_mosaic_level_3.isSelected = true
        }
        mDoodle.color = DoodlePath.getMosaicColor(mDoodle, level)

        // mosaic의 두께를 얻어 설정
        val size = sharedPreferences.getInt(DOODLE_MOSAIC_SIZE, DEFAULT_MOSAIC_SIZE)
        more_mosaic_thickness_seekbar.progress = size
        setDoodleSize(size)
    }

    /**
     * DoodleView를 계승하고 설정함수들을 재정의하여 UI 갱신
     */
    private inner class DoodleViewWrapper(context: Context?, bitmap: Bitmap?, optimizeDrawing: Boolean, listener: IDoodleListener?) : DoodleView(context, bitmap, optimizeDrawing, listener) {
        override fun setPen(pen: IDoodlePen) {
            super.setPen(pen)
            if (pen === DoodlePen.MOSAIC) {
                if (mMosaicLevel <= 0) {
                    mMosaicLevel = DoodlePath.MOSAIC_LEVEL_3
                    mDoodle.color = DoodlePath.getMosaicColor(mDoodle, mMosaicLevel)
                    if (mTouchGestureListener.selectedItem != null) {
                        // 편집중 선택되여 있는 item이 있으면 item의 색갈도 갱신
                        mTouchGestureListener.selectedItem.color = mDoodle.getColor().copy()
                    }
                } else {
                    mDoodle.color = DoodlePath.getMosaicColor(mDoodle, mMosaicLevel)
                }
            }
        }
        // 단일선택을 위하여 doodle 그리기 단추들의 ID를 넣는다
        private val mBtnShapeIds: MutableMap<IDoodleShape, Int> = HashMap()

        /**
         * Doodle의 그리기 형태를 설정한다 (e.g. 화살, 사각형, 원, etc)
         */
        override fun setShape(shape: IDoodleShape) {
            if (shape === DoodleShape.SHAPE_MOSAIC) {
                super.setShape(DoodleShape.PAINTBRUSH)
                mDoodle.pen = DoodlePen.MOSAIC
            } else {
                super.setShape(shape)
                mDoodle.pen = DoodlePen.BRUSH
                setSingleSelected(mBtnShapeIds.values, mBtnShapeIds[shape]!!)
            }
        }

        /**
         * Doodle의 그리기 두께를 설정하고 UI 갱신
         */
        override fun setSize(paintSize: Float) {
            super.setSize(paintSize)
            if (pen === DoodlePen.MOSAIC) {
                // mosaic일때에는 seekbar를 설정한다
                val seekBar = more_mosaic_thickness_seekbar as MySeekBar
                seekBar.progress = paintSize.toInt()
            } else {
                // 두께를 나타내는 동그라미를 크게 해준다
                var selection = 1
                if (mDoodle.shape == DoodleShape.ARROW) {
                    // 화살표인경우 더 두껍게 하여야 잘 보인다
                    if (paintSize == calcDoodlePaintSize(DOODLE_SIZE_1.toFloat()) * 2f) selection = 1
                    else if (paintSize == calcDoodlePaintSize(DOODLE_SIZE_2.toFloat()) * 1.5f) selection = 2
                    else if (paintSize == calcDoodlePaintSize(DOODLE_SIZE_3.toFloat()) * 1.5f) selection = 3
                } else {
                    if (paintSize == calcDoodlePaintSize(DOODLE_SIZE_1.toFloat())) selection = 1
                    else if (paintSize == calcDoodlePaintSize(DOODLE_SIZE_2.toFloat())) selection = 2
                    else if (paintSize == calcDoodlePaintSize(DOODLE_SIZE_3.toFloat())) selection = 3
                }
                doodle_size_selection.post {
                    if (selection == 1) {
                        doodle_size_selection.background = ContextCompat.getDrawable(context, R.drawable.doodle_size_1)
                        currDoodleSizeSelection = DOODLE_SIZE_1
                    } else if (selection == 2) {
                        doodle_size_selection.background = ContextCompat.getDrawable(context, R.drawable.doodle_size_2)
                        currDoodleSizeSelection = DOODLE_SIZE_2
                    } else if (selection == 3) {
                        doodle_size_selection.background = ContextCompat.getDrawable(context, R.drawable.doodle_size_3)
                        currDoodleSizeSelection = DOODLE_SIZE_3
                    }
                }
            }
            if (mTouchGestureListener.selectedItem != null) {
                mTouchGestureListener.selectedItem.size = size
            }
        }

        /**
         * Doodle의 색갈을 설정하고 UI 갱신
         */
        override fun setColor(color: IDoodleColor) {
            val pen = pen
            super.setColor(color)
            var doodleColor: DoodleColor? = null
            if (color is DoodleColor) {
                doodleColor = color
            }
            if (doodleColor != null
                    && canChangeColor(pen)) {
                // doodle의 색단추들의 단일선택 진행
                for (id in mBtnColorIds.values) {
                    if (id == mBtnColorIds.get(doodleColor.color)) {
                        this@EditActivity.findViewById<ImageView>(id).setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_check_vector_black))
                    } else {
                        this@EditActivity.findViewById<ImageView>(id).setImageDrawable(null)
                    }
                }
                // 편집중 선택한 item이 있으면 item의 색갈을 갱신시켜준다
                if (mTouchGestureListener.selectedItem != null) {
                    mTouchGestureListener.selectedItem.color = getColor().copy()
                }
            }
            // mosaic의 정도를 설정한다
            if (doodleColor != null && pen === DoodlePen.MOSAIC && doodleColor.level != mMosaicLevel) {
                when (doodleColor.level) {
                    DoodlePath.MOSAIC_LEVEL_1 -> btn_mosaic_level_1.performClick()
                    DoodlePath.MOSAIC_LEVEL_2 -> btn_mosaic_level_2.performClick()
                    DoodlePath.MOSAIC_LEVEL_3 -> btn_mosaic_level_3.performClick()
                }
            }
        }

        /**
         * Doodle의 확대경 활성상태를 설정한다
         */
        override fun enableZoomer(enable: Boolean) {
            super.enableZoomer(enable)
            if (enable) {
                Toast.makeText(applicationContext, "x" + mDoodleParams.mZoomerScale, Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * Doodle의 Undo 진행하고 undo/redo 메뉴의 활성상태 갱신
         */
        override fun undo() {
            mTouchGestureListener.selectedItem = null
            val res = super.undo()
            updateDoodleUndoRedo()
        }

        /**
         * Doodle의 지우기를 진행하고 undo/redo 메뉴의 활성상태 갱신
         */
        override fun clear() {
            super.clear()
            mTouchGestureListener.selectedItem = null
            updateDoodleUndoRedo()
        }

        /**
         * Doodle에 그리기 item을 추가한후 undo/redo 메뉴의 활성상태 갱신
         */
        override fun addItem(item: IDoodleItem) {
            super.addItem(item)
            updateDoodleUndoRedo()
        }

        var mLastIsDrawableOutside: Boolean? = null

        /**
         * 편집상태를 설정한다
         */
        override fun setEditMode(editMode: Boolean) {
            if (editMode == isEditMode) {
                return
            }
            super.setEditMode(editMode)

            val editIcon = ContextCompat.getDrawable(context, R.drawable.baseline_open_with_24)!!
            if (editMode) {
                Toast.makeText(applicationContext, R.string.doodle_edit_mode, Toast.LENGTH_SHORT).show()
                // 편집전 상태 보관
                mLastIsDrawableOutside = mDoodle.isDrawableOutside
                mDoodle.setIsDrawableOutside(true)

                // 편집메뉴의 색갈설정
                editIcon.colorFilter = PorterDuffColorFilter(getColor(R.color.text_selected_color), PorterDuff.Mode.SRC_IN)
                menu_edit!!.findItem(R.id.menu_doodle_edit).icon = editIcon
                doodle_bottom_bar.beGone()
            } else {
                // 편집전 상태 복귀
                if (mLastIsDrawableOutside != null) mDoodle.setIsDrawableOutside(mLastIsDrawableOutside!!)
                mTouchGestureListener.center()
                if (mTouchGestureListener.selectedItem == null) {
                    setPen(pen)
                }
                mTouchGestureListener.selectedItem = null

                // 편집메뉴의 색갈설정
                menu_edit!!.findItem(R.id.menu_doodle_edit).icon = editIcon

                // Undo/Redo 메뉴 갱신
                updateDoodleUndoRedo()
                doodle_bottom_bar.beVisible()
            }
        }

        /**
         * View들의 단일선택 진행
         * @param ids 모든 View들의 ID Collection
         * @param selectedId 선택하려는 View의 ID
         */
        private fun setSingleSelected(ids: Collection<Int>, selectedId: Int) {
            for (id in ids) {
                this@EditActivity.findViewById<View>(id).isSelected = id == selectedId
            }
        }

        init {
            // 단일선택 HashMap에 ID들 추가
            mBtnShapeIds[DoodleShape.PAINTBRUSH] = R.id.btn_paint_brush
            mBtnShapeIds[DoodleShape.ARROW] = R.id.btn_arrow
            mBtnShapeIds[DoodleShape.HOLLOW_CIRCLE] = R.id.btn_holl_circle
            mBtnShapeIds[DoodleShape.HOLLOW_RECT] = R.id.btn_holl_rect
        }
    }
}
