package com.kr.gallery.pro.activities

import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.kr.commons.extensions.deleteFileWithPath
import com.kr.commons.extensions.toast
import com.kr.commons.helpers.*
import com.kr.gallery.pro.R
import com.kr.gallery.pro.adapters.HiddenDirectoryListAdapter
import com.kr.gallery.pro.adapters.SelectionUpdateListener
import com.kr.gallery.pro.dialogs.DeleteFolderDialog
import com.kr.gallery.pro.extensions.*
import com.kr.gallery.pro.helpers.*
import com.kr.gallery.pro.models.HiddenDirectory
import com.kr.gallery.pro.services.MediaBackgroundService
import com.kr.gallery.pro.viewmodels.HiddenMediaViewModel
import com.kr.gallery.pro.views.KrBottomSheet
import kotlinx.android.synthetic.main.activity_manage_hidden_folder.*
import kotlinx.android.synthetic.main.bottom_sheet_behavior.*
import java.util.LinkedHashSet

/**
 * 숨긴 등록부관리 페지
 * @see SettingsActivity
 */
class HiddenFoldersActivity : SimpleActivity(), SelectionUpdateListener, DeleteFolderDialog.DeleteDialogListener {
    //Bottom bar
    lateinit var bottomSheetBehavior: BottomSheetBehavior<KrBottomSheet>

    /* Activity파괴시 저장할 상태변수들 */
    private var dialogOpened = false
    private var dialogType = -1

    //Adapter
    private lateinit var mDirectoryListAdapter: HiddenDirectoryListAdapter

    //Progress Dialog상태를 저장하기 위한 변수들
    var progressDialog: ProgressDialog? = null
    var progressType = 0

    //Broadcast수신
    private var mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action?:return
            when(action) {
                SHOW_START -> {
                    //Progress대화창 띄워주기
                    val count = intent.getIntExtra(DATA_COUNT, 0)
                    val progress = intent.getIntExtra(CURRENT_PROGRESS, 0)
                    progressType = PROGRESS_TYPE_SHOW
                    progressDialog = makeProgressDialog(count, getString(R.string.unhiding))
                    progressDialog!!.progress = progress
                    progressDialog!!.show()
                }
                ACTION_PROGRESS -> {
                    //Progress대화창 progress값갱신
                    if(progressDialog != null && !isDestroyed) {
                        val progress = intent.getIntExtra(ACTION_PROGRESS, 0)
                        progressDialog!!.progress = progress
                    }
                }
                ACTION_COMPLETE -> {
                    //Progress대화창 닫고 toast띄워주기
                    if(progressDialog != null && !isDestroyed) {
                        progressDialog!!.dismiss()
                        progressDialog = null

                        val failedMediaCount = intent.getIntExtra(FAILED_MEDIA_COUNT, 0)

                        mDirectoryListAdapter.clearSelection()
                        onSelectionUpdate()

                        if(failedMediaCount == 0) toast(R.string.directory_unhide_complete)
                        else toast(getString(R.string.directory_unhide_failed, failedMediaCount), Toast.LENGTH_LONG)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var bottomBarState = BottomSheetBehavior.STATE_HIDDEN
        val selectedKeys: LinkedHashSet<Int>
        var allSelected = false
        if(savedInstanceState != null) {
            bottomBarState = savedInstanceState.getInt(BOTTOM_BAR_STATE)
            dialogOpened = savedInstanceState.getBoolean(DIALOG_OPENED, false)
            dialogType = savedInstanceState.getInt(DIALOG_TYPE, -1)
            selectedKeys = savedInstanceState.getSerializable(SELECTED_KEYS)!! as LinkedHashSet<Int>
            allSelected = savedInstanceState.getBoolean(ALL_SELECTED, false)

            //Progress dialog상태 불러들이기
            val hasProgress = savedInstanceState.getBoolean(HAS_PROGRESS, false)
            if(hasProgress) {
                progressType = savedInstanceState.getInt(PROGRESS_TYPE)
                val progressMax = savedInstanceState.getInt(PROGRESS_MAX)
                val currentProgress = savedInstanceState.getInt(CURRENT_PROGRESS)

                progressDialog = makeProgressDialog(progressMax, getString(
                        when (progressType) {
                            PROGRESS_TYPE_HIDE -> R.string.hiding
                            PROGRESS_TYPE_SHOW -> R.string.unhiding
                            PROGRESS_TYPE_DELETE -> R.string.deleting
                            else -> R.string.moving
                        }
                ))
                progressDialog!!.progress = currentProgress
                progressDialog!!.show()
            }
        }
        else {
            selectedKeys = LinkedHashSet()
        }

        setContentView(R.layout.activity_manage_hidden_folder)
        supportActionBar?.hide()

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = bottomBarState
        buildSheet()
        bottomSheetBehavior.disableDragging(bottomBarState)

        //Recyclerview에 layoutManager와 adapter설정
        mDirectoryListAdapter = HiddenDirectoryListAdapter(this, directories_recycler_view, this)
        mDirectoryListAdapter.selectedKeys = selectedKeys
        mDirectoryListAdapter.setSelectable(selectedKeys.size > 0)
        directories_recycler_view.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        directories_recycler_view.adapter = mDirectoryListAdapter
        updateSelectedCount(selectedKeys.size, allSelected)

        val viewModel = ViewModelProvider(this).get(HiddenMediaViewModel::class.java)
        viewModel.hiddenDirectories.observe(this){
            mDirectoryListAdapter.setClickListener { any: Any, _: View? ->
                val clickedDir = any as HiddenDirectory
                val path = clickedDir.folderPath

                //등록부보기화면 열기
                Intent(this, MediaActivity::class.java).apply {
                    putExtra(DIRECTORY, path)
                    putExtra(SHOW_HIDDEN, true)
                    putExtra(DATE, "")
                    putExtra(MEDIA_TYPE, 0)
                    startActivity(this)
                }
            }

            hidden_folders_help.visibility = if(it.isEmpty()) View.VISIBLE else View.GONE
            mDirectoryListAdapter.submitList(it as ArrayList<HiddenDirectory>)
        }

        //Activity가 파괴되기전에 Dialog가 이미 펼쳐져있는 상태라면 다시 현시해준다
        if(dialogOpened) {
            if(dialogType == DELETE_DIALOG) {
                val dialog = supportFragmentManager.findFragmentByTag(DELETE_DIALOG_TAG) as DeleteFolderDialog
                dialog.addListener(this)
            }
        }

        //BroadcastReceiver 등록
        val intentFilterLocal = IntentFilter()
        intentFilterLocal.addAction(SHOW_START)
        intentFilterLocal.addAction(ACTION_PROGRESS)
        intentFilterLocal.addAction(ACTION_COMPLETE)
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilterLocal)
    }

    //Bottom bar에 삭제, 현시단추 추가
    private fun buildSheet() {
        bottomSheet.addItem(
                applicationContext,
                KEY_DELETE,
                R.drawable.ic_outline_delete_24,
                R.string.delete,
                false
        )

        bottomSheet.addItem(
                applicationContext,
                KEY_SHOW,
                R.drawable.ic_outline_show_24,
                R.string.show,
                false
        )

        bottomSheet.itemMap[KEY_DELETE]!!.setOnClickListener{
            onClickDelete()
        }
        bottomSheet.itemMap[KEY_SHOW]!!.setOnClickListener{
            onClickShow()
        }
    }

    private fun onClickDelete(){
        if(dialogOpened)
            return

        hideBottomSheet()
        val dialog = DeleteFolderDialog(false, mDirectoryListAdapter.getSelectedCounts() == 1, this)
        dialog.show(supportFragmentManager, DELETE_DIALOG_TAG)

        //상태보관
        dialogOpened = true
        dialogType = DELETE_DIALOG
    }

    private fun onClickShow(){
        val hiddenFolders = mDirectoryListAdapter.getSelectedPaths()
        //자료기지접근이 필요하므로 thread상에서 실행시킨다
        ensureBackgroundThread {
            val hiddenFilePaths = hiddenMediumDao.getHiddenMediaByDirectory(hiddenFolders) as ArrayList<String>

            runOnUiThread {
                handleSAFDialog(hiddenFilePaths) {
                    if(it) {
                        val serviceIntent = Intent().apply {
                            setClass(this@HiddenFoldersActivity, MediaBackgroundService::class.java)
                            action = SHOW_MEDIA_ACTION
                            putExtra(REMAINING_DATA, hiddenFilePaths)
                            putExtra(CURRENT_PROGRESS, 0)
                            putExtra(DATA_COUNT, hiddenFilePaths.size)
                        }

                        startService(serviceIntent)
                    }
                    else {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        dialogOpened = false
                    }
                }
            }
        }
    }

    //모두 선택/취소를 눌렀을때
    fun onSelectAll(view: View) {
        if(mDirectoryListAdapter.allSelected())
            mDirectoryListAdapter.deselectAll()
        else
            mDirectoryListAdapter.selectAll()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        /*-- 현재의 상태들을 보관한다 --*/
        outState.putSerializable(SELECTED_KEYS, mDirectoryListAdapter.selectedKeys)
        outState.putBoolean(ALL_SELECTED, mDirectoryListAdapter.allSelected())
        outState.putInt(BOTTOM_BAR_STATE, bottomSheetBehavior.state)
        outState.putBoolean(DIALOG_OPENED, dialogOpened)
        outState.putInt(DIALOG_TYPE, dialogType)

        //Progress dialog상태를 저장한다
        outState.putBoolean(HAS_PROGRESS, progressDialog != null)
        if(progressDialog != null) {
            outState.putInt(PROGRESS_MAX, progressDialog!!.max)
            outState.putInt(CURRENT_PROGRESS, progressDialog!!.progress)
            outState.putInt(PROGRESS_TYPE, progressType)
        }
    }

    override fun onDestroy() {
        if(progressDialog != null) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
        super.onDestroy()

        //Unregister broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver)
    }

    override fun showBottomSheet(){
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun hideBottomSheet(){
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    //Back단추를 눌렀을때 선택된 등록부가 있으면 선택을 없애고 선택된것이 없으면 back동작을 진행한다
    override fun onBackPressed() {
        if(bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED){
            mDirectoryListAdapter.deselectAll()
        }
        else {
            super.onBackPressed()
        }
    }

    override fun onSelectionUpdate() {
        updateSelectedCount(mDirectoryListAdapter.getSelectedCounts(), mDirectoryListAdapter.allSelected())
    }

    private fun updateSelectedCount(count: Int, allSelected: Boolean) {
        if(count == 0) {
            hideBottomSheet()

            title_view.visibility = View.VISIBLE
            selected_counts_label.visibility = View.GONE
        }
        else {
            showBottomSheet()

            title_view.visibility = View.GONE
            selected_counts_label.visibility = View.VISIBLE
            display_selected_items.text = getString(R.string.select_with_count, count)

            if(allSelected)
                select_all_items.text = getString(R.string.deselect_all)
            else
                select_all_items.text = getString(R.string.select_all)
        }

        mDirectoryListAdapter.setSelectable(count > 0)
    }

    fun onBackSelect(view: View) {
        finish()
    }

    //삭제대화창에서 취소를 눌렀을때
    override fun onCancel() {
        runAfterDelay {
            showBottomSheet()
        }
        dialogOpened = false
    }

    //삭제대화창에서 삭제를 눌렀을때
    override fun onDelete() {
        val directoryList = mDirectoryListAdapter.getSelectedPaths()
        dialogOpened = false

        ensureBackgroundThread {
            val mediaPaths = hiddenMediumDao.getHiddenMediaByDirectory(directoryList)

            runOnUiThread {
                handleSAFDialog(mediaPaths) {
                    if(it) {
                        //Progress dialog만들고 띄워주기
                        progressType = PROGRESS_TYPE_DELETE
                        progressDialog = makeProgressDialog(mediaPaths.size, getString(R.string.deleting))
                        progressDialog!!.show()

                        ensureBackgroundThread {
                            try {
                                mediaPaths.forEach {
                                    if(deleteFileWithPath(it, false)) {
                                        //Table에서 하나씩 삭제한다.
                                        hiddenMediumDao.deleteItem(it)
                                    }

                                    if(progressDialog != null && !isDestroyed) {
                                        progressDialog!!.incrementProgressBy(1)
                                        if (progressDialog!!.progress == progressDialog!!.max) {
                                            progressDialog!!.dismiss()
                                            progressDialog = null
                                        }
                                    }
                                }

                                mDirectoryListAdapter.clearSelection()
                                runOnUiThread {
                                    onSelectionUpdate()
                                    Toast.makeText(applicationContext, R.string.directory_delete_complete, Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    else {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        dialogOpened = false
                    }
                }
            }
        }
    }
}
