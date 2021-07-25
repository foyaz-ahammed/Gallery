package com.kr.gallery.pro.custom

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.kr.gallery.pro.R
import kotlinx.android.synthetic.main.snackbar.view.*

//label과 단추를 가진 view
class CustomSnackBar(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attrs, defStyleAttr) {
    constructor(context: Context?, attrs: AttributeSet?): this(context, attrs, 0)
    constructor(context: Context?): this(context, null)

    var parentView: ViewGroup? = null
    //단추를 눌렀을때 호출되는 callback
    var actionCallback: (() -> Unit)? = null
    //Snackbar가 없어진다음에 호출되는 callback
    var callbackOnFinish: (() -> Unit)? = null

    //Snack bar가 animation을 통해 없어질때 단추사건을 막아주기 위한 flag변수
    var shouldStop = false

    private val actionRunnable = Runnable {
        shouldStop = true
        disappearWithAnimation()
    }

    init {
        View.inflate(context, R.layout.snackbar, this)

        action.setOnClickListener {
            if(shouldStop)
                return@setOnClickListener

            closeSnackBar(true)
            actionCallback?.invoke()
        }

        postDelayed(actionRunnable, SNACK_BAR_DURATION)
    }

    /**
     * Fade, scale animation과 함께 보여진다
     * @see CustomSnackBar.show
     */
    fun appearWithAnimation() {
        alpha = 0f
        scaleX = 0.8f
        scaleY = 0.8f
        animate().alpha(1f).withLayer().scaleX(1f).scaleY(1f).setDuration(DURATION).start()
    }

    /**
     * Snack bar를 없앤다.
     * @param animate: animation을 보여주겠는가?
     * @param ignoreFinishCallback: [callbackOnFinish]를 호출안해주겠는가?
     */
    fun closeSnackBar(animate: Boolean, ignoreFinishCallback: Boolean = true) {
        shouldStop = true
        removeCallbacks(actionRunnable)

        if(animate) {
            if(ignoreFinishCallback) callbackOnFinish = null
            disappearWithAnimation()
        }
        else {
            parentView?.removeView(this)
            callbackOnFinish?.invoke()
        }
    }

    //Fade, scale animation이 진행되면서 없어진다
    private fun disappearWithAnimation() {
        val animation = animate().alpha(0f).withLayer().scaleX(0.8f).scaleY(0.8f).setDuration(DURATION)
        animation.setListener(object: AnimatorListenerAdapter(){
            override fun onAnimationEnd(animation: Animator?) {
                parentView?.removeView(this@CustomSnackBar)
                callbackOnFinish?.invoke()
            }
        })
        animation.start()
    }

    companion object{
        //Snack bar가 보여질때|사라질때 animation지속시간
        private const val DURATION = 300L
        //Snack bar현시시간
        private const val SNACK_BAR_DURATION = 4000L

        /**
         * @param context
         * @param _parentView: snack bar의 부모 view, activity의 root view가 될수 있다.
         * @param labelStringResId: 제목에 해당한 text
         * @param actionStringResId: 단추에 해당한 text
         * @param _actionCallback: 단추를 click했을때 호출되는 callback
         * @param _finishCallback: snack bar가 없어질때 호출되는 callback
         */
        fun show(context: Context, _parentView: ViewGroup, labelStringResId: Int, actionStringResId: Int, itemCount: Int,  _actionCallback: (() -> Unit), _finishCallback: (() -> Unit)): CustomSnackBar {
            val snackBar = CustomSnackBar(context)

            snackBar.apply {
                _parentView.addView(this)
                parentView = _parentView
                actionCallback = _actionCallback
                callbackOnFinish = _finishCallback
                label.text = context.getString(labelStringResId)
                action.text = context.getString(actionStringResId)
                tag = itemCount
                appearWithAnimation()
            }

            return snackBar
        }
    }
}
