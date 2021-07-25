package com.kr.gallery.pro.fragments

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.kr.commons.activities.BaseSimpleActivity
import com.kr.commons.extensions.hasPermission
import com.kr.commons.extensions.renameFile
import com.kr.commons.extensions.toast
import com.kr.commons.helpers.CAMERA_PATH
import com.kr.commons.helpers.CAM_SETTINGS_ANIMATE_DISTANCE
import com.kr.commons.helpers.MOVIE_PATH
import com.kr.commons.helpers.PERMISSION_WRITE_STORAGE
import com.kr.gallery.pro.R
import com.kr.gallery.pro.activities.MainActivity
import com.kr.gallery.pro.activities.MediaActivity
import com.kr.gallery.pro.adapters.*
import com.kr.gallery.pro.custom.NotifyingLinearLayoutManager
import com.kr.gallery.pro.custom.OnLayoutCompleteCallback
import com.kr.gallery.pro.databases.GalleryDatabase
import com.kr.gallery.pro.helpers.*
import com.kr.gallery.pro.interfaces.MediumDao
import com.kr.gallery.pro.models.Directory
import com.kr.gallery.pro.viewmodels.DirectoryViewModel
import kotlinx.android.synthetic.main.fragment_all.*
import java.util.LinkedHashSet
import kotlin.math.abs

/**
 * 모두보기페지에 해당한 fragment
 * @see MainActivity
 * @see TimeFragment
 */
class AllFragment : Fragment(), SelectionUpdateListener, OnLayoutCompleteCallback {
    //부모 activity(기본화면)
    private lateinit var mMainActivity: MainActivity

    //View model자료
    private lateinit var directoryViewModel: DirectoryViewModel

    //자료기지 조작을 위해 MediumDao를 얻는다
    private lateinit var mMediumDao: MediumDao

    private var stPickPt = 0F
    private var scrollingUp = true

    //Camera, Settings아이콘 view에 animation을 주기 위한 변수들
    private var mAnimateDuration = 0
    private var mAnimateDistance = 0f
    private var mAnimator: Animator? = null

    //RecyclerView에서 리용할 adapter
    private lateinit var mDirectoryAdapter: DirectoryListAdapter
    private var selectedKeys = LinkedHashSet<Int>()
    private var allSelected = false

    /**
     * Scroll이 진행될때 camera settings아이콘들에 대한 animation을 준다.
     * @see onVerticalScroll
     */
    private val scrollListener = object : RecyclerView.OnScrollListener(){
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            onVerticalScroll(dy)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //상태정보를 불러들인다
        if(savedInstanceState != null) {
            selectedKeys = savedInstanceState.getSerializable(SELECTED_KEYS)!! as LinkedHashSet<Int>
            allSelected = savedInstanceState.getBoolean(ALL_SELECTED, false)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_all, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //등록부의 RecyclerView에 layout manager, adapter를 설정한다.
        directories_grid.layoutManager = NotifyingLinearLayoutManager(requireContext(), this)

        mDirectoryAdapter = DirectoryListAdapter(activity as BaseSimpleActivity, directories_grid, this)
        mDirectoryAdapter.selectedKeys = selectedKeys

        //보관된 선택상태를 복귀하고 main activity에 반영한다
        mMainActivity.updateSelectedCounts(selectedKeys.size, allSelected)
        mDirectoryAdapter.setSelectable(selectedKeys.size > 0)

        directories_grid.adapter = mDirectoryAdapter
        directories_grid.addOnScrollListener(scrollListener)

        //ViewModel설정
        if (requireContext().hasPermission(PERMISSION_WRITE_STORAGE)) {
            directoryViewModel = activity?.let { ViewModelProvider(it).get(DirectoryViewModel::class.java) }!!
            directoryViewModel.allDirectories.observe(viewLifecycleOwner, Observer {
                setupFolderSection(it as ArrayList<Directory>)
            })
        }

        open_setting.setOnClickListener{
            mMainActivity.launchSettings()
        }

        open_camera.setOnClickListener{
            mMainActivity.launchCamera()
        }
    }

    //Activity가 파괴될때 상태들을 보관한다
    override fun onSaveInstanceState(outState: Bundle) {
        //선택정보를 보관한다
        outState.putSerializable(SELECTED_KEYS, mDirectoryAdapter.selectedKeys)
        outState.putSerializable(ALL_SELECTED, mDirectoryAdapter.allSelected())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        mMainActivity = MainActivity.getMainActivity(context)

        //ViewModel설정
        if (requireContext().hasPermission(PERMISSION_WRITE_STORAGE)) {
            //MediumDao객체를 얻는다.
            mMediumDao = GalleryDatabase.getInstance(context).MediumDao()
        }

        //Animation을 할때 움직임거리, 지속시간을 설정한다
        mAnimateDuration = context.resources.getInteger(R.integer.cam_settings_animate_duration)
        mAnimateDistance = Utils.convertDpToPixel(CAM_SETTINGS_ANIMATE_DISTANCE.toFloat(), context)
    }

    //선택을 모두 해제한다.
    fun deselectAll() {
        mDirectoryAdapter.deselectAll()
    }

    //등록부목록 view에서 선택상태를 포함한 배렬을 초기화
    fun clearDirectorySelectedKeys() {
        mDirectoryAdapter.clearSelection()
    }

    //Folder부분 갱신
    private fun setupFolderSection(directories: ArrayList<Directory>){
        //recycler view 갱신
        var cameraHeader: DirectoryItem? = null
        var cameraContent: DirectoryItem? = null
        var movieHeader: DirectoryItem? = null
        var movieContent: DirectoryItem? = null
        var folderCount = 0
        var folderHeader: DirectoryItem? = null
        val folderContents = ArrayList<DirectoryItem>()
        val directoryItemList = ArrayList<DirectoryItem>()

        for (dir in directories) {
            when (dir.path) {
                CAMERA_PATH -> {
                    cameraHeader = DirectoryItem(DIRECTORY_HEADER, R.string.camera, dir.mediaCnt, null)
                    cameraContent = DirectoryItem(DIRECTORY_CONTENT, 0, 0, dir)
                }
                MOVIE_PATH -> {
                    movieHeader = DirectoryItem(DIRECTORY_HEADER, R.string.movie, dir.mediaCnt, null)
                    movieContent = DirectoryItem(DIRECTORY_CONTENT, 0, 0, dir)
                }
                else -> {
                    folderContents.add(DirectoryItem(DIRECTORY_CONTENT, 0, 0, dir))
                    folderCount ++
                }
            }
        }
        if(folderCount > 0) {
            folderHeader = DirectoryItem(DIRECTORY_HEADER, R.string.folder, folderCount, null)
        }
        if(cameraHeader != null){
            directoryItemList.add(cameraHeader)
            directoryItemList.add(cameraContent!!)
        }
        if(movieHeader != null) {
            directoryItemList.add(movieHeader)
            directoryItemList.add(movieContent!!)
        }
        if(folderCount > 0){
            directoryItemList.add(folderHeader!!)
            directoryItemList.addAll(folderContents)
        }
        setupDirectoryAdapter(directoryItemList)
    }

    //Adapter의 설정할 자료갱신
    private fun setupDirectoryAdapter(dirs: ArrayList<DirectoryItem>) {
        mDirectoryAdapter.setClickListener {any: Any, view: View?  ->
            val clickedItem = any as DirectoryItem
            if(clickedItem.itemType == DIRECTORY_CONTENT) {
                val path = clickedItem.directory!!.path
                itemClicked(path)
            }
        }
        mDirectoryAdapter.submitList(dirs)
    }

    //선택된 등록부항목의 개수
    fun getSelectedItemCount(): Int {
        return mDirectoryAdapter.getSelectedCounts()
    }

    //선택된 등록부안의 media들의 화일경로목록
    fun getSelectedMediaPath(): List<String> {
        val list = ArrayList<String>()
        val directoryList = getSelectedDirectoriesPath()
        val media = mMediumDao.getMediaByDirectoryList(directoryList)
        if(media.isNotEmpty())
            list.addAll(media.map { it.path })
        return list
    }

    //선택된 등록부경로들을 돌려준다
    fun getSelectedDirectoriesPath(): List<String> {
        val list = ArrayList<String>()
        list.addAll(mDirectoryAdapter.getSelectedPaths())
        return list
    }

    //선택된 등록부의 이름을 돌려준다
    fun getSelectedDirectoryName(): String {
        return mDirectoryAdapter.getFirstSelectedItem()!!.name
    }

    //선택된 등록부의 이름을 변경한다
    fun renameSelectedDirectory(newName: String) {
        //등록부이름으로부터 Full path를 얻어낸다.
        val originalPath = mDirectoryAdapter.getFirstSelectedItem()!!.path
        val lastIndex = originalPath.lastIndexOf('/')
        val newPath = originalPath.substring(0, lastIndex + 1) + newName

        if(originalPath == newPath)
            return

        //등록부이름을 변경한다.
        mMainActivity.renameFile(originalPath, newPath){
            //성공
            if(it) {
                mDirectoryAdapter.selectedKeys.remove(originalPath.hashCode())
                mDirectoryAdapter.selectedKeys.add(newPath.hashCode())
                mMainActivity.getMediaAndDirectories()
            }
            //실패
            else {
                activity?.toast(R.string.unknown_error_occurred)
            }
        }
    }

    override fun onDestroyView() {
        directories_grid.removeOnScrollListener(scrollListener)
        super.onDestroyView()
    }

    /**
     * Recyclerview를 scroll했을대 호출된다.
     * @param dy: 움직인 거리
     */
    fun onVerticalScroll(dy: Int) {
        if(dy > 0) {
            if(scrollingUp) {
                //animation이 이미 진행되고 있었다면 중지시킨다.
                mAnimator?.pause()

                //animation객체를 창조하고 시작한다.
                val animator = createCamSettingsAnimator(-mAnimateDistance)
                animator?.start()

                mAnimator = animator

                //같은 방향으로 scroll했을때는 animation을 진행하지 않게 한다.
                scrollingUp = false
            }
        } else if(dy < 0) {
            if(!scrollingUp) {
                //animation이 이미 진행되고 있었다면 중지시킨다.
                mAnimator?.pause()

                //animation객체를 창조하고 시작한다.
                val animator = createCamSettingsAnimator(0f)
                animator?.start()

                mAnimator = animator

                //같은 방향으로 scroll했을때는 animation을 진행하지 않게 한다.
                scrollingUp = true
            }
        }
    }

    /**
     * [cam_setting_icons] view를 우아래로 움직이는 animation을 창조한다.
     * @param targetPos: 목표위치
     */
    private fun createCamSettingsAnimator(targetPos: Float): Animator? {
        //Animator를 창조하고 거리에 해당한 지속시간을 계산한다.
        val animator = ObjectAnimator.ofFloat(cam_setting_icons, "translationY", targetPos)
        val transX = cam_setting_icons.translationY
        val duration = (mAnimateDuration * abs(targetPos - transX) /mAnimateDistance).toLong()

        animator.duration = duration
        return animator
    }

    /**
     * 이름변경단추가 enable되여야 하는가를 돌려주는 함수이다.
     * Camera, Movie등록부가 아닌 일반 등록부 하나만 선택되였을때에 true를 돌려준다.
     */
    private fun shouldEnableRenameView(): Boolean{
        val count = mDirectoryAdapter.getSelectedCounts()
        if(count != 1)
            return false

        val path = mDirectoryAdapter.getFirstSelectedItem()!!.path
        if(path == CAMERA_PATH || path == MOVIE_PATH)
            return false
        return true
    }

    //Recycler view의 layout에서 변화가 일어났을때 호출된다
    override fun onLayoutComplete() {
        //Scroll할수 없는 경우에 camera, settings 아이콘이 안보이는 상태이면 이것을 보여준다
        //이것은 camera, settings view가 scroll에 따라서만 보이고 숨겨지기때문이다
        val canScroll = directories_grid.canScrollVertically(1) || directories_grid.canScrollVertically(-1)
        if(!canScroll && !scrollingUp){
            onVerticalScroll(-1)
        }
    }

    /**
     * 선택이 변하였을때 호출된다.
     * 하나라도 선택되였을때 bottom sheet를 보여주고 하나도 선택안되였을때 bottom sheet숨겨준다.
     * 선택된 개수를 view에 반영한다.
     */
    override fun onSelectionUpdate() {
        val count = getSelectedItemCount()

        mMainActivity.updateSelectedCounts(count, mDirectoryAdapter.allSelected())
        mMainActivity.enableRenameView(shouldEnableRenameView())
        mMainActivity.actionBar?.hide()
        if(count > 0) {
            mMainActivity.showBottomSheet()
        } else {
            mMainActivity.showBottomShareBar(false)
            mMainActivity.hideBottomSheet()
        }

        mDirectoryAdapter.setSelectable(count > 0)
    }

    /**
     * @param path: 등록부경로
     * 등록부보기화면을 펼쳐준다.
     */
    private fun itemClicked(path: String) {
        activity?.let {
            //MediaActivity를 기동시킨다.
            Intent(it, MediaActivity::class.java).apply {
                putExtra(DIRECTORY, path)
                putExtra(DATE, "")
                putExtra(MEDIA_TYPE, 0)
                requireActivity().startActivity(this)
            }
        }
    }

    /**
     * 모두 선택/취소단추가 눌리웠을때 호출된다.
     */
    fun selectAll() {
        if(isAllSelected()) {
            //모든 항목을 선택해제
            mDirectoryAdapter.deselectAll()
        }
        else {
            //모든 항목을 선택
            mDirectoryAdapter.selectAll()
        }

        onSelectionUpdate()
    }

    //모두 선택되였는가
    private fun isAllSelected(): Boolean {
        return mDirectoryAdapter.allSelected()
    }
}
