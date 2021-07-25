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
import kotlinx.android.synthetic.main.rename_file.*

/**
 * 등록부|화일의 이름변경, 혹은 새 등록부를 창조할때 띄워주는 Dialog이다
 * @param folderName 등록부|화일이름 입력칸의 기정입력값
 */
class RenameFileDialog(private var folderName: String,
                       var listener: NameSetListener?): DialogFragment() {

    constructor(): this("", null)

    companion object {
        const val FOLDER_NAME = "folder_name"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //Dialog에 animation 을 추가해준다
        val window = dialog!!.window!!
        window.attributes.windowAnimations = R.style.DialogAnimation

        return inflater.inflate(R.layout.rename_file, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog_header.text = resources.getText(R.string.rename_dialog_title)
        folder_name.setText(folderName)

        //확인, 취소단추의 눌림동작들을 추가한다.
        cancel_btn.setOnClickListener {
            listener?.onCancel()
            dismiss()
        }

        ok_btn.setOnClickListener {
            listener?.onNameSet(folder_name.text.toString())
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

        //너비를 화면너비만큼 설정, 배경은 투명하게 앉혀주고 화면아래에 위치하도록 해준다
        val window = dialog!!.window!!
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setGravity(Gravity.BOTTOM)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //등록부|화일이름을 보관한다
        outState.putString(FOLDER_NAME, folderName)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if(savedInstanceState != null) {
            //보관했던 등록부|화일이름을 복귀한다
            folderName = savedInstanceState.getString(FOLDER_NAME, "")
        }
    }

    fun addListener(_listener: NameSetListener?) {
        listener = _listener
    }

    interface NameSetListener{
        fun onCancel()
        fun onNameSet(name: String)
    }
}
