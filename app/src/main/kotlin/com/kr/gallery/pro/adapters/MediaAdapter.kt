package com.kr.gallery.pro.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.kr.commons.activities.BaseSimpleActivity
import com.kr.commons.adapters.MyListAdapter
import com.kr.commons.extensions.*
import com.kr.commons.helpers.SHARE_BY_BLUETOOTH
import com.kr.commons.helpers.SHARE_BY_MESSAGE
import com.kr.commons.helpers.SHARE_BY_NOTE
import com.kr.commons.models.FileDirItem
import com.kr.commons.views.FastScroller
import com.kr.commons.views.MyRecyclerView
import com.kr.gallery.pro.R
import com.kr.gallery.pro.extensions.loadImage
import com.kr.gallery.pro.extensions.moveFilesTo
import com.kr.gallery.pro.extensions.shareMediaPaths
import com.kr.gallery.pro.models.Medium
import kotlinx.android.synthetic.main.photo_video_item_grid.view.*

/**
 * 등록부보기화면의 recyclerview에서 리용하는 adapter클라스
 * @param activity
 * @param recyclerView
 * @param fastScroller: scroll할때 오른쪽에 보여지는 scroller view
 */
class MediaAdapter(activity: BaseSimpleActivity, recyclerView: MyRecyclerView, fastScroller: FastScroller) :
        MyListAdapter<Medium, MediaAdapter.DiffCallback>(activity, recyclerView, fastScroller, null, DiffCallback()) {

    init {
        setupDragListener(true)
    }

    //media 목록
    private var media = ArrayList<Medium>()

    //Transition animation을 위한 목록자료
    private val transitionKeyPosList = ArrayList<Pair<String, Int>>()

    //OTG 련결되였는가?
    private val hasOTGConnected = activity.hasOTGConnected()

    fun findPosByTransitionKey(key: String): Int {
        for (pair in transitionKeyPosList) {
            if (pair.first == key) return pair.second
        }
        return 0
    }

    override fun getItemCount() = media.size
    override fun getSelectableItemCount() = media.size
    override fun getIsItemSelectable(position: Int) = true
    override fun getItemSelectionKey(position: Int) = media.getOrNull(position)?.path?.hashCode()
    override fun getItemKeyPosition(key: Int) = media.indexOfFirst { (it as? Medium)?.path?.hashCode() == key }

    override fun getActionMenuId() = 0
    override fun prepareActionMode(menu: Menu) {}
    override fun actionItemPressed(id: Int) {}
    override fun onActionModeCreated() {}
    override fun onActionModeDestroyed() {}

    //선택된 화상|등록부 목록
    private fun getSelectedItems() = selectedKeys.mapNotNull { getItemWithKey(it) } as ArrayList<Medium>
    //선택된 화상|동영상들의 경로 목록
    fun getSelectedPaths() = getSelectedItems().map { it.path } as ArrayList<String>

    /**
     * @param key: [getItemSelectionKey] 등록부경로를 나타내는 key정보
     * @return 해당한 key를 가진 등록부를 돌려준다
     */
    private fun getItemWithKey(key: Int): Medium? = media.firstOrNull { (it as? Medium)?.path?.hashCode() == key }

    /**
     * @return Scroll을 진행할때 scroller에 표시될 문자렬을 돌려준다.
     * @param position: item 위치
     * @param dateFormat
     * @param timeFormat
     */
    fun getItemBubbleText(position: Int, dateFormat: String, timeFormat: String): String {
        return (media[position] as? Medium)?.getBubbleText(activity, dateFormat, timeFormat) ?: ""
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(R.layout.photo_video_item_grid, parent)
    }

    /**
     * @see com.kr.gallery.pro.fragments.PhotoFragment
     * @see com.kr.gallery.pro.fragments.VideoThumbFragment
     * @see MyListAdapter.ViewHolder.bindView
     * @see setupThumbnail
     * @see bindViewHolder
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //해당 위치의 Medium정보를 얻는다.
        val tmbItem = media.getOrNull(position) ?: return

        val transitionView = holder.itemView.findViewById<ImageView>(R.id.medium_thumbnail)

        holder.bindView(tmbItem, transitionView, true) {
            setupThumbnail(it, tmbItem)
        }

        bindViewHolder(holder)
    }

    //Glide로 적재해준 화상에 대하여 기억공간을 해제한다.
    override fun onViewRecycled(holder: ViewHolder) {
        if (!activity.isDestroyed) {
            val tmb = holder.itemView.medium_thumbnail
            if (tmb != null) {
                Glide.with(activity).clear(tmb)
            }
        }
    }

    fun setClickListener(callback: (Any, View?) -> Unit) {
        itemClick = callback
    }

    //submitList하기 전에 등록부목록을 보관해둔다.
    //transition key정보들을 갱신한다.
    override fun submitList(list: MutableList<Medium>?) {
        if(list != null) {
            media.clear()
            media.addAll(list)
            transitionKeyPosList.clear()
            for(i in 0 until media.size) {
                transitionKeyPosList.add(Pair(media[i].path.hashCode().toString(), i))
            }
        }
        super.submitList(list)
    }

    /**
     * 선택된 화상|동영상들을 전송한다.
     * @param target: 전송방식 [SHARE_BY_MESSAGE], [SHARE_BY_NOTE], [SHARE_BY_BLUETOOTH]
     */
    fun shareMedia(target: Int) {
        activity.shareMediaPaths(getSelectedPaths(), target)
    }

    //선택된 화일들을 이동
    fun moveFilesTo(callback: (success: Boolean, failedCount: Int) -> Unit) {
        val paths = getSelectedPaths()
        val fileDirItems = paths.asSequence().map {
            FileDirItem(it, it.getFilenameFromPath())
        }.toMutableList() as ArrayList

        activity.moveFilesTo(fileDirItems, callback)
    }
    /**
     * 화상을 view에 설정한다
     * @param view: [R.layout.photo_video_item_grid]
     * @param medium: 화상정보 [Medium]
     */
    private fun setupThumbnail(view: View, medium: Medium) {
        val isSelected = selectedKeys.contains(medium.path.hashCode())
        view.apply {
            //동영상인 경우 동영상아이콘 현시
            play_outline.beVisibleIf(medium.isVideo())

            //동영상인 경우 동영상길이 현시
            val showVideoDuration = medium.isVideo()
            if (showVideoDuration) {
                video_duration.text = medium.videoDuration.getFormattedDuration()
            }
            video_duration.beVisibleIf(showVideoDuration)

            //선택되였을때에는 푸른색mask화상 보여준다.
            medium_check?.beVisibleIf(isSelected)

            //화상현시
            var path = medium.path
            if (hasOTGConnected && context.isPathOnOTG(path)) {
                path = path.getOTGPublicPath(context)
            }
            activity.loadImage(medium.type, path, medium_thumbnail, null)
        }
    }

    /**
     * [DiffUtil.ItemCallback]
     * 2개의 [Medium]비교
     */
    class DiffCallback: DiffUtil.ItemCallback<Medium>(){
        override fun areItemsTheSame(oldItem: Medium, newItem: Medium): Boolean {
            //화일경로만 비교한다.
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: Medium, newItem: Medium): Boolean {
            //화일경로를 제외한 나머지 정보(크기, 수정시간, 촬영시간)들을 비교한다.
            return oldItem.size == newItem.size && oldItem.modified == newItem.modified && oldItem.dateTaken == newItem.dateTaken
        }
    }
}
