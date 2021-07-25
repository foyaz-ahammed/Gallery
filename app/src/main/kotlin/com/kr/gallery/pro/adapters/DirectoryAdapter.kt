package com.kr.gallery.pro.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.kr.commons.activities.BaseSimpleActivity
import com.kr.commons.adapters.MyRecyclerViewAdapter
import com.kr.commons.extensions.*
import com.kr.commons.views.FastScroller
import com.kr.commons.views.MyRecyclerView
import com.kr.gallery.pro.R
import com.kr.gallery.pro.extensions.*
import com.kr.gallery.pro.helpers.*
import com.kr.gallery.pro.models.Directory
import kotlinx.android.synthetic.main.directory_item_list.view.*

/**
 * @see [com.kr.gallery.pro.fragments.PickDirectoryDialogFragment]
 * 이동화면의 recyclerview에서 리용하는 adapter
 *
 * @see DirectoryListAdapter
 * @see DirectoryGridAdapter
 * @see HiddenDirectoryListAdapter
 */
class DirectoryAdapter(activity: BaseSimpleActivity, var dirs: ArrayList<Directory>, recyclerView: MyRecyclerView,
                       val isPickIntent: Boolean, fastScroller: FastScroller? = null, itemClick: ((Any) -> Unit)?) :
        MyRecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick) {

    init {
        setupDragListener(true)
    }

    override fun getItemCount() = dirs.size

    override fun getSelectableItemCount() = dirs.size
    override fun getIsItemSelectable(position: Int) = true
    override fun getItemSelectionKey(position: Int) = dirs.getOrNull(position)?.path?.hashCode()
    override fun getItemKeyPosition(key: Int) = dirs.indexOfFirst { it.path.hashCode() == key }

    override fun getActionMenuId() = 0
    override fun prepareActionMode(menu: Menu) {}
    override fun actionItemPressed(id: Int) {}
    override fun onActionModeCreated() {}
    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutType = R.layout.directory_item_list
        return createViewHolder(layoutType, parent)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val dir = dirs.getOrNull(position) ?: return
        holder.bindView(dir, true, !isPickIntent) { itemView, adapterPosition ->
            setupView(itemView, dir)
        }
        bindViewHolder(holder)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        if (!activity.isDestroyed) {
            Glide.with(activity).clear(holder.itemView.dir_thumbnail!!)
        }
    }

    private fun setupView(view: View, directory: Directory) {
        val isSelected = selectedKeys.contains(directory.path.hashCode())
        view.apply {
            //등록부이름, 화상개수를 현시
            dir_name.text = directory.name
            photo_cnt.text = directory.mediaCnt.toString()

            //등록부화상 설정
            val thumbnailType = when {
                directory.tmb.isVideoFast() -> TYPE_VIDEOS
                directory.tmb.isGif() -> TYPE_GIFS
                directory.tmb.isRawFast() -> TYPE_RAWS
                directory.tmb.isSvg() -> TYPE_SVGS
                else -> TYPE_IMAGES
            }
            activity.loadImage(thumbnailType, directory.tmb, dir_thumbnail)

            //선택되였으면 푸른색 mask 화상을 보여준다
            dir_check?.beVisibleIf(isSelected)

            dir_thumbnail.isList = true

            //등록부아이콘 설정
            val location = context.getPathLocation(directory.path)
            dir_location.beVisibleIf(location != LOCATION_INTERNAL)
            if (dir_location.isVisible()) {
                dir_location.setImageResource(if (location == LOCATION_SD) R.drawable.ic_sd_card_vector else R.drawable.ic_usb_vector)
            }
        }
    }
}
