package com.kr.gallery.pro.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.kr.gallery.pro.R
import com.kr.gallery.pro.models.CropSelectItem
import kotlinx.android.synthetic.main.crop_select_rv_item.view.*
import java.util.*


class CropSelectorRVAdapter(val context: Context, val cropItems: ArrayList<CropSelectItem>, val itemClick: (Int, Boolean) -> Unit) : RecyclerView.Adapter<CropSelectorRVAdapter.ViewHolder>() {

    private var currentSelection = cropItems.first()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(cropItems[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.crop_select_rv_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = cropItems.size

    fun getCurrentCrop() = currentSelection

    private fun setCurrentCrop(position: Int, animate: Boolean) {
        val cropItem = cropItems.getOrNull(position) ?: return
        if (currentSelection != cropItem) {
            currentSelection = cropItem
            notifyDataSetChanged()
            itemClick.invoke(position, animate)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(cropItem: CropSelectItem): View {
            itemView.apply {
                val icon: Drawable = ContextCompat.getDrawable(context, cropItem.iconResId)!!
                if (getCurrentCrop() == cropItem) {
                    DrawableCompat.setTint(icon, resources.getColor(R.color.text_selected_color, null))
                } else {
                    DrawableCompat.setTint(icon, Color.WHITE)
                }
                crop_select_item.setImageDrawable(icon)
                setOnClickListener {
                    setCurrentCrop(adapterPosition, true)
                }
            }
            return itemView
        }
    }

    fun selectItem(pos: Int, animate: Boolean) {
        setCurrentCrop(pos, animate)
    }
}
