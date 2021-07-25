package com.kr.commons.helpers

import android.app.Activity
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog


class Helper {
    companion object {
        var isFirst: Boolean = true
        var selectAll: Boolean = true
        var isEmpty: Boolean = false
        var isRecording: Boolean = false
        var isShortcutStop: Boolean = false
        fun adjustAlertDialogWidth(dialog: AlertDialog, activity: Activity) {
            dialog.window?.setGravity(Gravity.BOTTOM)
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            val displayWidth = displayMetrics.widthPixels
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.window?.attributes)
            val dialogWindowWidth = (displayWidth * 0.95f).toInt()
            layoutParams.width = dialogWindowWidth
            dialog.window?.attributes = layoutParams

            val btnPositive: Button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val btnNegative: Button = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            val buttonLayout = btnPositive.layoutParams as LinearLayout.LayoutParams
            buttonLayout.weight = 10f
            btnPositive.layoutParams = buttonLayout
            btnNegative.layoutParams = buttonLayout
        }
    }
}
