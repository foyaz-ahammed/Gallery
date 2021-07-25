package com.kr.gallery.pro.dialogs

import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.kr.commons.activities.BaseSimpleActivity
import com.kr.commons.dialogs.FilePickerDialog
import com.kr.commons.extensions.*
import com.kr.commons.helpers.DATE_FORMAT_ONE
import com.kr.commons.views.MyGridLayoutManager
import com.kr.gallery.pro.R
import com.kr.gallery.pro.adapters.DirectoryAdapter
import com.kr.gallery.pro.extensions.*
import com.kr.gallery.pro.models.Directory
import kotlinx.android.synthetic.main.dialog_directory_picker.view.*

class PickDirectoryDialog(val activity: BaseSimpleActivity, val sourcePath: String, showOtherFolderButton: Boolean, val showFavoritesBin: Boolean,
                          val callback: (path: String) -> Unit) {
    private var dialog: AlertDialog
    private var shownDirectories = ArrayList<Directory>()
    private var allDirectories = ArrayList<Directory>()
    private var openedSubfolders = arrayListOf("")
    private var view = activity.layoutInflater.inflate(R.layout.dialog_directory_picker, null)
    private var currentPathPrefix = ""

    init {
        (view.directories_grid.layoutManager as MyGridLayoutManager).apply {
            orientation = RecyclerView.VERTICAL
            spanCount = 1
        }

        val builder = AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setOnKeyListener { dialogInterface, i, keyEvent ->
                    if (keyEvent.action == KeyEvent.ACTION_UP && i == KeyEvent.KEYCODE_BACK) {
                        backPressed()
                    }
                    true
                }

        if (showOtherFolderButton) {
            builder.setNeutralButton(R.string.other_folder) { dialogInterface, i -> showOtherFolder() }
        }

        dialog = builder.create().apply {
            activity.setupDialogStuff(view, this, R.string.select_destination) {}
        }
        com.kr.commons.helpers.Helper.adjustAlertDialogWidth(dialog, activity)
        fetchDirectories(false)
    }

    private fun fetchDirectories(forceShowHidden: Boolean) {
        activity.getCachedDirectories() {
            if (it.isNotEmpty()) {
                activity.runOnUiThread {
                    gotDirectories(it)
                }
            }
        }
    }

    private fun showOtherFolder() {
        FilePickerDialog(activity, sourcePath, false, true, true) {
            activity.handleLockedFolderOpening(it) { success ->
                if (success) {
                    callback(it)
                }
            }
        }
    }

    private fun gotDirectories(newDirs: ArrayList<Directory>) {
        if (allDirectories.isEmpty()) {
            allDirectories = newDirs.clone() as ArrayList<Directory>
        }

        val distinctDirs = newDirs.filter { showFavoritesBin }.distinctBy { it.path.getDistinctPath() }.toMutableList() as ArrayList<Directory>
        val dirs = getSortedDirectories(distinctDirs)
        if (dirs.hashCode() == shownDirectories.hashCode()) {
            return
        }

        shownDirectories = dirs
        val adapter = DirectoryAdapter(activity, dirs.clone() as ArrayList<Directory>, view.directories_grid, true) {
            val clickedDir = it as Directory
            val path = clickedDir.path
            if (path.trimEnd('/') == sourcePath) {
                activity.toast(R.string.source_and_destination_same)
                return@DirectoryAdapter
            } else {
                activity.handleLockedFolderOpening(path) { success ->
                    if (success) {
                        callback(path)
                    }
                }
                dialog.dismiss()
            }
        }

        val scrollHorizontally = false
        val dateFormat = DATE_FORMAT_ONE
        val timeFormat = activity.getTimeFormat()
        view.apply {
            directories_grid.adapter = adapter

            directories_vertical_fastscroller.isHorizontal = false
            directories_vertical_fastscroller.beGoneIf(scrollHorizontally)

            directories_vertical_fastscroller.setViews(directories_grid) {
                directories_vertical_fastscroller.updateBubbleText(dirs[it].getBubbleText(activity, dateFormat, timeFormat))
            }
        }
    }

    private fun backPressed() {
        if (activity.config.groupDirectSubfolders) {
            if (currentPathPrefix.isEmpty()) {
                dialog.dismiss()
            } else {
                openedSubfolders.removeAt(openedSubfolders.size - 1)
                currentPathPrefix = openedSubfolders.last()
                gotDirectories(allDirectories)
            }
        } else {
            dialog.dismiss()
        }
    }
}
