package com.kr.gallery.pro.activities

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ProgressDialog
import android.content.*
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.provider.MediaStore.Video
import android.provider.Settings
import android.provider.Telephony
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayoutMediator
import com.kr.commons.extensions.*
import com.kr.commons.helpers.*
import com.kr.gallery.pro.BuildConfig
import com.kr.gallery.pro.R
import com.kr.gallery.pro.adapters.ScreenSlidePagerAdapter
import com.kr.gallery.pro.databases.GalleryDatabase
import com.kr.gallery.pro.dialogs.RenameFileDialog
import com.kr.gallery.pro.dialogs.DeleteFolderDialog
import com.kr.gallery.pro.dialogs.TransferMethodSelectionDialog
import com.kr.gallery.pro.extensions.*
import com.kr.gallery.pro.fragments.AllFragment
import com.kr.gallery.pro.helpers.*
import com.kr.gallery.pro.models.Directory
import com.kr.gallery.pro.models.Medium
import com.kr.gallery.pro.services.MediaBackgroundService
import com.kr.gallery.pro.services.MediaForegroundService
import com.kr.gallery.pro.views.KrBottomSheet
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet_behavior.*
import kotlinx.coroutines.*
import java.io.*

/**
 * App을 기동했을때의 첫 화면에 해당한 activity 이다
 * 삭제,전송,이름변경대화창들에서 단추가 눌리웠을때의 함수들을 정의한 interface 들
 * [DeleteFolderDialog.DeleteDialogListener], [TransferMethodSelectionDialog.TransferDialogListener], [RenameFileDialog.NameSetListener]
 * 자료기지 갱신하는 함수: [getMediaAndDirectories]
 */
class MainActivity : SimpleActivity(), DeleteFolderDialog.DeleteDialogListener, TransferMethodSelectionDialog.TransferDialogListener, RenameFileDialog.NameSetListener {

    //TabLayout 의  제목들('사진첩', '모두')
    private val tabTitles = intArrayOf(R.string.show_time, R.string.show_all)

    //MediaStore 에서의 변화를 감지하는 observer변수
    private var contentObserver: ContentObserver? = null

    /**
     * [getMediaAndDirectories]를 실행하는 한개 [Job]변수
     */
    var observerJob: Job? = null

    /**
     * 화일동작이 진행될때 현시되는 Progress Dialog, Dialog형태
     * See [PROGRESS_TYPE_HIDE], [PROGRESS_TYPE_SHOW], [PROGRESS_TYPE_DELETE], [PROGRESS_TYPE_MOVE]
     */
    var progressDialog: ProgressDialog? = null
    var progressType = 0 /* 숨기기 | 현시 | 삭제 | 이동 */

    /* Activity 파괴시 저장할 상태변수들 */
    private var dialogOpened = false
    private var dialogType = -1

    /**
     * 숨기기/숨김해제동작이 Foreground service를 통해 진행되던 상태에서
     * App을 다시 켰을때 현재까지의 진행상태를 보관한다
     * [MediaForegroundService]
     */
    private var mActionFinished = true
    private var mAction = ""            //동작
    private var mDataCount = 0         //전체화일개수
    private var mCurrentProgress = 0    //동작이 완료된 화일개수
    private var mRemainingData = ArrayList<Serializable>()  //남아있는 자료들
    private var mFailedMediaCount = 0        //동작이 실패한 화일개수

    lateinit var bottomSheetBehavior: BottomSheetBehavior<KrBottomSheet>

    //정적성원변수들
    companion object{
        //DialogFragment 들을 띄울때 리용하는 TAG들
        const val TRANSFER_DIALOG_TAG = "share_dialog"
        const val RENAME_DIALOG_TAG = "rename_dialog"
        const val RENAME_VIEW_ENABLED = "rename_view_enabled"

        /**
         * App이 처음 기동되였을때 [gotMedia]함수실행시 database갱신이 진행되였는가를 보관해둔다
         */
        var DATABASE_UPDATED = false

        /**
         * @param context
         * @return [MainActivity]를 돌려준다.
         */
        fun getMainActivity(context: Context): MainActivity {
            return if (context is MainActivity) {
                context
            } else (context as ContextWrapper).baseContext as MainActivity
        }
    }

    //Broadcast 수신
    private var mBroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action?: return
            when(action) {
                HIDE_START -> {
                    //Progress대화창 띄워주기
                    val count = intent.getIntExtra(DATA_COUNT, 0)
                    val progress = intent.getIntExtra(CURRENT_PROGRESS, 0)
                    progressType = PROGRESS_TYPE_HIDE
                    progressDialog = makeProgressDialog(count, getString(R.string.hiding))
                    progressDialog!!.progress = progress
                    progressDialog!!.show()
                }

                SHOW_START -> {
                    if(!mActionFinished) {
                        //Progress대화창 띄워주기
                        val count = intent.getIntExtra(DATA_COUNT, 0)
                        val progress = intent.getIntExtra(CURRENT_PROGRESS, 0)
                        progressType = PROGRESS_TYPE_SHOW
                        progressDialog = makeProgressDialog(count, getString(R.string.unhiding))
                        progressDialog!!.progress = progress
                        progressDialog!!.show()
                    }
                }

                ACTION_PROGRESS -> {
                    //Progress대화창 progress값갱신
                    if(progressDialog != null && !isDestroyed) {
                        val progress = intent.getIntExtra(ACTION_PROGRESS, 0)
                        progressDialog!!.progress = progress
                    }
                }

                ACTION_COMPLETE -> {
                    //Progress대화창 닫고 상태들 복귀
                    if(progressDialog != null && !isDestroyed) {
                        progressDialog!!.dismiss()
                        progressDialog = null

                        val failedMediaCount = intent.getIntExtra(FAILED_MEDIA_COUNT, 0)

                        val position = tabs.selectedTabPosition
                        if (position == 1) {
                            getAllFragment()!!.clearDirectorySelectedKeys()
                        }

                        hideBottomSheet()
                        updateSelectedCounts(0, false)

                        if(progressType == PROGRESS_TYPE_HIDE) {
                            if(failedMediaCount == 0) toast(R.string.directory_hide_complete)
                            else toast(getString(R.string.directory_hide_failed, failedMediaCount), Toast.LENGTH_LONG)
                        } else if(progressType == PROGRESS_TYPE_SHOW) {
                            if(failedMediaCount == 0) toast(R.string.directory_unhide_complete)
                            else toast(getString(R.string.directory_unhide_failed, failedMediaCount), Toast.LENGTH_LONG)
                        }
                    }
                }
            }
        }
    }

    //Foreground Service 가 실행되고있는가?
    private fun isForegroundServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        manager.getRunningServices(Int.MAX_VALUE).forEach {
            if(it.service.className == MediaForegroundService::class.java.name)
                return true
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        //Foreground service가 실행되고 있는지 먼저 검사한다.
        if(isForegroundServiceRunning()) {
            mActionFinished = false
            val serviceIntent = Intent(this, MediaForegroundService::class.java).setAction(REQUEST_STOP_SERVICE)
            val serviceConnection = object: ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    if(service != null) {
                        //동작진행상태들을 보관한다
                        val lastService = (service as MediaForegroundService.MyBinder).getService()
                        lastService.let {
                            mAction = it.mAction
                            mDataCount = it.mMediaCount
                            mCurrentProgress = it.mCurrentProgress
                            mRemainingData.clear()
                            mRemainingData.addAll(it.mRemainingData)
                            mFailedMediaCount = it.mFailedMediaCount
                        }

                        //동작들을 마저 진행한다.
                        continueLastAction()
                    }
                }
                override fun onServiceDisconnected(name: ComponentName?) {
                }
            }

            //Foreground service에 동작을 중지하라고 알려준다.
            bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE)
        }
        else {
            mActionFinished = true
        }

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        appLaunched(BuildConfig.APPLICATION_ID)

        if(hasPermission(PERMISSION_WRITE_STORAGE)) {
            tryLoadGallery();
        } else {
            //ViewPager 를 먼저 구축해준다.
            setupViewPager()

            //장치의 저정소에 대한 읽기쓰기권한이 부여되지 않았다면 사용자에게 권한을 요청한다.
            handlePermission(PERMISSION_WRITE_STORAGE) {
                //사용자가 권한요청을 거부하였다면 toast를 띄우고 app을 끈다.
                if (!it) {
                    toast(R.string.no_storage_permissions)
                    finish()

                    //사용자가 '다시 묻지 않음'을 check했을때
                    if(!shouldShowRequestPermissionRationale(getPermissionString(PERMISSION_WRITE_STORAGE))) {
                        //App정보화면을 펼친다.
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivityForResult(intent, 1)
                    }
                } else {
                    tryLoadGallery()
                }
            }
        }

        var bottomBarState = BottomSheetBehavior.STATE_HIDDEN
        var renameViewEnabled = false
        if (savedInstanceState != null) {
            if(mActionFinished) {
                //Progress dialog상태 불러들이기
                val hasProgress = savedInstanceState.getBoolean(HAS_PROGRESS, false)
                if (hasProgress) {
                    progressType = savedInstanceState.getInt(PROGRESS_TYPE)
                    val progressMax = savedInstanceState.getInt(PROGRESS_MAX)
                    val currentProgress = savedInstanceState.getInt(CURRENT_PROGRESS)

                    //Progress dialog 를 현시해준다.
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

            //기타 다른 상태들 복귀
            bottomBarState = savedInstanceState.getInt(BOTTOM_BAR_STATE)
            dialogOpened = savedInstanceState.getBoolean(DIALOG_OPENED, false)
            dialogType = savedInstanceState.getInt(DIALOG_TYPE, -1)
            renameViewEnabled = savedInstanceState.getBoolean(RENAME_VIEW_ENABLED, false)
        }

        updateWidgets()

        supportActionBar?.hide()
        buildSheet()

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = bottomBarState
        bottomSheetBehavior.disableDragging(bottomBarState)
        enableRenameView(renameViewEnabled)

        //Back단추 눌림동작
        back_on_select.setOnClickListener {
            onBackSelect()
        }

        //Dialog fragment에 listener를 추가해준다
        if(dialogOpened) {
            when(dialogType) {
                TRANSFER_DIALOG -> {
                    val dialog = supportFragmentManager.findFragmentByTag(TRANSFER_DIALOG_TAG) as TransferMethodSelectionDialog
                    dialog.addListener(this)
                }
                DELETE_DIALOG -> {
                    val dialog = supportFragmentManager.findFragmentByTag(DELETE_DIALOG_TAG) as DeleteFolderDialog
                    dialog.addListener(this)
                }
                RENAME_DIALOG -> {
                    val dialog = supportFragmentManager.findFragmentByTag(RENAME_DIALOG_TAG) as RenameFileDialog
                    dialog.addListener(this)
                }
            }
        }

        //BroadcastReceiver를 등록한다.
        val intentFilterLocal = IntentFilter()
        intentFilterLocal.addAction(HIDE_START)
        intentFilterLocal.addAction(SHOW_START)
        intentFilterLocal.addAction(ACTION_PROGRESS)
        intentFilterLocal.addAction(ACTION_COMPLETE)
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilterLocal)

        ensureBackgroundThread {
            //화상편집화면을 진행하는동안에 보관되였던 history화일들을 지운다.
            val externalDir = getExternalFilesDir(null)
            if(externalDir != null) {
                val directory = File(externalDir.path)
                val files = directory.listFiles()

                //화일들을 순환하면서 하나씩 지운다.
                if(files != null) {
                    for (file in files) {
                        file.delete()
                    }
                }
            }
        }
    }

    /**
     * [MediaForegroundService]
     * Foreground service가 실행중이던 상태에서 App이 켜졌을때
     * Foreground service를 중지시키고 미진된 동작들을
     * 여기서 계속한다
     */
    fun continueLastAction() {
        when(mAction) {
            HIDE_MEDIA_ACTION -> {
                val serviceIntent = Intent().apply {
                    setClass(this@MainActivity, MediaBackgroundService::class.java)
                    action = HIDE_MEDIA_ACTION
                    putExtra(CURRENT_PROGRESS, mCurrentProgress)
                    putExtra(REMAINING_DATA, mRemainingData)
                    putExtra(DATA_COUNT, mDataCount)
                    putExtra(FAILED_MEDIA_COUNT, mFailedMediaCount)
                }
                startService(serviceIntent)
            }
            SHOW_MEDIA_ACTION -> {
                val serviceIntent = Intent().apply {
                    setClass(this@MainActivity, MediaBackgroundService::class.java)
                    action = SHOW_MEDIA_ACTION
                    putExtra(CURRENT_PROGRESS, mCurrentProgress)
                    putExtra(REMAINING_DATA, mRemainingData)
                    putExtra(DATA_COUNT, mDataCount)
                    putExtra(FAILED_MEDIA_COUNT, mFailedMediaCount)
                }
                startService(serviceIntent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
    }

    override fun onDestroy() {
        if(progressDialog != null){
            progressDialog!!.dismiss()
            progressDialog = null
        }

        super.onDestroy()

        contentObserver?.let { contentResolver.unregisterContentObserver(it) }
        if (!isChangingConfigurations) {
            GalleryDatabase.destroyInstance()
        }

        //BroadcastReceiver를 등록해제한다
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver)
    }

    //Bottom sheet의 상태에 따라서 back단추 동작을 재정의해주었다.
    override fun onBackPressed() {
        when(bottomSheetBehavior.state) {
            //Bottom sheet의 상태가 expand된 상태라면 bottom sheet를 collapse상태로 만든다.
            BottomSheetBehavior.STATE_EXPANDED -> {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                bottomSheet.toggleExpand()
            }

            //Bottom sheet의 상태가 collapse된 상태라면 선택을 해제하고 bottom sheet를 숨긴다.
            BottomSheetBehavior.STATE_COLLAPSED -> {
                val position = tabs.selectedTabPosition

                if (position == 1) {
                    getAllFragment()?.deselectAll()
                }
                else {
                    // super.onBackPressed()를 호출하면 android framekwork leak이 발생한다
                    // 해결책은 super대신 finish() 호출하는것이다
                    finish()
                }
            }

            else -> {
                // super.onBackPressed()를 호출하면 android framekwork leak이 발생한다
                // 해결책은 super대신 finish() 호출하는것이다
                finish()
            }
        }
    }

    //Back단추를 눌렀을때 호출된다
    private fun onBackSelect() {
        //Get which tab is selected
        val position = tabs.selectedTabPosition
        if(position == 1) {
            getAllFragment()?.deselectAll()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //Progress dialog상태저장
        outState.putBoolean(HAS_PROGRESS, progressDialog != null)
        if(progressDialog != null) {
            outState.putInt(PROGRESS_MAX, progressDialog!!.max)
            outState.putInt(CURRENT_PROGRESS, progressDialog!!.progress)
            outState.putInt(PROGRESS_TYPE, progressType)
        }

        //기타 다른 상태들 저장
        outState.putInt(BOTTOM_BAR_STATE, bottomSheetBehavior.state)
        outState.putBoolean(DIALOG_OPENED, dialogOpened)
        outState.putInt(DIALOG_TYPE, dialogType)

        val view = bottomSheet.itemMap[KEY_RENAME]!!
        outState.putBoolean(RENAME_VIEW_ENABLED, view.isEnabled)
    }

    /**
     * [AllFragment]
     * [view_pager]에 속해있는 AllFragment(모두보기화면)를 돌려준다
     */
    private fun getAllFragment(): AllFragment? {
        for (it in supportFragmentManager.fragments) {
            if (it is AllFragment) {
                return it
            }
        }
        return null
    }

    //Viewpager2에서 페지절환에서 scroll속도가 빠르므로 속도를 늦춰준다
    private fun ViewPager2.reduceDragSensitivity() {
        //Viewpager2의 recyclerview를 먼저 얻는다.
        val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
        recyclerViewField.isAccessible = true
        val recyclerView = recyclerViewField.get(this) as RecyclerView

        //Recyclerview의 touch slop값을 5배로 증가시킨다.
        val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
        touchSlopField.isAccessible = true
        val touchSlop = touchSlopField.get(recyclerView) as Int
        touchSlopField.set(recyclerView, touchSlop * 5)
    }

    private fun checkOTGPath() {
        ensureBackgroundThread {
            if (!config.wasOTGHandled && hasPermission(PERMISSION_WRITE_STORAGE) && hasOTGConnected() && config.OTGPath.isEmpty()) {
                getStorageDirectories().firstOrNull { it.trimEnd('/') != internalStoragePath && it.trimEnd('/') != sdCardPath }?.apply {
                    config.wasOTGHandled = true
                    val otgPath = trimEnd('/')
                    config.OTGPath = otgPath
                }
            }
        }
    }

    /**
     * Media store에서 화상,동영상을 모두 얻고 자료기지를 갱신한다.
     * UI를 설정한다.
     */
    private fun tryLoadGallery() {
        if (hasPermission(PERMISSION_WRITE_STORAGE)) {
            if(contentObserver == null) {
                contentObserver = object : ContentObserver(Handler()){
                    override fun onChange(selfChange: Boolean) {
                        getMediaAndDirectories()
                    }
                }
                contentResolver.registerContentObserver(Images.Media.EXTERNAL_CONTENT_URI, true, contentObserver!!)
                contentResolver.registerContentObserver(Video.Media.EXTERNAL_CONTENT_URI, true, contentObserver!!)
            }

            checkOTGPath()
            getMediaAndDirectories() // Load Data from DB
            setupViewPager() // Set up Time, All fragments in View pager.
        }
    }

    //viewpager설정
    private fun setupViewPager() {
        val tabsPagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager, lifecycle)

        //Tab를 움직일때 굳는 현상이 있으므로 offscreenPageLimit을 설정한다
        view_pager.offscreenPageLimit = 1

        view_pager.adapter = tabsPagerAdapter
        view_pager.reduceDragSensitivity()
        view_pager.isUserInputEnabled = false

        //Tab들의 제목설정
        TabLayoutMediator(tabs, view_pager) { tab, position ->
            tab.text = resources.getString(tabTitles[position])
        }.attach()
    }

    /**
     * media store에서 모든 media를 얻고 자료기지에 보관한다.
     */
    fun getMediaAndDirectories() {
        //이미 실행되고 있는 thread는 중지시킨다
        observerJob?.cancel()

        observerJob = CoroutineScope(Dispatchers.IO).launch {
            val media = try {
                yield()
                mediaDB.getAllMedia() as ArrayList<Medium>
            } catch (e: java.lang.Exception) {
                ArrayList<Medium>()
            }
            yield()
            gotMedia(media)
        }
    }

    override fun showBottomSheet() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun hideBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    /**
     * 선택이 변하는데 따라 선택개수, 모두선택에 해당한 label들을 갱신한다.
     */
    @SuppressLint("SetTextI18n")
    override fun updateSelectedCounts(count: Int, allSelected: Boolean) {
        if(count > 0) {
            val countString = getString(R.string.select_with_count, count)
            display_selected_items.text = countString

            if(allSelected)
                select_all_items.text = getString(R.string.deselect_all)
            else
                select_all_items.text = getString(R.string.select_all)

            selected_view.visibility = View.VISIBLE
            tabs.visibility = View.INVISIBLE
        }
        else {
            selected_view.visibility = View.GONE
            tabs.visibility = View.VISIBLE
        }
    }

    //모두 선택/취소를 눌렀을때
    fun onSelectAll(view: View) {
        val fragment = getAllFragment()!!
        fragment.selectAll()
    }

    /**
     * Bottom sheet의 이름변경단추를 능동/비능동시켜준다
     * @param enable: true: 능동, false: 비능동
     */
    fun enableRenameView(enable: Boolean){
        //Text와 Image의 색을 변경시킨다
        val view = bottomSheet.itemMap[KEY_RENAME]!!
        val textView = view.findViewById<TextView>(R.id.collapsed_item_text)
        val imgView = view.findViewById<ImageView>(R.id.collapsed_item_img)
        val color = if(enable) getColor(R.color.bottomSheetColor) else getColor(R.color.disabledIconColor)

        if(enable == view.isEnabled)
            return

        view.isEnabled = enable
        textView.setTextColor(color)
        imgView.setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY)
    }

    /**
     * [Directory], [Medium]
     * [MediaStore]에서 모든 화상,동영상을 얻은 다음 자료기지의 table들 갱신
     */
    @SuppressLint("Recycle")
    private suspend fun gotMedia(oldMedia: ArrayList<Medium>) {
        val allMedia = ArrayList<Medium>()

        allMedia.addAll(getImagesFromMediaStore())
        allMedia.addAll(getVideosFromMediaStore())

        //얻어진 media을 분석하여 table에 추가/갱신, 삭제할 media들을 추출한다
        val mediaToRemove = oldMedia.clone() as ArrayList<Medium>
        val mediaToAdd = allMedia.clone() as ArrayList<Medium>
        mediaToRemove.removeAll(allMedia)
        mediaToAdd.removeAll(oldMedia)

        yield()
        if(config.isFirstMediaAction()) {
            DATABASE_UPDATED = mediaToRemove.isNotEmpty() || mediaToAdd.isNotEmpty()
            config.clearFirstMediaAction()
        }
        mediaDB.deleteAndInsert(mediaToRemove, mediaToAdd)

        //Directory 얻어내기
        yield()
        val newDirs = mediaDB.getDirectories() as ArrayList<Directory>

        //자료기지에 이미 있는 directory들과 비교
        val oldDirs = directoryDao.getAll() as ArrayList<Directory>
        val dirsToRemove = oldDirs.clone() as ArrayList<Directory>
        val dirsToAdd = newDirs.clone() as ArrayList<Directory>
        dirsToRemove.removeAll(newDirs)
        dirsToAdd.removeAll(oldDirs)
        yield()
        directoryDao.deleteAndInsert(dirsToRemove, dirsToAdd)
    }

    /**
     * 설정화면 activity 열기
     */
    fun launchSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    /**
     * 카메라 activity 열기
     */
    fun launchCamera() {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            toast(R.string.no_app_found)
        }
    }

    //Bottom sheet에 단추들 추가
    private fun buildSheet() {
        bottomSheet.addItem(
                applicationContext,
                KEY_SHARE,
                R.drawable.ic_outline_share_24,
                R.string.share,
                false
        )
        bottomSheet.addItem(
                applicationContext,
                KEY_DELETE,
                R.drawable.ic_outline_delete_24,
                R.string.delete,
                false
        )
        bottomSheet.addItem(
                applicationContext,
                KEY_HIDE,
                R.drawable.ic_outline_hide_24,
                R.string.hide,
                false
        )
        bottomSheet.addItem(
                applicationContext,
                KEY_RENAME,
                R.drawable.ic_outline_rename_24,
                R.string.rename,
                false
        )

        bottomSheet.itemMap[KEY_SHARE]!!.setOnClickListener{
            onClickShare()
        }

        bottomSheet.itemMap[KEY_DELETE]!!.setOnClickListener{
            onClickDelete()
        }

        bottomSheet.itemMap[KEY_HIDE]!!.setOnClickListener{
            onClickHide()
        }

        bottomSheet.itemMap[KEY_RENAME]!!.setOnClickListener{
            onClickRename()
        }
    }

    /**
     * [KEY_SHARE]
     * Bottom sheet에서 전송을 눌렀을때
     */
    private fun onClickShare() {
        if(dialogOpened)
            return

        //Hide the bottom sheet
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        //Open transfer method selection dialog
        val dialog = TransferMethodSelectionDialog(this)
        dialog.show(supportFragmentManager, TRANSFER_DIALOG_TAG)

        //상태보관
        dialogOpened = true
        dialogType = TRANSFER_DIALOG
    }

    /**
     * [KEY_DELETE]
     * Bottom sheet에서 삭제를 눌렀을때
     */
    private fun onClickDelete() {
        val position = tabs.selectedTabPosition
        if(position == 0)
            return

        if(dialogOpened)
            return

        dialogOpened = true
        val fragment = getAllFragment()!!

        //Bottom sheet를 숨겨준다.
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        //삭제대화창을 연다.
        val isSingular = fragment.getSelectedItemCount() == 1
        val dialog = DeleteFolderDialog(false, isSingular, this)
        dialog.show(supportFragmentManager, DELETE_DIALOG_TAG)

        //상태보관
        dialogType = DELETE_DIALOG
    }

    /**
     * [KEY_HIDE]
     * Bottom sheet에서 숨기기를 눌렀을때
     */
    private fun onClickHide() {
        val position = tabs.selectedTabPosition
        if(position == 0)
            return

        val fragment = getAllFragment()!!
        val dirPaths = fragment.getSelectedDirectoriesPath()

        handleSAFDialog(dirPaths) {
            //자료기지접근이 필요하므로 thread상에서 실행시킨다
            ensureBackgroundThread {
                val mediaToHide = mediaDB.getMediaByDirectoryList(dirPaths) as ArrayList<Medium>
                val serviceIntent = Intent().apply {
                    setClass(this@MainActivity, MediaBackgroundService::class.java)
                    action = HIDE_MEDIA_ACTION
                    putExtra(REMAINING_DATA, mediaToHide)
                    putExtra(CURRENT_PROGRESS, 0)
                    putExtra(DATA_COUNT, mediaToHide.size)
                }
                startService(serviceIntent)
            }
        }
    }

    /**
     * [KEY_RENAME]
     * Bottom sheet에서 이름변경을 눌렀을때
     */
    private fun onClickRename() {
        val position = tabs.selectedTabPosition
        if(position == 0)
            return

        if(dialogOpened)
            return

        dialogOpened = true
        val fragment = getAllFragment()!!

        //Bottom sheet를 숨긴다.
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        //이름변경대화창을 띄운다
        val name = fragment.getSelectedDirectoryName()
        val dialog = RenameFileDialog(name, this)
        dialog.show(supportFragmentManager, RENAME_DIALOG_TAG)

        //상태저장
        dialogType = RENAME_DIALOG
    }

    /**
     * [TransferMethodSelectionDialog]
     * 전송대화창에서 통보문을 눌렀을때
     */
    override fun onMessage() {
        runAfterDelay {
            //숨겨진 bottom sheet를 다시 보여준다.
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            ensureBackgroundThread {
                val uriList = ArrayList<Uri>()

                //Get files
                val position = tabs.selectedTabPosition
                if(position == 1) {
                    val fragment = getAllFragment() ?: return@ensureBackgroundThread
                    val pathList = fragment.getSelectedMediaPath()
                    pathList.forEach {
                        getFinalUriFromPath(it, BuildConfig.APPLICATION_ID)?.let { it1 -> uriList.add(it1) }
                    }
                }

                //Start activity
                val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
                intent.apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setPackage(Telephony.Sms.getDefaultSmsPackage(applicationContext))
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
                }
                startActivity(intent)
            }
        }
        dialogOpened = false
    }

    /**
     * [TransferMethodSelectionDialog]
     * 전송대화창에서 기록장을 눌렀을때
     */
    override fun onNote() {
        runAfterDelay {
            //숨겨진 bottom sheet를 다시 보여준다.
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            //아직 구현안됨
        }
        dialogOpened = false
    }

    /**
     * [TransferMethodSelectionDialog]
     * 전송대화창에서 블루투스를 눌렀을때
     */
    override fun onBluetooth() {
        runAfterDelay {
            //숨겨진 bottom sheet를 다시 보여준다.
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            ensureBackgroundThread {
                val uriList = ArrayList<Uri>()

                //선택된 화일경로들을 얻고 화일경로 -> Uri로 변환하여 전송할 자료들을 추가한다.
                val position = tabs.selectedTabPosition
                if(position == 1) {
                    val fragment = getAllFragment() ?: return@ensureBackgroundThread
                    val pathList = fragment.getSelectedMediaPath()
                    pathList.forEach {
                        getFinalUriFromPath(it, BuildConfig.APPLICATION_ID)?.let { it1 -> uriList.add(it1) }
                    }
                }

                //Bluetooth activity를 연다.
                val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
                intent.apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.setPackage("com.android.bluetooth")
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
                }
                startActivity(intent)
            }
        }
        dialogOpened = false
    }

    /**
     * [DeleteFolderDialog.DeleteDialogListener.onCancel]
     * [TransferMethodSelectionDialog.TransferDialogListener.onCancel]
     * [RenameFileDialog.NameSetListener.onCancel]
     * 삭제, 전송, 이름변경대화창들에서 cancel을 눌렀을때, 혹은 바깥령역을 touch 했을때 대화창이 닫기면서 호출된다
     */
    override fun onCancel() {
        runAfterDelay{
            //숨겨진 bottom sheet를 다시 보여준다.
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        dialogOpened = false
    }

    /**
     * [DeleteFolderDialog]
     * 삭제 대화창에서 삭제단추를 눌렀을때 호출된다
     */
    override fun onDelete() {
        val position = tabs.selectedTabPosition
        if(position == 0)
            return

        val fragment = getAllFragment()!!
        runAfterDelay {
            ensureBackgroundThread {
                val pathList = fragment.getSelectedMediaPath()

                runOnUiThread {
                    handleSAFDialog(pathList) {
                        //Progress dialog만들고 띄워주기
                        lateinit var progressDialog: ProgressDialog
                        runOnUiThread {
                            progressDialog = makeProgressDialog(pathList.size, getString(R.string.deleting))
                            progressDialog.show()

                            ensureBackgroundThread {
                                try {
                                    var failedCount = 0
                                    pathList.forEach {
                                        if(!deleteFileWithPath(it, true)) failedCount ++
                                        progressDialog.incrementProgressBy(1)
                                        if(progressDialog.progress == progressDialog.max) {
                                            progressDialog.dismiss()
                                        }
                                    }

                                    runOnUiThread {
                                        fragment.clearDirectorySelectedKeys()

                                        //Bottom bar숨기기
                                        hideBottomSheet()
                                        updateSelectedCounts(0, false)
                                        if(failedCount == 0) toast(R.string.directory_delete_complete)
                                        else toast(getString(R.string.file_delete_failed, failedCount), Toast.LENGTH_LONG)
                                    }
                                } catch (e: java.lang.Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        dialogOpened = false
                    }
                }
            }
        }
    }

    /**
     * [RenameFileDialog]
     * 이름변경대화창에서 확인 단추를 눌렀을때 호출된다.
     * @param name: 새 등록부이름
     * 여기서 등록부이름을 파라메터로 들어온 새 이름으로 rename해준다
     */
    override fun onNameSet(name: String) {
        runAfterDelay {
            //bottom bar 를 다시 보여준다
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            ensureBackgroundThread {
                val position = tabs.selectedTabPosition
                if(position == 1) {
                    val fragment = getAllFragment()!!
                    fragment.renameSelectedDirectory(name)
                }
            }
        }
        dialogOpened = false
    }
}
