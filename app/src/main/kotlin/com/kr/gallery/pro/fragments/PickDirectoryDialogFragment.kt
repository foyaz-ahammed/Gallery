package com.kr.gallery.pro.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.kr.commons.activities.BaseSimpleActivity
import com.kr.commons.extensions.*
import com.kr.commons.helpers.DATE_FORMAT_ONE
import com.kr.commons.views.MyGridLayoutManager
import com.kr.gallery.pro.R
import com.kr.gallery.pro.adapters.DirectoryAdapter
import com.kr.gallery.pro.extensions.*
import com.kr.gallery.pro.models.Directory
import kotlinx.android.synthetic.main.dialog_directory_picker.*
import java.io.Serializable

/**
 * 이동화면에 해당한 fragment
 */
class PickDirectoryDialogFragment : DialogFragment() {
    private var shownDirectories = ArrayList<Directory>()
    private var allDirectories = ArrayList<Directory>()
    private lateinit var sourcePath: String

    companion object {
        const val SRC_PATH = "path"
        const val CALLBACK = "callback"
        fun newInstance(path: String, callback: (path: String) -> Unit) : PickDirectoryDialogFragment {
            val fragment = PickDirectoryDialogFragment()
            val bundle = Bundle().apply {
                putString(SRC_PATH, path)
                putSerializable(CALLBACK, callback as Serializable)
            }
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_App_Dialog_FullScreenWithoutOpacity)

        sourcePath = arguments?.getString(SRC_PATH).toString()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_directory_picker,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireDialog().window?.setWindowAnimations(R.style.DialogAnimation)

        (directories_grid.layoutManager as MyGridLayoutManager).apply {
            orientation = RecyclerView.VERTICAL
            spanCount = 1
        }

        fetchDirectories()
        back_from_move.setOnClickListener {
            dismiss()
        }
    }

    private fun fetchDirectories() {
        activity?.getCachedDirectories() { it ->
            if (it.isNotEmpty()) {
                activity?.runOnUiThread {
                    gotDirectories(it)
                }
            }
        }
    }

    private fun gotDirectories(newDirs: ArrayList<Directory>) {
        if (allDirectories.isEmpty()) {
            allDirectories = newDirs.clone() as ArrayList<Directory>
        }

        val distinctDirs = newDirs.distinctBy { it.path.getDistinctPath() }.toMutableList() as ArrayList<Directory>
        val dirs = getSortedDirectories(distinctDirs)
        if (dirs.hashCode() == shownDirectories.hashCode()) {
            return
        }

        shownDirectories = dirs
        val adapter = DirectoryAdapter(activity as BaseSimpleActivity, dirs.clone() as ArrayList<Directory>, directories_grid, true) {
            val clickedDir = it as Directory
            val path = clickedDir.path
            if (path.trimEnd('/') == sourcePath) {
                activity?.toast(R.string.source_and_destination_same)
                return@DirectoryAdapter
            } else {
                activity?.handleLockedFolderOpening(path) { success ->
                    if (success) {
                        val callback: (path: String) -> Unit = arguments?.getSerializable(CALLBACK) as (path: String) -> Unit
                        callback(path)
                    }
                }
                dialog!!.dismiss()
            }
        }

        val dateFormat = DATE_FORMAT_ONE
        val timeFormat = activity?.getTimeFormat()
        view.apply {
            directories_grid.adapter = adapter

            directories_vertical_fastscroller.isHorizontal = false
            directories_vertical_fastscroller.beGoneIf(false)

            directories_vertical_fastscroller.setViews(directories_grid) {
                activity?.let { it1->
                    directories_vertical_fastscroller.updateBubbleText(dirs[it].getBubbleText(it1, dateFormat, timeFormat))
                }
            }
        }
    }
}
