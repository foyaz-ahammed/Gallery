package com.kr.gallery.pro.fragments

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.kr.commons.extensions.getSharedPrefs
import com.kr.commons.extensions.hasPermission
import com.kr.commons.helpers.CAM_SETTINGS_ANIMATE_DISTANCE
import com.kr.commons.helpers.PERMISSION_WRITE_STORAGE
import com.kr.gallery.pro.R
import com.kr.gallery.pro.activities.MainActivity
import com.kr.gallery.pro.adapters.TimeFolderAdapter
import com.kr.gallery.pro.custom.NotifyingLinearLayoutManager
import com.kr.gallery.pro.custom.OnLayoutCompleteCallback
import com.kr.gallery.pro.extensions.config
import com.kr.gallery.pro.helpers.*
import com.kr.gallery.pro.models.*
import com.kr.gallery.pro.viewmodels.CoverPageViewModel
import com.kr.gallery.pro.viewmodels.TimeThumbViewModel
import kotlinx.android.synthetic.main.fragment_all.*
import kotlinx.android.synthetic.main.fragment_time.cam_setting_icons
import kotlinx.android.synthetic.main.fragment_time.directories_grid
import kotlinx.android.synthetic.main.fragment_time.open_camera
import kotlinx.android.synthetic.main.fragment_time.open_setting
import kotlin.collections.ArrayList
import kotlin.math.abs

/**
 * 사진첩보기페지에 해당한 fragment
 * @see MainActivity
 * @see AllFragment
 */
class TimeFragment : Fragment(), OnLayoutCompleteCallback, SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var coverPageViewModel: CoverPageViewModel
    private lateinit var timeThumbViewModel: TimeThumbViewModel
    private var scrollingUp = true

    //기본화면 activity
    private lateinit var mMainActivity: MainActivity

    //자식 view들
    private lateinit var mThumbView: RecyclerView

    //Recyclerview에서 리용하는 adapter
    private lateinit var mThumbListAdapter: TimeFolderAdapter

    private var mIsCoverItemSet = false

    //Camera, Settings아이콘 view에 animation을 주기 위한 변수들
    private var mAnimateDuration = 0
    private var mAnimateDistance = 0f
    private var mAnimator: Animator? = null

    /**
     * Scroll이 진행될때 camera settings아이콘들에 대한 animation을 준다.
     * @see onVerticalScroll
     */
    private val scrollListener = object : RecyclerView.OnScrollListener(){
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            onVerticalScroll(dy)
        }
    }

    //Preference변화를 감지하기 위하여 preference변수를 만든다.
    private lateinit var mPrefs: SharedPreferences

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_time, container, false)

        //RecyclerView를 얻고 거기에 layoutmanager와 adapter를 설정한다.
        mThumbView = view.findViewById(R.id.directories_grid)

        mThumbView.layoutManager = NotifyingLinearLayoutManager(requireContext(), this)

        mThumbListAdapter = TimeFolderAdapter(requireContext(), this)
        mThumbView.adapter = mThumbListAdapter

        //Scroll변화를 감지한다.
        mThumbView.addOnScrollListener(scrollListener)

        return view;
    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //ViewModel설정
        if (requireContext().hasPermission(PERMISSION_WRITE_STORAGE)) {
            //Cover page에 해당한 viewmodel자료를 얻는다.
            coverPageViewModel = activity?.let { ViewModelProvider(it).get(CoverPageViewModel::class.java) }!!
            coverPageViewModel.allCoverPageItems.observe(viewLifecycleOwner, Observer { it ->
                //App이 처음 기동했을때 빈 자료가 들어오면 갱신을 진행하지 않는다
                if(it.isNotEmpty() || !mMainActivity.config.isFirstMediaAction()) {
                    mIsCoverItemSet = true
                    mThumbListAdapter.setCoverItem(it as ArrayList<Medium>)

                    //자료기지가 변하는데 view가 갱신이 안되는 경우가 있으므로 림시 Log를 현시하도록 하였음(Tag: "TimeFragment-Observe")
                    Log.w("TimeFragment-Observe", "media count: " + it.size)
                    it.forEach {
                        Log.w("TimeFragment-Observe", "medium: $it")
                    }
                }
            })

            //시간별 화상을 얻는 TimeThumbViewModel자료를 얻는다.
            timeThumbViewModel = activity?.let { ViewModelProvider(it).get(TimeThumbViewModel::class.java) }!!
            timeThumbViewModel.allTimeThumbItemsWithMedia.observe(viewLifecycleOwner, Observer { it ->
                val thumbnailItems = it as ArrayList<TimeThumbnailItemJsonMedia>
                setupThumbnailsAdapter(thumbnailItems.map { TimeThumbnailItem(it) } as ArrayList<TimeThumbnailItem>)
            })
        }

        open_setting.setOnClickListener{
            mMainActivity.launchSettings()
        }
        open_camera.setOnClickListener{
            mMainActivity.launchCamera()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        //부모 activity를 얻는다
        mMainActivity = MainActivity.getMainActivity(context)

        //Preference얻기
        mPrefs = context.getSharedPrefs()
        mPrefs.registerOnSharedPreferenceChangeListener(this)

        //Animation을 위한 변수값들을 얻는다
        mAnimateDuration = context.resources.getInteger(R.integer.cam_settings_animate_duration)
        mAnimateDistance = Utils.convertDpToPixel(CAM_SETTINGS_ANIMATE_DISTANCE.toFloat(), context)
    }

    override fun onDetach() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(this)
        super.onDetach()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if(sharedPreferences == null || key == null)
            return

        //App이 처음 기동했을때 첫 자료기지 접근시 화상|동영상이 하나도 없는것으로 하여
        //자료기지,ViewModel이 갱신안되는 경우 수동적으로 adapter자료를 설정한다.
        if(key == FIRST_MEDIA_ACTION) {
            val isFirst = context?.config?.isFirstMediaAction()?: return
            if(!isFirst && !mIsCoverItemSet && !MainActivity.DATABASE_UPDATED) {
                mIsCoverItemSet = true
                mThumbListAdapter.setCoverItem(ArrayList())
            }
        }
    }

    private fun setupThumbnailsAdapter(timeThumbnailList: ArrayList<TimeThumbnailItem>) {
        mThumbListAdapter.submitList(timeThumbnailList.cloneList())
    }

    override fun onDestroyView() {
        directories_grid.removeOnScrollListener(scrollListener)
        super.onDestroyView()
    }

    /**
     * [cam_setting_icons] view를 우아래로 움직이는 animation을 창조한다.
     * @param targetPos: 목표위치
     */
    private fun createCamSettingsAnimator(targetPos: Float): Animator? {
        //Animator를 창조하고 거리에 해당한 지속시간을 계산한다.
        val animator = ObjectAnimator.ofFloat(cam_setting_icons, "translationY", targetPos)
        val transX = cam_setting_icons.translationY
        val duration = (mAnimateDuration * abs(targetPos - transX)/mAnimateDistance).toLong()

        animator.duration = duration
        return animator
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
                animator!!.start()

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
                animator!!.start()

                mAnimator = animator

                //같은 방향으로 scroll했을때는 animation을 진행하지 않게 한다.
                scrollingUp = true
            }
        }
    }

    //Recycler view의 layout이 변하였을때 호출된다
    override fun onLayoutComplete() {
        //Scroll할수 없는 경우에 camera, settings 아이콘이 안보이는 상태이면 이것을 보여준다
        //이것은 camera, settings view가 scroll에 따라서만 보이고 숨겨지기때문이다
        val canScroll = directories_grid.canScrollVertically(1) || directories_grid.canScrollVertically(-1)
        if(!canScroll && !scrollingUp){
            onVerticalScroll(-1)
        }
    }
}
