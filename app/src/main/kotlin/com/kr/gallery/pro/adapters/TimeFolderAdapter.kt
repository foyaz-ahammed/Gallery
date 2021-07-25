package com.kr.gallery.pro.adapters

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kr.commons.extensions.areDigitsOnly
import com.kr.commons.helpers.DATE_FORMAT_ONE
import com.kr.commons.helpers.DAY_SECONDS
import com.kr.commons.helpers.ensureBackgroundThread
import com.kr.gallery.pro.R
import com.kr.gallery.pro.activities.ManageCoverActivity
import com.kr.gallery.pro.activities.MediaActivity
import com.kr.gallery.pro.activities.ViewPagerActivity
import com.kr.gallery.pro.extensions.coverPageDB
import com.kr.gallery.pro.helpers.*
import com.kr.gallery.pro.models.Medium
import com.kr.gallery.pro.models.SliderItem
import com.kr.gallery.pro.models.TimeThumbnailItem
import com.smarteist.autoimageslider.IndicatorView.animation.type.BaseAnimation.DEFAULT_ANIMATION_TIME
import com.smarteist.autoimageslider.SliderAnimations
import com.smarteist.autoimageslider.SliderView
import com.wang.avi.AVLoadingIndicatorView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * 사진첩보기화면의 recycler view에서 리용하는 adapter클라스이다.
 * Cover page와 날자별 화상목록을 slide현시한다.
 * @param context
 * @param ownerFragment: life cycle(onResume, onPause, onDestroy, ...)을 감시하기 위한 대상 fragment
 */
class TimeFolderAdapter(private val context: Context, private val ownerFragment: Fragment): ListAdapterWithHeader<TimeThumbnailItem, RecyclerView.ViewHolder>(DiffCallback()) {
    private var mCoverPageItems: ArrayList<Medium>? = null

    //Layout inflater
    private val mLayoutInflater: LayoutInflater = LayoutInflater.from(context)

    companion object{
        //Header view의 위치(0)
        const val HEADER_POSITION = 0

        const val VIEW_TYPE_COVER = 100
        const val VIEW_TYPE_TIME = 101
    }

    /**
     * Cover page의 화상들을 설정한다.
     * @param list: 화상목록
     */
    fun setCoverItem(list: ArrayList<Medium>) {
        if(mCoverPageItems != list) {
            if(mCoverPageItems == null)
                mCoverPageItems = ArrayList()

            mCoverPageItems!!.clear()
            mCoverPageItems!!.addAll(list)
            notifyItemChanged(HEADER_POSITION)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if(viewType == VIEW_TYPE_COVER){
            val itemView = mLayoutInflater.inflate(R.layout.time_folder_cover_layout, parent, false)
            CoverViewHolder(itemView)
        }
        else{
            val itemView = mLayoutInflater.inflate(R.layout.time_folder_list_item, parent, false)
            TimeViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewType = getItemViewType(position)

        if(viewType == VIEW_TYPE_TIME) {
            holder as TimeViewHolder

            //들어온 Medium자료로부터 SliderItem형의 자료를 얻는다.
            val thumbnailItem = getItem(position)
            val slideImageData = thumbnailItem.mediaList.map {
                val cal = Calendar.getInstance(Locale.ENGLISH)
                cal.timeInMillis = it.dateTaken
                val timeString = context.getString(R.string.time_string, cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE))
                SliderItem(timeString, it.fullPath)
            } as ArrayList<SliderItem>

            //자동순환을 할수 있는 view들 가운데서 현재 view의 위치를 계산한다.
            var cycleCount = 0
            for (i in 1 .. position){
                if(getItem(i).count > 1)
                    cycleCount ++
            }

            //View를 갱신한다.
            holder.bind(
                    slideImageData,
                    thumbnailItem.date,
                    thumbnailItem.count,
                    thumbnailItem.type,
                    cycleCount
            )
            holder.itemView.tag = position

            //time view들에는 top margin을 없앤다.
            val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
            layoutParams.topMargin = 0
        }
        else {
            holder as CoverViewHolder
            if(mCoverPageItems != null)
                holder.bind(mCoverPageItems!!)
            holder.itemView.tag = position

            //맨 우의 cover page view에는 50dp의 top margin을 준다.
            val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
            layoutParams.topMargin = Utils.convertDpToPixel(50f, context).toInt()
        }
    }

    //view가 recycle되거나 자동순환을 중지한다.
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        val position = holder.adapterPosition
        if(position == HEADER_POSITION) {
            holder as CoverViewHolder
            holder.getSliderView().stopAutoCycle()
        }
        else {
            holder as TimeViewHolder
            holder.getSliderView().stopAutoCycle()
        }
    }

    //맨우(0)위치는 CoverPage ViewType, 그 외는 Time ViewType 를 돌려준다.
    override fun getItemViewType(position: Int): Int {
        return if(position == HEADER_POSITION) VIEW_TYPE_COVER else VIEW_TYPE_TIME
    }

    /**
     * @see DiffUtil.ItemCallback
     * 2개의 [TimeThumbnailItem]자료를 비교한다.
     */
    private class DiffCallback: DiffUtil.ItemCallback<TimeThumbnailItem>(){
        override fun areItemsTheSame(oldItem: TimeThumbnailItem, newItem: TimeThumbnailItem): Boolean {
            return oldItem.date == newItem.date && oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: TimeThumbnailItem, newItem: TimeThumbnailItem): Boolean {
            return oldItem.count == newItem.count && oldItem.mediaList == newItem.mediaList
        }
    }

    //Coverpage에서 click했을때
    private fun coverPageItemClicked(path: String) {
        context.let {
            ensureBackgroundThread {
                val coverPageDirectories = it.coverPageDB.getDirectories() as ArrayList<String>

                Intent(it, ViewPagerActivity::class.java).apply {
                    putExtra(PATH, path)
                    putExtra(DIRECTORY_LIST, coverPageDirectories)
                    it.startActivity(this)
                }
            }
        }
    }

    /**
     * Time페지 thumbnail을 click했을때 호출된다
     * @param date 날자
     * @param type 화상|동영상 [MEDIA_IMAGE] 혹은 [MEDIA_MOVIE]
     */
    private fun thumbViewItemClicked(date: String, type: Int) {
        //등록부보기화면을 연다.
        Intent(context, MediaActivity::class.java).apply {
            putExtra(DIRECTORY, "")
            putExtra(DATE, date)
            putExtra(MEDIA_TYPE, type)
            context.startActivity(this)
        }
    }

    /**
     * Time항목에 해당한 ViewHolder
     */
    inner class TimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //자식 view들
        private var imageSlider: SliderView
        private var displayDate: TextView
        private var displayTime: TextView
        private var videoOverlay: ImageView
        private var counts: TextView

        init {
            //자식 view들을 얻는다.
            itemView.let {
                imageSlider = it.findViewById(R.id.image_slider)
                displayDate = it.findViewById(R.id.date_label)
                displayTime = it.findViewById(R.id.time_label)
                videoOverlay = it.findViewById(R.id.video_overlay)
                counts = it.findViewById(R.id.counts)
            }
        }

        //자식 view들을 갱신한다.
        fun bind(sliderItems: ArrayList<SliderItem>, date: String, itemCounts: Int, type: Int, cycleCount: Int) {
            //자동순환하기전에 주는 지연시간 계산
            val startDelay = (DEFAULT_ANIMATION_TIME * ((cycleCount % 10) + 1)).toLong()

            //slider view설정
            imageSlider.apply {
                setSliderAdapter(
                        ImageListSliderAdapter(context, sliderItems) {
                            thumbViewItemClicked(date, type)
                        }
                )

                setSliderTransformAnimation(SliderAnimations.SIMPLETRANSFORMATION)
                autoCycleDirection = SliderView.AUTO_CYCLE_DIRECTION_RIGHT
                scrollTimeInSec = 2
                isAutoCycle = true
                setLifecycle(ownerFragment.lifecycle, startDelay)

                setCurrentPageListener{
                    if(sliderItems.size > it)
                        displayTime.text = sliderItems[it].dateTimeStr
                }
                startAutoCycleAfterDelay()
            }

            //년, 월, 일을 얻어낸다.
            val dateValues = date.split(".")
            val year = dateValues[0].toInt()
            val month = dateValues[1].toInt()
            val day = dateValues[2].toInt()

            //날자문자렬을 얻어낸다.(오늘, 어제, 2021.1.1)
            val today = formatDate(System.currentTimeMillis())
            val yesterday = formatDate(System.currentTimeMillis() - DAY_SECONDS * 1000)
            val formattedDate =
                    when {
                        today == date -> {
                            context.getString(R.string.today)
                        }
                        yesterday == date -> {
                            context.getString(R.string.yesterday)
                        }
                        else -> {
                            context.getString(R.string.date_format_label, year, month, day)
                        }
                    }

            //날자, 시간, 개수 label들의 text를 설정한다
            if(type == MEDIA_IMAGE) {
                displayDate.text = formattedDate
                videoOverlay.visibility = View.GONE
                displayDate.visibility = View.VISIBLE
            } else {
                displayDate.text = formattedDate
                videoOverlay.visibility = View.VISIBLE  // Display Film Overlay for Movie thumbnail
                displayDate.visibility = View.VISIBLE
            }

            if(sliderItems.isNotEmpty())
                displayTime.text = sliderItems[0].dateTimeStr

            counts.text = context.getString(R.string.media_count, itemCounts)
        }

        private fun formatDate(timeMillis: Long): String {
            val cal = Calendar.getInstance(Locale.ENGLISH)
            cal.timeInMillis = timeMillis
            return DateFormat.format(DATE_FORMAT_ONE, cal).toString()
        }

        fun getSliderView(): SliderView {
            return imageSlider
        }
    }

    /**
     * Cover page항목에 대한 ViewHolder
     */
    inner class CoverViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        //자식 view들
        private val loadingView: AVLoadingIndicatorView
        private val addPathBtn: ImageView
        private val sliderView: SliderView

        //slider view에 설정할 adapter
        private val mAdapter = CoverPageSliderAdapter()

        init {
            //자식 view들 얻기
            itemView.apply {
                loadingView = findViewById(R.id.progress)
                sliderView = findViewById(R.id.sliderView)
                addPathBtn = findViewById(R.id.add_new_paths)
            }

            //sliderview설정
            sliderView.apply {
                setSliderAdapter(mAdapter)
                setSliderTransformAnimation(SliderAnimations.SIMPLETRANSFORMATION)
                autoCycleDirection = SliderView.AUTO_CYCLE_DIRECTION_RIGHT
                scrollTimeInSec = 2
                isAutoCycle = true
                startAutoCycle()
                setLifecycle(ownerFragment.lifecycle, 0)
            }

            //'+'단추 click사건을 정의한다.
            addPathBtn.setOnClickListener {
                //'사진첩에 그림추가'화면을 현시한다.
                context.startActivity(Intent(context, ManageCoverActivity::class.java))
            }
        }

        fun bind(coverItems: ArrayList<Medium>) {
            //'loading'아이콘을 숨긴다.
            loadingView.smoothToHide()

            //SliderView의 image갱신
            val sliderItems = ArrayList<SliderItem>()
            coverItems.forEach {
                //Get date string from time Millis
                val sdf = SimpleDateFormat("yyyy MM dd hh:mm a")
                val date = Date(it.modified)
                val dateStr = sdf.format(date)

                //Generate slider item, and add to list
                sliderItems.add(SliderItem(dateTimeStr = dateStr, imagePath = it.path))
            }

            //화상목록을 설정한다.
            mAdapter.setupAdapterData(sliderItems) {
                if(it is SliderItem) {
                    coverPageItemClicked(it.imagePath)
                }
            }
            mAdapter.notifyDataSetChanged()

            //'등록부추가'단추 현시
            addPathBtn.visibility = if(coverItems.isEmpty()) View.VISIBLE else View.GONE
        }

        fun getSliderView(): SliderView {
            return sliderView
        }
    }
}
