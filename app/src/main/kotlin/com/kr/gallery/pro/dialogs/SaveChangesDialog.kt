package com.kr.gallery.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.kr.commons.activities.BaseSimpleActivity
import com.kr.commons.extensions.setupDialogStuff
import com.kr.gallery.pro.R

class SaveChangesDialog(val activity: BaseSimpleActivity, discardCallback: () -> Unit, saveCallback: () -> Unit ) {

    init {
        val view = activity.layoutInflater.inflate(R.layout.save_changes, null)

        AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setPositiveButton(activity.resources.getText(R.string.save)) { dialog, which -> saveCallback() }
                .setNegativeButton(activity.resources.getText(R.string.discard)) {dialog, which -> discardCallback() }
                .create().apply {
                    activity.setupDialogStuff(view, this)
                }
    }
}
