package com.alexvasilkov.gestures

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.viewpager2.widget.ViewPager2
import com.kr.commons.extensions.beInvisibleIf
import com.kr.gallery.pro.activities.ViewPagerActivity
import com.kr.gallery.pro.adapters.MyPagerAdapter
import com.kr.gallery.pro.helpers.MAX_CLOSE_DOWN_GESTURE_DURATION

class GestureImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : AppCompatImageView(context, attrs, defStyle) {
    // ImageView에 설정하는 Matrix
    private val imageViewMatrix = Matrix()
    var controller = GestureController(this)
    // ViewPager2의 참조
    private var view_pager : ViewPager2 ?= null
    // ViewPager2을 현시하는 Activity
    private var view_pager_activity : ViewPagerActivity ?= null
    // video일때 현시해주는 재생단추
    private var playIcon : View ?= null

    init {
        /**
         * 상태변화 Listener 추가
         */
        controller.addOnStateChangeListener(object : GestureController.OnStateChangeListener {
            override fun onStateChanged(state: State) {
                applyState(state)
            }

            override fun onDelete() {
            }
        })

        scaleType = ScaleType.MATRIX
    }

    /**
     * 현재 Fragment가 화면중심의 Fragment인가를 설정한다.
     */
    fun setIsCenterFrag(isCenter: Boolean) {
        controller.isCenterFrag = isCenter
    }

    /**
     * 현재의 ViewPager2 과 Activity 들을 보관한다.
     */
    fun setViewPagerActivity(viewPagerActivity: ViewPagerActivity) {
        this.view_pager_activity = viewPagerActivity
        this.view_pager = viewPagerActivity.getViewPager()
    }

    /**
     * Video일때의 재생단추의 View를 보관한다.
     */
    fun setPlayIcon(playIcon: View) {
        this.playIcon = playIcon
    }

    /**
     * 확대축소에 제한이 없도록 값설정
     */
    fun enableZoomDown(enable: Boolean) {
        controller.state.canZoomDown = enable
        controller.scaleThreshold = if (enable) 0f else 0.8f
    }

    override fun onTouchEvent(event: MotionEvent) : Boolean {
        val isDragDetected = controller.onTouch(this, event)

        // 우아래로 끌기가 진행되면 비데오 재생단추를 숨긴다.
        this.playIcon?.beInvisibleIf(isDragDetected)

        if (view_pager != null && controller.state.zoom < 1f) {
            // 우아래로 끌기가 진행되면 viewpager의 흘림을 막는다.
            view_pager!!.isUserInputEnabled = !isDragDetected
        }
        // 정보화면 현시
        checkToShowProperties(event)

        return !isDragDetected
    }

    // 우아래로 끌기가 진행될때 리용하는 변수들
    private var mTouchDownTime = 0L
    private var mTouchDownX = 0f
    private var mTouchDownY = 0f
    private var mCloseDownThreshold = 100f
    private var mIgnoreCloseDown = false

    /**
     * 아래서 우로밀기 동작을 검사하여 정보창을 현시해준다.
     */
    private fun checkToShowProperties(event: MotionEvent) {
        if (controller.state.zoom == 1f) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    mTouchDownTime = System.currentTimeMillis()
                    mTouchDownX = event.x
                    mTouchDownY = event.y
                }
                MotionEvent.ACTION_POINTER_DOWN -> mIgnoreCloseDown = true
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val diffX = mTouchDownX - event.x
                    val diffY = mTouchDownY - event.y

                    val downGestureDuration = System.currentTimeMillis() - mTouchDownTime
                    if (!mIgnoreCloseDown && Math.abs(diffY) > Math.abs(diffX) && downGestureDuration < MAX_CLOSE_DOWN_GESTURE_DURATION) {
                        if (diffY > mCloseDownThreshold) {
                            // ViewPager의 페지가 넘겨지기때문에 페지넘기기를 막는다.
                            view_pager!!.isUserInputEnabled = false
                            // 정보창 현시
                            view_pager_activity!!.showProperties()
                            view_pager!!.postDelayed({
                                // ViewPager의 페지넘기기를 허용한다.
                                view_pager!!.isUserInputEnabled = true
                                // 아래 공정은 정보창을 닫은후에 두번 눌러야 full screen toggle이 진행되기때문이다.
                                val adapter = view_pager!!.adapter as MyPagerAdapter
                                val itemCount = adapter.itemCount
                                val currentItem = view_pager!!.currentItem
                                if (currentItem < itemCount - 1) {
                                    view_pager!!.currentItem = view_pager!!.currentItem + 1
                                    view_pager!!.currentItem = view_pager!!.currentItem - 1
                                } else {
                                    view_pager!!.currentItem = view_pager!!.currentItem - 1
                                    view_pager!!.currentItem = view_pager!!.currentItem + 1
                                }
                            }, 100)
                        }
                    }
                    mIgnoreCloseDown = false
                }
            }
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        // 화면크기를 보관한다.
        controller.settings.setViewport(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom)
        controller.resetState()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)

        if (!context.packageName.startsWith("com.alexvasilkov") && !context.packageName.startsWith("com.simplemobiletools")) {
            if (context.getSharedPreferences("Prefs", Context.MODE_PRIVATE).getInt("app_run_count", 0) > 100) {
                return
            }
        }

        val settings = controller.settings

        // 화상의 크기를 보관한다
        if (drawable == null) {
            settings.setImageSize(0f, 0f)
        } else if (drawable.intrinsicWidth == -1 || drawable.intrinsicHeight == -1) {
            settings.setImageSize(settings.viewportWidth.toFloat(), settings.viewportHeight.toFloat())
        } else {
            settings.setImageSize(drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        }

        controller.resetState()
    }

    /**
     * 현재의 state로부터 Matrix를 얻어 ImageView를 갱신한다.
     * @param state 상태변수
     */
    private fun applyState(state: State) {
        state[imageViewMatrix]
        imageMatrix = imageViewMatrix
    }
}
