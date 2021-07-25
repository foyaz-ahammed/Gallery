package com.kr.gallery.pro.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.kr.gallery.pro.R
import kotlinx.android.synthetic.main.transfer_method_select.*

/**
 * 전송단추를 눌렀을때 띄워주는 dialog이다.
 */
class TransferMethodSelectionDialog(var listener: TransferDialogListener? = null): DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //Dialog에 animation 을 추가해준다
        val window = dialog!!.window!!
        window.attributes.windowAnimations = R.style.DialogAnimation

        return inflater.inflate(R.layout.transfer_method_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //단추들의 click사건동작을 추가한다.
        message_btn.setOnClickListener {
            listener?.onMessage()
            dismiss()
        }
        note_btn.setOnClickListener {
            listener?.onNote()
            dismiss()
        }
        bluetooth_btn.setOnClickListener {
            listener?.onBluetooth()
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(activity as Context, theme) {
            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                //바깥령역에 touch하면 listener 의 onCancel 을 호출한다
                val dialogBounds = Rect()
                window!!.decorView.getHitRect(dialogBounds)
                if(!dialogBounds.contains(ev.x.toInt(), ev.y.toInt()))
                    listener?.onCancel()
                return super.dispatchTouchEvent(ev)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        //Back단추가 눌리면 listener 의 onCancel 을 호출한다
        dialog!!.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                listener?.onCancel()
            }
            false
        }
    }

    override fun onStart() {
        super.onStart()

        //너비난 화면너비와 같게, 바깥배경은 투명하게, 위치는 화면아래에 떨구어준다.
        val window = dialog!!.window!!
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setGravity(Gravity.BOTTOM)
    }

    fun addListener(_listener: TransferDialogListener) {
        listener = _listener
    }

    interface TransferDialogListener {
        fun onMessage()
        fun onNote()
        fun onBluetooth()
        fun onCancel()
    }
}
