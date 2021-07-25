package com.kr.gallery.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.kr.commons.activities.BaseSimpleActivity
import com.kr.commons.extensions.setupDialogStuff
import com.kr.gallery.pro.R

class SaveBeforeExitDialog(val activity: BaseSimpleActivity, callback: () -> Unit) {

    init {
        val view = activity.layoutInflater.inflate(R.layout.save_before_exit, null)

        AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setPositiveButton(activity.resources.getText(R.string.save_exit)) { dialog, which -> callback() }
                .setNegativeButton(activity.resources.getText(R.string.exit)) {dialog, which -> activity.finish() }
                .create().apply {
                    activity.setupDialogStuff(view, this)
                }
    }
}
