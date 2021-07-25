package com.kr.gallery.pro.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kr.commons.extensions.*
import com.kr.commons.helpers.REQUEST_PICK_IMAGE
import com.kr.commons.helpers.REQUEST_SET_WALLPAPER
import com.kr.gallery.pro.R
import com.kr.gallery.pro.activities.MediaActivity
import com.kr.gallery.pro.extensions.getPathLocation
import com.kr.gallery.pro.extensions.loadImage
import com.kr.gallery.pro.helpers.*
import com.kr.gallery.pro.models.Directory
import kotlinx.android.synthetic.main.directory_item_list.view.*

/**
 * [com.kr.gallery.pro.activities.ImagePickActivity]
 * 화상선택화면의 recyclerview에서 리용하는 adapter
 */
class PickDirectoryAdapter(context: Context, private val mIsGetImageIntent: Boolean, private val mIsSetWallpaperIntent: Boolean)
    : ListAdapter<Directory, PickDirectoryAdapter.ViewHolder>(DiffCallback()) {
    private val layoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = layoutInflater.inflate(R.layout.directory_item_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val directory = getItem(position)

        //Directory정보(이름, 화상, 개수...)를 view에 현시해준다
        val view = holder.itemView
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
            context.loadImage(thumbnailType, directory.tmb, dir_thumbnail)
            val location = context.getPathLocation(directory.path)
            dir_location.beVisibleIf(location != LOCATION_INTERNAL)
            if (dir_location.isVisible()) {
                dir_location.setImageResource(if (location == LOCATION_SD) R.drawable.ic_sd_card_vector else R.drawable.ic_usb_vector)
            }

            //Click했을때 처리
            setOnClickListener {
                context.let {
                    it as Activity
                    Intent(it, MediaActivity::class.java).apply {
                        addFlags(FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra(DIRECTORY, directory.path)
                        putExtra(DATE, "")
                        putExtra(MEDIA_TYPE, TYPE_IMAGES)
                        putExtra(GET_IMAGE_INTENT, mIsGetImageIntent)
                        putExtra(SET_WALLPAPER_INTENT, mIsSetWallpaperIntent)

                        when {
                            mIsGetImageIntent -> it.startActivityForResult(this, REQUEST_PICK_IMAGE)
                            mIsSetWallpaperIntent -> it.startActivityForResult(this, REQUEST_SET_WALLPAPER)
                            else -> it.startActivity(this)
                        }
                    }
                }
            }
        }
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    class DiffCallback: DiffUtil.ItemCallback<Directory>(){
        override fun areItemsTheSame(oldItem: Directory, newItem: Directory): Boolean
            = oldItem.path == newItem.path

        override fun areContentsTheSame(oldItem: Directory, newItem: Directory): Boolean
            = oldItem.name == newItem.name && oldItem.tmb == newItem.tmb && oldItem.mediaCnt == newItem.mediaCnt
    }
}
