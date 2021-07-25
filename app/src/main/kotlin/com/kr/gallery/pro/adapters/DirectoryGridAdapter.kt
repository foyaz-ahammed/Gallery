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
import com.kr.gallery.pro.extensions.getPathLocation
import com.kr.gallery.pro.extensions.loadImage
import com.kr.gallery.pro.helpers.*
import com.kr.gallery.pro.models.Directory
import kotlinx.android.synthetic.main.directory_item_grid.view.*

/**
 * @see com.kr.gallery.pro.activities.ManageCoverActivity
 * '사진첩에 그림추가'화면의 recyclerview에서 리용하는 adapter
 */
class DirectoryGridAdapter(activity: BaseSimpleActivity, recyclerView: MyRecyclerView,
                           private val selectionUpdateListener: SelectionUpdateListener) :
        MyListAdapter<Directory, DirectoryGridAdapter.DiffCallback>(activity, recyclerView, null, null, DiffCallback()) {

    //등록부목록
    private val dirs = ArrayList<Directory>()

    init {
        setupDragListener(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutType = R.layout.directory_item_grid
        return createViewHolder(layoutType, parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dir = getItem(position)

        holder.bindView(dir, null, false) { itemView ->
            setupView(itemView, dir)
        }

        bindViewHolder(holder)
    }

    //선택이 갱신되였을때
    override fun updateTitle(){
        selectionUpdateListener.onSelectionUpdate()
    }

    //Menu는 필요없으므로 아래의 함수내용들은 정의하지 않는다.
    override fun getActionMenuId() = 0
    override fun prepareActionMode(menu: Menu) {}
    override fun actionItemPressed(id: Int) {}
    override fun onActionModeCreated() {}
    override fun onActionModeDestroyed() {}

    //Glide로 적재해준 화상에 대하여 기억공간을 해제한다.
    override fun onViewRecycled(holder: ViewHolder) {
        if (!activity.isDestroyed && holder.itemViewType == DIRECTORY_CONTENT) {
            Glide.with(activity).clear(holder.itemView.dir_thumbnail!!)
        }
    }

    //submitlist하기전에 등록부목록을 보관해둔다.
    override fun submitList(list: MutableList<Directory>?) {
        if(list != null){
            dirs.clear()
            dirs.addAll(list)
        }
        super.submitList(list)
    }

    //등록부선택과 관련한 함수들
    override fun getSelectableItemCount() = dirs.size
    override fun getIsItemSelectable(position: Int) = true
    override fun getItemSelectionKey(position: Int) = dirs.getOrNull(position)?.path?.hashCode()
    override fun getItemKeyPosition(key: Int) = dirs.indexOfFirst { it.path.hashCode() == key }
    override fun allSelected() = getSelectedCounts() == itemCount
    private fun getSelectedItems() = selectedKeys.mapNotNull { getItemWithKey(it) } as ArrayList<Directory>
    fun getSelectedPaths() = getSelectedItems().map { it.path } as ArrayList<String>
    private fun getItemWithKey(key: Int): Directory? = dirs.firstOrNull { it.path.hashCode() == key }

    //View설정
    private fun setupView(view: View, dir: Directory) {
        val isSelected = selectedKeys.contains(dir.path.hashCode())
        view.apply {
            //등록부이름, 화상개수를 현시
            dir_name.text = dir.name
            photo_cnt.text = dir.mediaCnt.toString()

            //등록부화상 설정
            val thumbnailType = when {
                dir.tmb.isVideoFast() -> TYPE_VIDEOS
                dir.tmb.isGif() -> TYPE_GIFS
                dir.tmb.isRawFast() -> TYPE_RAWS
                dir.tmb.isSvg() -> TYPE_SVGS
                else -> TYPE_IMAGES
            }
            activity.loadImage(thumbnailType, dir.tmb, dir_thumbnail)

            //선택되였으면 푸른색 mask 화상을 보여준다
            dir_check?.beVisibleIf(isSelected)

            //등록부아이콘 설정
            val location = context.getPathLocation(dir.path)
            dir_location.beVisibleIf(location != LOCATION_INTERNAL)
            if (dir_location.isVisible()) {
                dir_location.setImageResource(if (location == LOCATION_SD) R.drawable.ic_sd_card_vector else R.drawable.ic_usb_vector)
            }
        }
    }

    /**
     * @see [DiffUtil.ItemCallback]
     * @see HiddenDirectoryListAdapter.DiffCallback
     * 2개의 [Directory]자료를 비교한다
     */
    class DiffCallback: DiffUtil.ItemCallback<Directory>(){
        override fun areItemsTheSame(oldItem: Directory, newItem: Directory): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: Directory, newItem: Directory): Boolean {
            return oldItem.name == newItem.name && oldItem.tmb == newItem.tmb && oldItem.mediaCnt == newItem.mediaCnt
        }
    }
}
