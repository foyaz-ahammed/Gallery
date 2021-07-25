package com.kr.gallery.pro.adapters

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kr.gallery.pro.R
import com.kr.gallery.pro.models.FilterItem
import kotlinx.android.synthetic.main.editor_filter_rv_item.view.*
import org.wysaid.nativePort.CGENativeLibrary
import java.util.*

/**
 * Filter의 item들의 RecyclerView Adapter
 */
class FiltersAdapter(val context: Context, val filterItems: ArrayList<FilterItem>, val itemClick: (Int) -> Unit) : RecyclerView.Adapter<FiltersAdapter.ViewHolder>() {

    private var currentSelection = filterItems.first()
    private var currentPos = 0
    private var strokeBackground = ContextCompat.getDrawable(context, R.drawable.filter_selection_bg)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(filterItems[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.editor_filter_rv_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = filterItems.size

    fun getCurrentFilter() = currentSelection

    fun getCurrentFilterPos() = currentPos

    fun setCurrentFilter(position: Int) {
        val filterItem = filterItems.getOrNull(position) ?: return
        if (currentSelection != filterItem) {
            currentSelection = filterItem
            currentPos = position
            notifyDataSetChanged()
        }
        itemClick.invoke(position)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(filterItem: FilterItem): View {
            itemView.apply {
                if (getCurrentFilter() == filterItem) {
                    editor_filter_item_thumbnail.setImageDrawable(strokeBackground)
                    editor_filter_item_label_selected.text = context.getString(filterItem.filter.first)
                    editor_filter_item_label.text = ""
                } else {
                    editor_filter_item_thumbnail.setImageBitmap(filterItem.bitmap)
                    editor_filter_item_label_selected.text = ""
                    editor_filter_item_label.text = context.getString(filterItem.filter.first)
                }

                setOnClickListener {
                    setCurrentFilter(adapterPosition)
                }
            }
            return itemView
        }
    }

    /**
     * 파라메터로 들어온 Bitmap화상으로 filter item들의 thumb 갱신
     */
    fun updateFilterThumb(newThumb : Bitmap) {
        for (filterItem  in filterItems) {
            filterItem.bitmap.recycle()

            val ruleString = filterItem.filter.second
            val dstImage = CGENativeLibrary.filterImage_MultipleEffects(newThumb, ruleString, 1f)
            filterItem.bitmap = dstImage

        }

        setCurrentFilter(0)
    }
}
