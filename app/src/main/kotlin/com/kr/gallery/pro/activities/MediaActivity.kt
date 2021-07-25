package com.kr.gallery.pro.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.app.ProgressDialog
import android.app.SharedElementCallback
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.Transition
import android.transition.TransitionManager
import android.util.ArrayMap
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.kr.commons.extensions.deleteFileWithPath
import com.kr.commons.extensions.showErrorToast
import com.kr.commons.extensions.toast
import com.kr.commons.helpers.*
import com.kr.commons.views.MyRecyclerView
import com.kr.gallery.pro.R
import com.kr.gallery.pro.adapters.MediaAdapter
import com.kr.gallery.pro.custom.MediaViewModelFactory
import com.kr.gallery.pro.custom.NotifyingGridLayoutManager
import com.kr.gallery.pro.custom.OnLayoutCompleteCallback
import com.kr.gallery.pro.dialogs.DeleteFolderDialog
import com.kr.gallery.pro.dialogs.TransferMethodSelectionDialog
import com.kr.gallery.pro.extensions.*
import com.kr.gallery.pro.helpers.*
import com.kr.gallery.pro.models.Medium
import com.kr.gallery.pro.viewmodels.MediaViewModel
import com.kr.gallery.pro.views.KrBottomSheet
import com.kr.gallery.pro.views.MySquareImageView
import kotlinx.android.synthetic.main.activity_media.*
import kotlinx.android.synthetic.main.bottom_sheet_behavior.*
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.util.*
import kotlin.collections.ArrayList

/**
 * 등록부보기화면
 * 기본화면 - 사진첩보기, 모두보기화면들에서 날자, 혹은 등록부를 click했을때 등록부보기화면을 연다.
 * @see MainActivity
 */
class MediaActivity : SimpleActivity(), TransferMethodSelectionDialog.TransferDialogListener, DeleteFolderDialog.DeleteDialogListener, OnLayoutCompleteCallback {

    //등록부경로, 날자, Media형태(화상/동영상)
    private var mPath:String = ""
    private var mTimeStamp:String = ""
    private var mMediaType = 0

    //화상선택 intent인가?
    private var mIsGetImageIntent = false
    //배경화면설정 intent인가?
    private var mIsSetWallpaperIntent = false

    //숨겨진 media를 보여주는가?
    private var mShowHidden = false

    //입력으로 등록부가 아니라 날자가 들어왔을때 true이다.
    private var mShowAll = false

    //확대/축소를 감지하는 listener
    private var mZoomListener: MyRecyclerView.MyZoomListener? = null

    //Bottom sheet를 관리하는 변수
    lateinit var bottomSheetBehavior: BottomSheetBehavior<KrBottomSheet>

    //Media view model
    private lateinit var mMediaViewModel: MediaViewModel

    //Media자료
    private var mMediaToShow = ArrayList<Medium>()

    //Directory adapter
    private lateinit var mMediaAdapter: MediaAdapter

    //Animation을 위한 Transition key
    private var mStartTransitionKey : String = ""

    /* Activity파괴시 저장할 상태변수들 */
    private var dialogOpened = false
    private var dialogType = -1

    private var reEnterBundle: Bundle? = null
    private var tempItemAnimator: RecyclerView.ItemAnimator? = null

    //상수정의
    companion object {
        const val WALLPAPER_REQUEST_CODE = 101
        const val VIEWPAGER_REQUEST_CODE = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)

        setExitSharedElementCallback(mSharedElementCallback)

        var bottomBarState = BottomSheetBehavior.STATE_HIDDEN
        val selectedKeys: LinkedHashSet<Int>
        var allSelected = false
        if(savedInstanceState != null) {
            //상태들 불러들이기
            selectedKeys = savedInstanceState.getSerializable(SELECTED_KEYS)!! as LinkedHashSet<Int>
            allSelected = savedInstanceState.getBoolean(ALL_SELECTED, false)
            bottomBarState = savedInstanceState.getInt(BOTTOM_BAR_STATE)
            dialogOpened = savedInstanceState.getBoolean(DIALOG_OPENED, false)
            dialogType = savedInstanceState.getInt(DIALOG_TYPE)
        } else {
            selectedKeys = LinkedHashSet()
        }

        try {
            //intent로부터 정보들 얻기
            intent.apply {
                mIsGetImageIntent = getBooleanExtra(GET_IMAGE_INTENT, false)
                mIsSetWallpaperIntent = getBooleanExtra(SET_WALLPAPER_INTENT, false)

                mPath = getStringExtra(DIRECTORY) ?: ""
                mTimeStamp = getStringExtra(DATE) ?: ""
                mMediaType = getIntExtra(MEDIA_TYPE, 0)
                mShowHidden = getBooleanExtra(SHOW_HIDDEN, false)

                if(mTimeStamp != "")
                    mShowAll = true
            }

            //제목 label에 등록부이름 혹은 '날자 화상'|'날자 동영상'의 text를 설정한다.
            media_dir_name.text =
                    if (mShowAll) {
                        if(mMediaType == MEDIA_IMAGE)
                            mTimeStamp + " " + getString(R.string.images)
                        else
                            mTimeStamp + " " + getString(R.string.videos)
                    }
                    else getHumanizedFilename(mPath)
        } catch (e: Exception) {
            showErrorToast(e)
            finish()
            return
        }
        supportActionBar?.hide()

        if (mShowAll) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }

        updateWidgets()

        buildSheet()
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = bottomBarState
        bottomSheet.expand(bottomBarState == BottomSheetBehavior.STATE_EXPANDED)
        bottomSheetBehavior.disableDragging(bottomBarState)

        media_select_all_items.setOnClickListener{
            if(mMediaAdapter.allSelected())
                mMediaAdapter.deselectAll()
            else
                mMediaAdapter.selectAll()
        }

        media_back_from_select.setOnClickListener {
            getMediaAdapter()!!.deselectAll()
        }

        //Recyclerview, Adapter설정
        initZoomListener()
        setupGridLayoutManager()
        mMediaAdapter = MediaAdapter(this, media_grid, media_vertical_fastscroller)
        mMediaAdapter.selectedKeys = selectedKeys
        mMediaAdapter.setSelectable(selectedKeys.size > 0)
        mMediaAdapter.setupZoomListener(mZoomListener)
        media_grid.adapter = mMediaAdapter

        media_vertical_fastscroller.isHorizontal = false
        media_vertical_fastscroller.setViews(media_grid, null) {
            val bubbleText = mMediaAdapter.getItemBubbleText(it, DATE_FORMAT_ONE, TIME_FORMAT_12)
            media_vertical_fastscroller.updateBubbleText(bubbleText)
        }

        updateSelectedCounts(selectedKeys.size, allSelected)

        val viewModel: MediaViewModel by viewModels { MediaViewModelFactory(application, mPath, mShowHidden, mTimeStamp, mMediaType) }
        mMediaViewModel = viewModel
        viewModel.mediaFiltered.observe(this, androidx.lifecycle.Observer {
            mMediaToShow = it as ArrayList<Medium>
            setupAdapterData()
        })

        //Dialog fragment에 listener를 추가해준다
        if(dialogOpened) {
            when(dialogType) {
                TRANSFER_DIALOG -> {
                    val dialog = supportFragmentManager.findFragmentByTag(MainActivity.TRANSFER_DIALOG_TAG) as TransferMethodSelectionDialog
                    dialog.addListener(this)
                }
                DELETE_DIALOG -> {
                    val dialog = supportFragmentManager.findFragmentByTag(DELETE_DIALOG_TAG) as DeleteFolderDialog
                    dialog.addListener(this)
                }
            }
        }
    }

    /**
     * 선택이 변하는데 따라 선택개수, 모두선택에 해당한 label들을 갱신한다.
     */
    @SuppressLint("SetTextI18n")
    override fun updateSelectedCounts(count: Int, allSelected: Boolean) {
        if(count > 0) {
            media_display_selected_items.text = getString(R.string.select_with_count, count)

            if(allSelected)
                media_select_all_items.text = getString(R.string.deselect_all)
            else
                media_select_all_items.text = getString(R.string.select_all)

            media_selected_counts_label.visibility = View.VISIBLE
            media_dir_name.visibility = View.GONE
        } else {
            media_selected_counts_label.visibility = View.GONE
            media_dir_name.visibility = View.VISIBLE
        }
    }

    private fun getMediaAdapter() = media_grid.adapter as? MediaAdapter

    //Adapter에 자료 설정
    private fun setupAdapterData() {
        mMediaAdapter.setClickListener { any: Any, transitionView: View?->
            if (any is Medium && !isFinishing) {
                itemClicked(any.path, transitionView)
            }
        }
        mMediaAdapter.submitList(mMediaToShow)
    }

    /**
     * Bottom sheet의 상태에 따라서 back단추 동작을 재정의해주었다.
     * @see MainActivity.onBackPressed
     */
    override fun onBackPressed() {
        when(bottomSheetBehavior.state) {
            //Bottom sheet의 상태가 expand된 상태라면 bottom sheet를 collapse상태로 만든다.
            BottomSheetBehavior.STATE_EXPANDED -> {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                bottomSheet.toggleExpand()
            }

            //Bottom sheet의 상태가 collapse된 상태라면 선택을 해제하고 bottom sheet를 숨긴다.
            BottomSheetBehavior.STATE_COLLAPSED -> {
                mMediaAdapter.deselectAll()
            }

            else -> {
                super.onBackPressed()
            }
        }
    }

    //Recyclerview에 layoutmanager설정
    private fun setupGridLayoutManager() {
        val layoutManager = media_grid.layoutManager as NotifyingGridLayoutManager
        layoutManager.setLayoutCallback(this)

        (media_grid.layoutParams as RelativeLayout.LayoutParams).apply {
            topMargin = 0
            bottomMargin = 0
        }

        layoutManager.orientation = RecyclerView.VERTICAL

        layoutManager.spanCount = config.mediaColumnCnt
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return 1
            }
        }
    }

    //Recyclerview에 확대/축소 listener추가
    private fun initZoomListener() {
        val layoutManager = media_grid.layoutManager as NotifyingGridLayoutManager
        mZoomListener = object : MyRecyclerView.MyZoomListener {
            /*
            확대
            column 수를 감소시킨다.
             */
            override fun zoomIn() {
                if (layoutManager.spanCount > 1) {
                    reduceColumnCount()
                    getMediaAdapter()?.finishActMode()
                }
            }

            /*
            축소
            column 수를 증가시킨다.
             */
            override fun zoomOut() {
                if (layoutManager.spanCount < MAX_COLUMN_COUNT) {
                    increaseColumnCount()
                    getMediaAdapter()?.finishActMode()
                }
            }
        }
    }

    //Recyclerview의 column수를 증가시킨다.
    private fun increaseColumnCount() {
        config.mediaColumnCnt = ++(media_grid.layoutManager as NotifyingGridLayoutManager).spanCount
        columnCountChanged()
    }

    //Recyclerview의 column수를 감소시킨다.
    private fun reduceColumnCount() {
        config.mediaColumnCnt = --(media_grid.layoutManager as NotifyingGridLayoutManager).spanCount
        columnCountChanged()
    }

    //Recyclerview의 column수가 변하였을때 호출된다.
    private fun columnCountChanged() {
        mMediaAdapter.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        //배경설정화면이 설정되였으면 activity를 끝낸다.
        if(requestCode == WALLPAPER_REQUEST_CODE) {
            if(resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        } else if (requestCode == VIEWPAGER_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (mTimeStamp != "") {
                    // shared transition이 일어나지 않는 경우이다
                    // 이때 새 자료기자를 얻어 media_grid를 갱신시켜야 한다
                    ensureBackgroundThread {
                        mMediaToShow = mediaDB.getMediaListByDate(mTimeStamp, mMediaType) as ArrayList<Medium>
                        setupAdapterData()
                    }
                }

                if (!mMediaViewModel.mediaFiltered.hasObservers()) addViewModelObserver()
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // viewpager에서 모든화상들이 삭제되여 돌아왔다.
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    /**
     * 화면에서 화상이나 동영상 하나를 선택했을때 호출된다.
     * @param path: 화일경로
     * @param transitionView: activity를 끝내면서 transition을 진행할 view
     */
    private fun itemClicked(path: String, transitionView: View?) {
        //배경화면설정 intent일때
        if(mIsSetWallpaperIntent) {
            //배경화면설정 activity를 연다.
            ensureBackgroundThread {
                var imageUri = getImageURI(path)
                if(imageUri == null)
                    imageUri = Uri.fromFile(File(path))
                val intent = Intent(this, SetWallpaperActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    data = imageUri
                }
                startActivityForResult(intent, WALLPAPER_REQUEST_CODE)
            }
        }

        //화상선택 intent일때
        else if (mIsGetImageIntent) {
            ensureBackgroundThread {
                var imageUri = getImageURI(path)
                if(imageUri == null)
                    imageUri = Uri.fromFile(File(path))

                Intent().apply {
                    data = imageUri
                    setResult(Activity.RESULT_OK, this)
                }
                finish()
            }
        } else {
            mStartTransitionKey = path.hashCode().toString()
            val activityOptions = ActivityOptions.makeSceneTransitionAnimation(this, transitionView, mStartTransitionKey)

            val mediumPathHashCodeList = ArrayList<String>()
            for( medium in mMediaToShow) {
                mediumPathHashCodeList.add(medium.path.hashCode().toString())
            }

            // observer 삭제하고 onActivityReenter/onActivityResult에서 새자료룰 얻어 shared transition 발생시킨다
            mMediaViewModel.mediaFiltered.removeObservers(this)

            //전체보여주기가 아닌경우(등록부에서)
            if(!mShowAll) {
                //화상보기화면을 연다.
                val directoryList = ArrayList<String>(listOf(mPath))
                Intent(this, ViewPagerActivity::class.java).apply {
                    putExtra(SKIP_AUTHENTICATION, false)
                    putExtra(DIRECTORY_LIST, directoryList)
                    putExtra(PATH, path)
                    putExtra(SHOW_HIDDEN, mShowHidden)
                    putExtra(TRANSITION_KEY, mStartTransitionKey)
                    putExtra(MEDIUM_PATH_HASH_LIST, mediumPathHashCodeList)
                    putExtra(TIMESTAMP, mTimeStamp)
                    startActivityForResult(this, VIEWPAGER_REQUEST_CODE, activityOptions.toBundle())
                }
            }

            //전체보여주기인 경우(날자별보기에서)
            else {
                //화상보기화면을 연다.
                Intent(this, ViewPagerActivity::class.java).apply {
                    putExtra(SKIP_AUTHENTICATION, false)
                    putExtra(PATH, path)
                    putExtra(TRANSITION_KEY, mStartTransitionKey)
                    putExtra(MEDIUM_PATH_HASH_LIST, mediumPathHashCodeList)
                    putExtra(TIMESTAMP, mTimeStamp)
                    startActivityForResult(this, VIEWPAGER_REQUEST_CODE, activityOptions.toBundle())
                }
            }
        }
    }
    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)

        tempItemAnimator = media_grid.itemAnimator;
        media_grid.itemAnimator = null;

        postponeEnterTransition()

        reEnterBundle = Bundle(data!!.extras)
        val currentPos = reEnterBundle!!.getInt(EXTRA_POSITION_ON_VIEWPAGER)
        val currentTransitionKey = reEnterBundle!!.getString(EXTRA_CURRENT_TRANSITION_KEY)

        var scrolled = false
        var sharedViewFound = false
        val observer = media_grid.viewTreeObserver
        observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // 이 시점에서 recyclerview의 layout은 완성되고 크기와 모든 자식 view들을 알수있는 시점이다
                if (!scrolled) {
                    media_grid.scrollToPosition(currentPos)
                    media_grid.requestLayout()
                    scrolled = true
                    return
                }

                if (!sharedViewFound) {
                    val newSharedElement = getThumbViewByPosition(currentPos)
                    if (newSharedElement != null) {
                        (newSharedElement as ImageView).transitionName = currentTransitionKey
                        sharedViewFound = true
                    }
                }
                if (sharedViewFound) {
                    startPostponedEnterTransition()
                    media_grid.viewTreeObserver
                            .removeOnGlobalLayoutListener(this)
                }
            }
        })

        ensureBackgroundThread {
            var newMediums : ArrayList<Medium>? = null

            if (!mShowAll) newMediums = mediaDB.getMediaByDirectoryList(arrayListOf(mPath)) as ArrayList<Medium>
            else newMediums = mediaDB.getMediaListByDate(mTimeStamp, mMediaType) as ArrayList<Medium>

            // 새 medium목록과 이전 목록 비교
            var same = true
            if (newMediums.size == mMediaToShow.size) {
                for (i in 0 until newMediums.size) {
                    val newMedium = newMediums[i]
                    val oldMedium = mMediaToShow[i]
                    if (newMedium.name != oldMedium.name || newMedium.modified != oldMedium.modified) {
                        same = false
                        break
                    }
                }
            } else same = false

            if (same) {
                scrolled = true
                runOnUiThread {
                    media_grid.scrollToPosition(currentPos)
                    media_grid.requestLayout()
                }
            } else {
                mMediaToShow = newMediums
                setupAdapterData()
            }
            if (!mMediaViewModel.mediaFiltered.hasObservers()) addViewModelObserver()
        }
    }

    private fun addViewModelObserver() {
        runOnUiThread {
            mMediaViewModel.mediaFiltered.observe(this, androidx.lifecycle.Observer {
                mMediaToShow = it as ArrayList<Medium>
                setupAdapterData()
            })
        }
    }

    override fun onEnterAnimationComplete() {
        media_grid.postDelayed({
            if (tempItemAnimator != null) media_grid.itemAnimator = tempItemAnimator

            removeActivityFromTransitionManager()
        }, 1000)
    }

    private val mSharedElementCallback: SharedElementCallback = object : SharedElementCallback() {
        override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
            if (reEnterBundle != null) {
                val currentTransitionKey = reEnterBundle!!.getString(EXTRA_CURRENT_TRANSITION_KEY)
                val currentPos = reEnterBundle!!.getInt(EXTRA_POSITION_ON_VIEWPAGER)
                val newSharedElement: ImageView? = getThumbViewByPosition(currentPos) as ImageView
                newSharedElement?.transitionName = currentTransitionKey
                if (newSharedElement != null) {
                    names.clear()
                    names.add(currentTransitionKey!!)
                    sharedElements.clear()
                    sharedElements[currentTransitionKey] = newSharedElement
                } else {
                    names.clear()
                    sharedElements.clear()
                }
                reEnterBundle = null
            }
        }
    }

    fun getThumbViewByPosition(pos: Int): View? {
        val viewHolder = media_grid.findViewHolderForAdapterPosition(pos)
        if (viewHolder?.itemView != null) {
            val itemView = (viewHolder.itemView as RelativeLayout).findViewById<MySquareImageView>(R.id.medium_thumbnail)
            return itemView
        }
        return null
    }

    private fun removeActivityFromTransitionManager() {
        val transitionManagerClass: Class<*> = TransitionManager::class.java
        try {
            val runningTransitionsField: Field =
                    transitionManagerClass.getDeclaredField("sRunningTransitions")
            runningTransitionsField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val runningTransitions: ThreadLocal<WeakReference<ArrayMap<ViewGroup, ArrayList<Transition>>>?> =
                    runningTransitionsField.get(transitionManagerClass) as ThreadLocal<WeakReference<ArrayMap<ViewGroup, ArrayList<Transition>>>?>
            if (runningTransitions.get() == null || runningTransitions.get()?.get() == null) {
                return
            }
            val map: ArrayMap<ViewGroup, ArrayList<Transition>> =
                    runningTransitions.get()?.get() as ArrayMap<ViewGroup, ArrayList<Transition>>
            map[window.decorView]?.let { transitionList ->
                transitionList.forEach { transition ->
                    //Add a listener to all transitions. The last one to finish will remove the decor view:
                    transition.addListener(object : Transition.TransitionListener {
                        override fun onTransitionEnd(transition: Transition) {
                            //When a transition is finished, it gets removed from the transition list
                            // internally right before this callback. Remove the decor view only when
                            // all the transitions related to it are done:
                            if (transitionList.isEmpty()) {
                                map.remove(window.decorView)
                            }
                            transition.removeListener(this)
                        }

                        override fun onTransitionCancel(transition: Transition?) {}
                        override fun onTransitionPause(transition: Transition?) {}
                        override fun onTransitionResume(transition: Transition?) {}
                        override fun onTransitionStart(transition: Transition?) {}
                    })
                }
                //If there are no active transitions, just remove the decor view immediately:
                if (transitionList.isEmpty()) {
                    map.remove(window.decorView)
                }
            }
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        super.onDestroy()

        removeActivityFromTransitionManager()
    }

    //Bottom sheet 만들기
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

        //이전 화면이 숨김등록부화면이 아니라면 expand상태를 추가하고 expand view에 이름변경, 이동 단추를 추가한다
        if(!mShowHidden) {
            bottomSheet.addItem(
                    applicationContext,
                    KEY_EXPAND,
                    R.drawable.ic_outline_more_horiz_24,
                    R.string.expand,
                    false
            )

            bottomSheet.addItem(applicationContext, KEY_ENCRYPT, 0, R.string.encrypt, true)
            bottomSheet.addItem(applicationContext, KEY_MOVE, 0, R.string.move, true)
        }

        //'기타/닫기'단추를 눌렀을때 bottom sheet를 expand|collapse시킨다.
        bottomSheet.itemMap[KEY_EXPAND]?.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            else
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            bottomSheet.toggleExpand()
        }

        bottomSheet.itemMap[KEY_SHARE]!!.setOnClickListener{
            onClickShare()
        }

        bottomSheet.itemMap[KEY_DELETE]!!.setOnClickListener{
            onClickDelete()
        }

        bottomSheet.itemMap[KEY_MOVE]?.setOnClickListener{
            mMediaAdapter.apply {
                moveFilesTo {
                    success, failedCount ->
                    if(!success) {
                        toast(R.string.copy_move_failed)
                    }
                    else {
                        if(failedCount == 0) toast(R.string.file_move_complete)
                        else toast(getString(R.string.file_move_failed, failedCount))
                    }

                    //선택을 해제하고 bottom bar를 숨겨준다
                    clearSelection()
                    updateTitle()
                }
            }
        }
    }

    private fun onClickShare() {
        if(dialogOpened)
            return

        //Bottom bar를 숨긴다
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheet.expand(false)

        //전송대화창을 연다
        val dialog = TransferMethodSelectionDialog(this)
        dialog.show(supportFragmentManager, MainActivity.TRANSFER_DIALOG_TAG)

        //상태보관
        dialogOpened = true
        dialogType = TRANSFER_DIALOG
    }

    /**
     * [TransferMethodSelectionDialog]
     * 전송대화창에서 통보문을 눌렀을때
     */
    override fun onMessage() {
        runAfterDelay {
            getMediaAdapter()!!.shareMedia(SHARE_BY_MESSAGE)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        dialogOpened = false
    }

    /**
     * [TransferMethodSelectionDialog]
     * 전송대화창에서 기록장을 눌렀을때
     */
    override fun onNote() {
        runAfterDelay {
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
            getMediaAdapter()!!.shareMedia(SHARE_BY_BLUETOOTH)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        dialogOpened = false
    }

    /**
     * [DeleteFolderDialog.DeleteDialogListener.onCancel]
     * [TransferMethodSelectionDialog.TransferDialogListener.onCancel]
     * 삭제, 전송대화창들에서 cancel을 눌렀을때, 혹은 바깥령역을 touch 했을때 대화창이 닫기면서 호출된다
     */
    override fun onCancel() {
        runAfterDelay{
            //bottom bar를 보여준다(collapsed로 복귀)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        dialogOpened = false
    }

    private fun onClickDelete() {
        if(dialogOpened)
            return

        dialogOpened = true
        //Bottom bar를 숨긴다
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheet.expand(false)

        //삭제대화창을 연다
        val dialog = DeleteFolderDialog(true, mMediaAdapter.getSelectedCounts() == 1, this)
        dialog.show(supportFragmentManager, DELETE_DIALOG_TAG)

        //상태보관
        dialogType = DELETE_DIALOG
    }

    /**
     * [DeleteFolderDialog]
     * 삭제 대화창에서 삭제단추를 눌렀을때 호출된다
     */
    override fun onDelete() {
        runAfterDelay {
            ensureBackgroundThread {
                val pathList = mMediaAdapter.getSelectedPaths()
                if(pathList.isEmpty())
                    return@ensureBackgroundThread

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
                                    if (mShowHidden) {
                                        if (deleteFileWithPath(it, false)) {
                                            //Table에서 하나씩 삭제한다.
                                            hiddenMediumDao.deleteItem(it)
                                        } else {
                                            failedCount ++
                                        }
                                    } else {
                                        if(!deleteFileWithPath(it, true)) failedCount ++
                                    }
                                    progressDialog.incrementProgressBy(1)
                                    if (progressDialog.progress == progressDialog.max) {
                                        progressDialog.dismiss()
                                    }
                                }

                                runOnUiThread {
                                    mMediaAdapter.clearSelection()
                                    mMediaAdapter.updateTitle()

                                    if(failedCount == 0) toast(R.string.file_delete_complete)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //상태저장
        outState.putSerializable(SELECTED_KEYS, mMediaAdapter.selectedKeys)
        outState.putBoolean(ALL_SELECTED, mMediaAdapter.allSelected())
        outState.putInt(BOTTOM_BAR_STATE, bottomSheetBehavior.state)
        outState.putBoolean(DIALOG_OPENED, dialogOpened)
        outState.putInt(DIALOG_TYPE, dialogType)
    }

    override fun showBottomSheet() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            bottomSheet.itemMap[KEY_EXPAND]?.let { bottomSheet.setInitCollapsed(it) }
        }
    }

    override fun hideBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    //RecyclerView의 layout이 갱신될때 scroller에서 계산을 다시 진행한다.
    override fun onLayoutComplete() {
        media_vertical_fastscroller?.measureRecyclerView()
    }
}
