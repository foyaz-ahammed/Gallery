package com.kr.gallery.pro.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.kr.commons.activities.BaseSimpleActivity
import com.kr.commons.adapters.MyListAdapter
import com.kr.commons.extensions.*
import com.kr.commons.views.MyRecyclerView
import com.kr.gallery.pro.R
import com.kr.gallery.pro.extensions.getFolderNameFromPath
import com.kr.gallery.pro.extensions.getPathLocation
import com.kr.gallery.pro.extensions.loadImage
import com.kr.gallery.pro.helpers.*
import com.kr.gallery.pro.models.HiddenDirectory
import kotlinx.android.synthetic.main.directory_item_list.view.*

/**
 * 숨긴등록부화면의 recyclerview 에서 리용하는 adapter클라스
 * @see HiddenDirectory
 * @param activity: recyclerview를 가지고 있는 activity
 * @param recyclerView: adapter가 적용될 recyclerview
 * @param selectionUpdateListener: 선택변화를 감지하는 listener
 */
class HiddenDirectoryListAdapter(activity: BaseSimpleActivity, recyclerView: MyRecyclerView,
                                 private val selectionUpdateListener: SelectionUpdateListener) :
        MyListAdapter<HiddenDirectory, HiddenDirectoryListAdapter.DiffCallback>(activity, recyclerView, null, null, DiffCallback()) {

    //구성자
    init {
        setupDragListener(true)
    }

    //등록부목록
    private val dirs = ArrayList<HiddenDirectory>()

    //등록부선택을 관리하기 위한 함수들 정의
    override fun getSelectableItemCount() = dirs.size
    override fun getIsItemSelectable(position: Int) = true
    override fun getItemSelectionKey(position: Int) = dirs.getOrNull(position)?.folderPath?.hashCode()
    override fun getItemKeyPosition(key: Int) = dirs.indexOfFirst { it.folderPath.hashCode() == key }

    //선택된 등록부목록
    private fun getSelectedItems() = selectedKeys.mapNotNull { getItemWithKey(it) } as ArrayList<HiddenDirectory>
    //선택된 등록부들의 경로 목록
    fun getSelectedPaths() = getSelectedItems().map { it.folderPath } as ArrayList<String>

    /**
     * @param key: [getItemSelectionKey] 등록부경로를 나타내는 key정보
     * @return 해당한 key를 가진 등록부를 돌려준다
     */
    private fun getItemWithKey(key: Int): HiddenDirectory? = dirs.firstOrNull { it.folderPath.hashCode() == key }

    //Menu가 없으므로 아래의 함수들의 내용은 정의해주지 않는다.
    override fun getActionMenuId() = 0
    override fun onActionModeCreated() {}
    override fun onActionModeDestroyed() {}
    override fun actionItemPressed(id: Int) {}
    override fun prepareActionMode(menu: Menu) {}

    override fun updateTitle(){
        selectionUpdateListener.onSelectionUpdate()
    }

    fun setClickListener(callback: (Any, View?) -> Unit) {
        itemClick = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(R.layout.directory_item_list, parent)
    }

    /**
     * @see MyListAdapter.ViewHolder.bindView
     * @see setupView
     * @see bindViewHolder
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dir = getItem(position)
        holder.bindView(dir, null, false) { itemView ->
            setupView(itemView, dir)
        }
        bindViewHolder(holder)
    }

    //Glide로 적재해준 화상에 대하여 기억공간을 해제한다.
    override fun onViewRecycled(holder: ViewHolder) {
        if (!activity.isDestroyed) {
            Glide.with(activity).clear(holder.itemView.dir_thumbnail!!)
        }
    }

    //submitList하기 전에 등록부목록을 보관해둔다
    override fun submitList(list: MutableList<HiddenDirectory>?) {
        dirs.clear()
        if(list != null)
            dirs.addAll(list)
        super.submitList(list)
    }

    /**
     * 등록부정보를 view에 반영한다(화상, 아이콘, 등록부이름, media개수)
     * @param view: [R.layout.directory_item_list]
     * @param directory: 등록부정보 [HiddenDirectory]
     */
    private fun setupView(view: View, directory: HiddenDirectory) {
        //항목이 선택되였는가 얻기
        val isSelected = selectedKeys.contains(directory.folderPath.hashCode())

        //View설정
        view.apply {
            //등록부이름, media개수 현시
            dir_name.text = activity.getFolderNameFromPath(directory.folderPath)
            photo_cnt.text = directory.mediaCount.toString()

            //등록부화상 현시
            val thumbnailType = when {
                directory.thumbnail.isVideoFast() -> TYPE_VIDEOS
                directory.thumbnail.isGif() -> TYPE_GIFS
                directory.thumbnail.isRawFast() -> TYPE_RAWS
                directory.thumbnail.isSvg() -> TYPE_SVGS
                else -> TYPE_IMAGES
            }
            dir_thumbnail.isList = true
            activity.loadImage(thumbnailType, directory.thumbnail, dir_thumbnail)

            //등록부가 선택되였으면 푸른색 mask화상 view를 보여주고 그렇지 않으면 숨겨준다
            dir_check?.beVisibleIf(isSelected)

            val location = context.getPathLocation(directory.folderPath)
            dir_location.beVisibleIf(location != LOCATION_INTERNAL)
            if (dir_location.isVisible()) {
                dir_location.setImageResource(if (location == LOCATION_SD) R.drawable.ic_sd_card_vector else R.drawable.ic_usb_vector)
            }
        }
    }

    /**
     * [DiffUtil.ItemCallback]
     * 2개의 [HiddenDirectory]비교
     */
    class DiffCallback: DiffUtil.ItemCallback<HiddenDirectory>(){
        override fun areItemsTheSame(oldItem: HiddenDirectory, newItem: HiddenDirectory): Boolean {
            //등록부경로만 비교한다.
            return oldItem.folderPath == newItem.folderPath
        }

        override fun areContentsTheSame(oldItem: HiddenDirectory, newItem: HiddenDirectory): Boolean {
            //등록부경로를 제외한 나머지 정보들을 비교한다.
            return oldItem.thumbnail == newItem.thumbnail && oldItem.mediaCount == newItem.mediaCount
        }
    }
}
