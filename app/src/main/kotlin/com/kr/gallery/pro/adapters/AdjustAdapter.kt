package com.kr.gallery.pro.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kr.gallery.pro.R
import com.kr.gallery.pro.models.AdjustConfig
import kotlinx.android.synthetic.main.editor_adjust_rv_item.view.*
import java.util.*

/**
 * Adjust의 항목들의 RecyclerView Adapter
 */
class AdjustAdapter(val context: Context, val adjustItems: ArrayList<AdjustConfig>, val itemClick: (Int) -> Unit) : RecyclerView.Adapter<AdjustAdapter.ViewHolder>() {

    private var currentSelection = adjustItems.first()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(adjustItems[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.editor_adjust_rv_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = adjustItems.size

    fun getCurrentAdjustItem() = currentSelection

    fun getAdjustItemByIndex(index: Int) : AdjustConfig {
        return adjustItems[index]
    }

    private fun setAdjustByPositionIndex(position: Int) {
        val adjustItem = adjustItems.getOrNull(position) ?: return
        if (currentSelection != adjustItem) {
            currentSelection = adjustItem
            notifyDataSetChanged()
        }
        itemClick.invoke(position)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(adjustItem: AdjustConfig): View {
            itemView.apply {
                editor_adjust_item_label.text = resources.getString(adjustItem.mNameResID)

                val icon: Drawable = ContextCompat.getDrawable(context, adjustItem.mIconResID)!!
                icon.setBounds(0, 0, 70, 70)
                editor_adjust_item_label.setCompoundDrawables(null, icon, null, null)

                editor_adjust_item_label.isSelected = getCurrentAdjustItem() == adjustItem

                setOnClickListener {
                    setAdjustByPositionIndex(adapterPosition)
                }
            }
            return itemView
        }
    }

    fun selectItem(pos: Int) {
        setAdjustByPositionIndex(pos)
    }

    fun resetItems() {
        for (item in adjustItems) {
            item.slierIntensity = 0.5f
        }
    }
}
