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
import kotlinx.android.synthetic.main.delete_folder.*
import kotlinx.android.synthetic.main.delete_folder.view.*

/**
 * @param isFile true이면 화일, false이면 등록부
 * @param isSingular true이면 1개 삭제, false이면 여러개삭제
 */
class DeleteFolderDialog(private var isFile: Boolean, private var isSingular: Boolean, var listener: DeleteDialogListener?): DialogFragment() {
    constructor(): this(false, false, null)

    companion object {
        const val IS_FILE = "is_file"
        const val IS_SINGULAR = "is_singular"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //Dialog에 animation 을 추가해준다
        val window = dialog!!.window!!
        window.attributes.windowAnimations = R.style.DialogAnimation

        val view = inflater.inflate(R.layout.delete_folder, container, false)
        view.delete_title.text = getString(
                if(isFile && isSingular)
                    R.string.delete_selected_file
                else if(isFile && !isSingular)
                    R.string.delete_selected_files
                else if(!isFile && isSingular)
                    R.string.delete_selected_folder
                else
                    R.string.delete_selected_folders
        )

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //확인, 취소 단추들의 click사건 정의
        cancel_btn.setOnClickListener {
            listener?.onCancel()
            dismiss()
        }

        delete_btn.setOnClickListener {
            listener?.onDelete()
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

    fun addListener(_listener: DeleteDialogListener?) {
        listener = _listener
    }

    //상태저장
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(IS_FILE, isFile)
        outState.putBoolean(IS_SINGULAR, isSingular)
    }

    //상태복귀
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if(savedInstanceState != null) {
            isFile = savedInstanceState.getBoolean(IS_FILE)
            isSingular = savedInstanceState.getBoolean(IS_SINGULAR)
        }
    }

    interface DeleteDialogListener {
        fun onCancel()
        fun onDelete()
    }
}
