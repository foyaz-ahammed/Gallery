package com.alexvasilkov.gestures

import android.graphics.RectF
import android.view.*
import java.util.*

class GestureController(private val targetView: View) : View.OnTouchListener {
    companion object {
        private const val STATE_CHANGE_IDLE_DURATION = 200L     // refocus on the image at dragging too, if the finger hasnt moved for this long
        private val tmpRectF = RectF()
        private val tmpPointArr = FloatArray(2)
    }

    // touch하여 움직였을때의 변화림계값
    private val touchSlop: Int
    // state 갱신 listener들의 배렬
    private val stateListeners = ArrayList<OnStateChangeListener>()

    // animation을 진행하는 클라스 객체
    private val animationEngine: AnimationEngine

    private val gestureDetector: GestureDetector
    private val scaleDetector: ScaleGestureDetector
    private val dragDetector: DragUpDownGestureDetector

    // 부모 View에서 touch사건을 처리하지 못하게 하는 기발, true: 불허용, false: 허용
    private var isInterceptTouchDisallowed = false
    // touch하여 끌기가 진행중인가를 나타낸다.
    private var isScrollDetected = false
    // 확대축소가 진행중인가를 나타낸다.
    private var isScaleDetected = false
    // 끌기가 진행중인가를 나타낸다.
    private var isDragDetected = false

    // animation에 리용되는 시작중심위치이다.
    private var pivotX = java.lang.Float.NaN
    private var pivotY = java.lang.Float.NaN
    // animation에 리용되는 마감중심위치이다.
    private var endPivotX = java.lang.Float.NaN
    private var endPivotY = java.lang.Float.NaN
    // touch사건 도중에 state가 갱신되였는가를 나타낸다.
    private var isStateChangedDuringTouch = false
    private var isAnimatingInBounds = false

    // animation에 리용되는 변화값을 생성
    private val stateScroller: FloatScroller
    // animation의 시작상태 변수
    private val stateStart = State()
    // animation의 마감상태 변수
    private val stateEnd = State()
    // 이전 상태 보관 변수
    private val prevState = State()
    // 화면크기, 화상크기, 기능여부 등 설정들을 보관한다.
    val settings: Settings
    // 위치, 회전각도 등 상태정보를 보관한다.
    val state = State()
    // state에 대한 갱신을 진행하는 클라스객체
    val stateController: StateController
    // slide 형식으로 보여질때의 축소비률이다. 0.8f보다 더 작게 축소되였으면 0.8f로, 크면은 1f로 확대된다.
    var scaleThreshold = 0.8f
    // 현재의 Fragment가 중심에 놓인 Fragment인가를 나타낸다.
    var isCenterFrag = false

    init {
        val context = targetView.context
        settings = Settings()
        stateController = StateController(settings)

        animationEngine = AnimationEngine(targetView)
        val internalListener = InternalGesturesListener()
        gestureDetector = GestureDetector(context, internalListener)
        scaleDetector = ScaleGestureDetectorFixed(context, internalListener)
        dragDetector = DragUpDownGestureDetector(internalListener)

        stateScroller = FloatScroller()

        val configuration = ViewConfiguration.get(context)
        touchSlop = configuration.scaledTouchSlop
    }

    fun addOnStateChangeListener(listener: OnStateChangeListener) {
        stateListeners.add(listener)
    }

    fun removeOnStateChangeListener(listener: OnStateChangeListener) {
        stateListeners.remove(listener)
    }

    /**
     * state를 초기화 한다.
     */
    fun resetState() {
        stopAllAnimations()
        val reset = stateController.resetState(state)
        if (reset) {
            notifyStateReset()
        } else {
            notifyStateUpdated()
        }
    }

    fun resetPrevState() {
        prevState.set(state)
    }

    private fun animateKeepInBounds() {
        if (state.zoom > scaleThreshold) {
            // 화상축소비률이 림계비률(0.8f)보다 크므로 원상으로(1f) 확대
            animateStateTo(state, true)
        } else {
            // 림계비률보다 더 축소되였으므로 림계비률로 확대
            val copy = state.copy()
            copy.zoom = scaleThreshold
            // 밀고 올로간 거리가 화면의 절반을 넘지못하면, 혹은 밀고 내려갔으면, 혹은 현재 fragment가 아닌경우 중심으로 이동
            // 이것은 gestureImageView의 onTouchEvent에서 삭제검사할때와 같아야 한다
            val deltaY = settings.viewportHeight / 2 - state.y - settings.imageHeight / 2
            if (deltaY < settings.viewportHeight / 2 || !isCenterFrag) {
                // 삭제하지 않을때
                copy.y = (settings.viewportHeight - settings.imageHeight) / 2f
                targetView.alpha = 1f
            } else {
                // 삭제할때
                copy.y = -settings.imageHeight - 10f
            }

            animateStateTo(copy, false)
        }
    }

    /**
     * 파라메터로 들어온 endState의 값들을 목표로 화상의 animation을 시작한다.
     */
    private fun animateStateTo(endState: State?, keepInBounds: Boolean = true) {
        if (endState == null) {
            return
        }

        var endStateRestricted: State? = null
        if (keepInBounds) {
            endStateRestricted = stateController.restrictStateBoundsCopy(endState, prevState, pivotX, pivotY)
        }

        if (endStateRestricted == null) {
            endStateRestricted = endState
        }

        if (endStateRestricted == state) {
            return
        }

        // 진행중인 animation은 중지한다.
        stopAllAnimations()

        isAnimatingInBounds = keepInBounds
        stateStart.set(state)
        stateEnd.set(endStateRestricted)

        // 시작중심점에 시작/마감 state의 변화를 적용하여 마감중심점을 얻는다.
        if (!java.lang.Float.isNaN(pivotX) && !java.lang.Float.isNaN(pivotY)) {
            tmpPointArr[0] = pivotX
            tmpPointArr[1] = pivotY
            MathUtils.computeNewPosition(tmpPointArr, stateStart, stateEnd)
            endPivotX = tmpPointArr[0]
            endPivotY = tmpPointArr[1]
        }
        // animation에 리용되는 변화값 구간 설정
        stateScroller.startScroll(0f, 1f)
        // animation 시작
        animationEngine.start()
    }

    /**
     * 진행중인 animation을 중지시킨다.
     */
    private fun stopStateAnimation() {
        if (!stateScroller.isFinished) {
            stateScroller.forceFinished()
            onStateAnimationFinished()
        }
    }

    /**
     * 모든 animation을 중지시킨다.
     */
    private fun stopAllAnimations() {
        stopStateAnimation()
    }

    /**
     * animation이 끝났을때 호출되는 함수
     */
    private fun onStateAnimationFinished() {
        isAnimatingInBounds = false
        pivotX = java.lang.Float.NaN
        pivotY = java.lang.Float.NaN
        if (state.zoom < 1f && state.y < -settings.imageHeight / 2) {
            // delete
            stateListeners.forEach {
                it.onDelete()
            }
        }
    }

    /**
     * state가 갱신되였다는것을 listener들에 알려준다.
     */
    private fun notifyStateUpdated() {
        prevState.set(state)
        stateListeners.forEach {
            it.onStateChanged(state)
        }
    }

    /**
     * state가 초기화되였다는것을 listener들에 알려준다.
     */
    private fun notifyStateReset() {
        stateListeners.forEach {
            it.onStateChanged(state)
        }
        notifyStateUpdated()
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        onTouchInternal(view, event)

        return settings.getIsEnabled() && isDragDetected
    }

    private fun onTouchInternal(view: View, event: MotionEvent): Boolean {
        val viewportEvent = MotionEvent.obtain(event)
        viewportEvent.offsetLocation((-view.paddingLeft).toFloat(), (-view.paddingTop).toFloat())

        gestureDetector.setIsLongpressEnabled(view.isLongClickable)
        gestureDetector.onTouchEvent(viewportEvent)
        scaleDetector.onTouchEvent(viewportEvent)

        // 끌기는 축소되였을때만 가능하다.(우로 밀어 삭제동작)
        isDragDetected = if (state.zoom == scaleThreshold) dragDetector.onTouchEvent(viewportEvent)
        else false

        if (isStateChangedDuringTouch) {
            // touch로 하여 state가 갱신되였다.
            isStateChangedDuringTouch = false
            // 떨림을 방지한다.
            stateController.restrictStateBounds(state, prevState, pivotX, pivotY, true)
            if (state != prevState) {
                notifyStateUpdated()
            }
        }

        if (viewportEvent.actionMasked == MotionEvent.ACTION_UP || viewportEvent.actionMasked == MotionEvent.ACTION_CANCEL) {
            onUpOrCancel(viewportEvent)
        }

        if (!isInterceptTouchDisallowed && shouldDisallowInterceptTouch(viewportEvent)) {
            // touch 사건을 부모가 간섭하지 못하게 한다.
            isInterceptTouchDisallowed = true

            val parent = view.parent
            parent?.requestDisallowInterceptTouchEvent(true)
        }

        viewportEvent.recycle()
        return isDragDetected
    }

    private fun shouldDisallowInterceptTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                stateController.getMovementArea(state, tmpRectF)
                // 화상을 좌우로 움직일수 있는지 검사
                val isPannable = State.compare(tmpRectF.width()) > 0 || State.compare(tmpRectF.height()) > 0
                if (isPannable) {
                    return true
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                return settings.isZoomEnabled
            }
        }

        return false
    }

    private fun onDown(event: MotionEvent): Boolean {
        isInterceptTouchDisallowed = false

        return false
    }

    private fun onUpOrCancel(event: MotionEvent) {
        isScrollDetected = false
        isScaleDetected = false

        if (!isAnimatingInBounds) {
            animateKeepInBounds()
        }
    }

    private fun onSingleTapUp(event: MotionEvent): Boolean {
        if (!settings.isDoubleTapEnabled()) {
            targetView.performClick()
        }

        return false
    }

    private fun onLongPress(event: MotionEvent) {
        if (settings.getIsEnabled()) {
            targetView.performLongClick()
        }
    }

    private fun onScroll(e1: MotionEvent, e2: MotionEvent, dx: Float, dy: Float): Boolean {
        if (!stateScroller.isFinished) {
            return false
        }

        if (state.zoom >= 1f || scaleThreshold == 0f) {
            // 확대하였을때 끌기 기능
            if (!isScrollDetected) {
                // 끌기한 거리가 림계값보다 크면 끌기로 판정
                isScrollDetected = Math.abs(e2.x - e1.x) > touchSlop || Math.abs(e2.y - e1.y) > touchSlop

                if (isScrollDetected) {
                    return false
                }
            }

            if (isScrollDetected) {
                // 화상이동 진행
                state.translateBy(-dx, -dy)
                isStateChangedDuringTouch = true
            }
        }

        return isScrollDetected
    }

    private fun onFling(vx: Float, vy: Float): Boolean {
        return true
    }

    private fun onSingleTapConfirmed(event: MotionEvent): Boolean {
        if (state.zoom < 1f && scaleThreshold > 0f) {
            // viewpager가 축소되였을때 한번 누르면 초기상태로 확대시킨다.
            val copy = state.copy()
            copy.zoom = 1f
            animateStateTo(copy, false)
        } else {
            // 화면 한번 누르기 진행. toggle full screen
            if (settings.isDoubleTapEnabled()) {
                targetView.performClick()
            }
        }

        return false
    }

    private fun onDoubleTapEvent(event: MotionEvent): Boolean {
        if (!settings.isDoubleTapEnabled()) {
            return false
        }

        if (event.actionMasked != MotionEvent.ACTION_UP) {
            return false
        }

        if (isScaleDetected) {
            return false
        }

        if (!settings.swallowDoubleTaps) {
            if (state.zoom < 1f && scaleThreshold > 0f) {
                // viewpager가 축소되였을때 두번 누르면 초기상태로 확대시킨다.
                val copy = state.copy()
                copy.zoom = 1f
                animateStateTo(copy, false)
            } else {
                // 두번눌렀을때 확대축소 진행
                animateStateTo(stateController.toggleMinMaxZoom(state, event.x, event.y))
            }
        }
        return true
    }

    private fun onScaleBegin(): Boolean {
        isScaleDetected = settings.isZoomEnabled
        // 끌어올려 삭제할때에는 확대축소를 진행하지 않는다.
        return isScaleDetected && !isDragDetected
    }

    private fun onScale(detector: ScaleGestureDetector): Boolean {
        if (!settings.isZoomEnabled || !stateScroller.isFinished || isDragDetected) {
            return false
        }

        val scaleFactor = detector.scaleFactor
        pivotX = detector.focusX
        pivotY = detector.focusY

        if (state.zoom * scaleFactor >= 1f && scaleThreshold > 0f) {
            if (state.zoom < 1f) {
                // 실례로 0.8f에서 1.223으로 넘어갈때 viewpager확대를 1f로 맞추어 준다
                state.zoom = 1f
                prevState.zoom = 1f

                // 0.999f을 설정하여 onStateChanged에서 1f를 설정하도록 한다
                val copy = state.copy()
                copy.zoom = 0.999f
                stateListeners.forEach {
                    it.onStateChanged(copy)
                }
            }
        }

        state.zoomBy(scaleFactor, pivotX, pivotY)
        isStateChangedDuringTouch = true
        return true
    }

    private fun onScaleEnd() {
        isScaleDetected = false
    }

    private fun onDrag(yDelta: Float) {
        isDragDetected = true

        val deltaY = settings.viewportHeight / 2 - state.y - settings.imageHeight / 2
        if (deltaY > 0) {
            // 끌어 올릴때 alpha값을 작게하기
            val alpha = 1f - deltaY / (settings.viewportHeight / 2) * 0.7f
            targetView.alpha = alpha
        } else targetView.alpha = 1f

        // 끌어올리기 진행
        state.translateBy(0f, yDelta)
        isStateChangedDuringTouch = true
    }

    /**
     * 파라메터로 들어온 View에 대하여 animation을 진행하는 Runnable 클라스
     * @param view animation이 진행되는 View
     */
    private inner class AnimationEngine(val view: View) : Runnable {
        // animation이 진행되는 Frame들의 시간간격
        private val FRAME_TIME = 10L

        fun onStep(): Boolean {
            var shouldProceed = false

            if (!stateScroller.isFinished) {
                stateScroller.computeScroll()
                val factor = stateScroller.curr

                if (java.lang.Float.isNaN(pivotX) || java.lang.Float.isNaN(pivotY) || java.lang.Float.isNaN(endPivotX) || java.lang.Float.isNaN(endPivotY)) {
                    MathUtils.interpolate(state, stateStart, stateEnd, factor)
                } else {
                    MathUtils.interpolate(state, stateStart, pivotX, pivotY, stateEnd, endPivotX, endPivotY, factor)
                }

                shouldProceed = true

                if (stateScroller.isFinished) {
                    onStateAnimationFinished()
                }
            }

            if (shouldProceed) {
                notifyStateUpdated()
            }

            return shouldProceed
        }

        /**
         * Runnable을 실행하고 여부에 따라 새로운 실행을 계획한다.
         */
        override fun run() {
            if (onStep()) {
                scheduleNextStep()
            }
        }

        /**
         * animation의 다음 Frame을 실행시킨다.
         */
        private fun scheduleNextStep() {
            view.removeCallbacks(this)
            view.postOnAnimationDelayed(this, FRAME_TIME)
        }

        fun start() {
            scheduleNextStep()
        }
    }

    interface OnStateChangeListener {
        fun onStateChanged(state: State)
        fun onDelete()
    }

    private inner class InternalGesturesListener : GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener, DragUpDownGestureDetector.OnDragGestureListener {

        override fun onSingleTapConfirmed(event: MotionEvent) = this@GestureController.onSingleTapConfirmed(event)

        override fun onDoubleTap(event: MotionEvent) = false

        override fun onDoubleTapEvent(event: MotionEvent) = this@GestureController.onDoubleTapEvent(event)

        override fun onDown(event: MotionEvent) = this@GestureController.onDown(event)

        override fun onShowPress(event: MotionEvent) {}

        override fun onSingleTapUp(event: MotionEvent) = this@GestureController.onSingleTapUp(event)

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float) = this@GestureController.onScroll(e1, e2, distanceX, distanceY)

        override fun onLongPress(event: MotionEvent) {
            this@GestureController.onLongPress(event)
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float) = this@GestureController.onFling(velocityX, velocityY)

        override fun onScale(detector: ScaleGestureDetector) = this@GestureController.onScale(detector)

        override fun onScaleBegin(detector: ScaleGestureDetector) = this@GestureController.onScaleBegin()

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            this@GestureController.onScaleEnd()
        }

        override fun onDrag(detector: DragUpDownGestureDetector, yDelta: Float) {
            this@GestureController.onDrag(yDelta)
        }
    }
}
