package com.kr.gallery.pro.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kr.commons.activities.BaseSimpleActivity
import com.kr.commons.adapters.MyListAdapter
import com.kr.commons.extensions.*
import com.kr.commons.views.FastScroller
import com.kr.commons.views.MyRecyclerView
import com.kr.gallery.pro.R
import com.kr.gallery.pro.extensions.config
import com.kr.gallery.pro.extensions.getPathLocation
import com.kr.gallery.pro.extensions.loadImage
import com.kr.gallery.pro.helpers.*
import com.kr.gallery.pro.models.Directory
import kotlinx.android.synthetic.main.directory_item_header.view.*
import kotlinx.android.synthetic.main.directory_item_list.view.*

/**
 * 모두보기화면의 recyclerview에서 리용하는 adapter클라스
 */
class DirectoryListAdapter(activity: BaseSimpleActivity, recyclerView: MyRecyclerView,
                           private val selectionUpdateListener: SelectionUpdateListener) :
        MyListAdapter<DirectoryItem, DirectoryListAdapter.DiffCallback>(activity, recyclerView, null, null, DiffCallback()) {

    init {
        setupDragListener(true)
    }

    private val dirs = ArrayList<Directory>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutType = if(viewType == DIRECTORY_HEADER) R.layout.directory_item_header else R.layout.directory_item_list
        return createViewHolder(layoutType, parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dir = getItem(position)
        holder.bindView(dir, null, false) { itemView ->
            setupView(itemView, dir)
        }

        //맨 첫행에는 margin을 준다
        val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
        if(position == 0) {
            layoutParams.topMargin = Utils.convertDpToPixel(30f, activity).toInt()
        }
        else {
            layoutParams.topMargin = 0
        }
        bindViewHolder(holder)
    }

    override fun updateTitle(){
        selectionUpdateListener.onSelectionUpdate()
    }

    fun setClickListener(callback: (Any, View?) -> Unit) {
        itemClick = callback
    }


    override fun getSelectableItemCount() = dirs.size
    override fun getIsItemSelectable(position: Int) = getItem(position).itemType == DIRECTORY_CONTENT
    override fun getItemSelectionKey(position: Int) = dirs.getOrNull(getContentPosition(position))?.path?.hashCode()
    override fun getItemKeyPosition(key: Int): Int {
        for (i in 0 until itemCount) {
            val directoryItem = getItem(i)
            if(directoryItem.itemType == DIRECTORY_CONTENT) {
                if(directoryItem.directory!!.path.hashCode() == key)
                    return i
            }
        }
        return -1
    }

    override fun getActionMenuId() = 0
    override fun prepareActionMode(menu: Menu) {}
    override fun actionItemPressed(id: Int) {}
    override fun onActionModeCreated() {}
    override fun onActionModeDestroyed() {}

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && holder.itemViewType == DIRECTORY_CONTENT) {
            Glide.with(activity).clear(holder.itemView.dir_thumbnail!!)
        }
    }

    override fun submitList(list: MutableList<DirectoryItem>?) {
        if(list != null){
            dirs.clear()
            list.forEach {
                if(it.itemType == DIRECTORY_CONTENT){
                    dirs.add(it.directory!!)
                }
            }
        }
        super.submitList(list)
    }

    //현재 위치까지에서 item type가 Content인 개수를 돌려준다
    private fun getContentPosition(position: Int): Int {
        var pos = 0
        for (i in 0..position)
        {
            if(getItem(i).itemType == DIRECTORY_CONTENT)
                pos ++
        }

        return pos - 1
    }

    //모든 항목들이 다 선택되였는가?
    override fun allSelected(): Boolean {
        var count = 0
        for (i in 0 until itemCount) {
            if(getItem(i).itemType == DIRECTORY_CONTENT)
                count ++
        }

        return getSelectedCounts() == count
    }

    private fun getSelectedItems() = selectedKeys.mapNotNull { getItemWithKey(it) } as ArrayList<Directory>
    fun getSelectedPaths() = getSelectedItems().map { it.path } as ArrayList<String>
    fun getFirstSelectedItem() = getItemWithKey(selectedKeys.first())
    private fun getItemWithKey(key: Int): Directory? = dirs.firstOrNull { it.path.hashCode() == key }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).itemType
    }

    private fun setupView(view: View, dirItem: DirectoryItem) {
        if(dirItem.itemType == DIRECTORY_HEADER){
            view.apply {
                folder_name.text = activity.getString(dirItem.directoryName)
                folder_cnt.text = activity.getString(R.string.media_count, dirItem.count)
            }
            return
        }

        val directory = dirItem.directory!!
        val isSelected = selectedKeys.contains(directory.path.hashCode())
        view.apply {
            dir_name.text = directory.name
            photo_cnt.text = directory.mediaCnt.toString()
            val thumbnailType = when {
                directory.tmb.isVideoFast() -> TYPE_VIDEOS
                directory.tmb.isGif() -> TYPE_GIFS
                directory.tmb.isRawFast() -> TYPE_RAWS
                directory.tmb.isSvg() -> TYPE_SVGS
                else -> TYPE_IMAGES
            }

            dir_check?.beVisibleIf(isSelected)

            dir_thumbnail.isList = true
            activity.loadImage(thumbnailType, directory.tmb, dir_thumbnail)

            val location = context.getPathLocation(directory.path)
            dir_location.beVisibleIf(location != LOCATION_INTERNAL)
            if (dir_location.isVisible()) {
                dir_location.setImageResource(if (location == LOCATION_SD) R.drawable.ic_sd_card_vector else R.drawable.ic_usb_vector)
            }
        }
    }

    /**
     * @see DiffUtil.ItemCallback
     * 2개의 [DirectoryItem]변수 값을 비교한다
     */
    class DiffCallback: DiffUtil.ItemCallback<DirectoryItem>(){
        override fun areItemsTheSame(oldItem: DirectoryItem, newItem: DirectoryItem): Boolean {
            //Header일때는 등록부이름을 비교한다.
            if(oldItem.itemType == DIRECTORY_HEADER)
                return newItem.itemType == DIRECTORY_HEADER && oldItem.directoryName == newItem.directoryName

            //Content일때는 등록부경로를 비교한다.
            return newItem.itemType == DIRECTORY_CONTENT && oldItem.directory!!.path == newItem.directory!!.path
        }

        override fun areContentsTheSame(oldItem: DirectoryItem, newItem: DirectoryItem): Boolean {
            //Header일때는 개수를 비교한다.
            if(oldItem.itemType == DIRECTORY_HEADER)
                return newItem.itemType == DIRECTORY_HEADER && oldItem.count == newItem.count

            //Content일때는 등록부이름, thumbnail경로, media개수를 비교한다.
            return newItem.itemType == DIRECTORY_CONTENT && oldItem.directory!!.name == newItem.directory!!.name &&
                    oldItem.directory!!.tmb == newItem.directory!!.tmb && oldItem.directory!!.mediaCnt == newItem.directory!!.mediaCnt
        }
    }
}
