package com.kr.gallery.pro.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.kr.gallery.pro.R
import com.kr.gallery.pro.helpers.KEY_EXPAND
import java.util.HashMap

class KrBottomSheet(context: Context?, attrs: AttributeSet?) : CoordinatorLayout(context!!, attrs) {
    var collapsedItemsContainer: ViewGroup? = null
    var expandedItemsContainer: ViewGroup? = null
    var itemMap = HashMap<String, View>()

    /**
     * @param context
     * @param key Identifying string
     * @param imgResId Image resource (R.drawable.ic_outline_delete_24)
     * @param strResId String resource
     * @param isRoom if true, show on expanded state, otherwise, show always
     */
    fun addItem(context: Context?, key: String?, imgResId: Int, strResId: Int, isRoom: Boolean) {
        if (!isRoom) { // Add normal items
            collapsedItemsContainer = findViewById(R.id.collapsedItemsContainer)
            val collapsedItem: View = LayoutInflater.from(context)
                .inflate(R.layout.bottom_dialog_item, collapsedItemsContainer, false)
            val textView = collapsedItem.findViewById<TextView>(R.id.collapsed_item_text)
            val imageView = collapsedItem.findViewById<ImageView>(R.id.collapsed_item_img)
            textView.setText(strResId)
            imageView.setImageResource(imgResId)
            itemMap[key!!] = collapsedItem
            collapsedItemsContainer?.addView(collapsedItem)
        } else { // Add collapsed items
            expandedItemsContainer = findViewById(R.id.expandedItemsContainer)
            val expandedItem: View = LayoutInflater.from(context)
                .inflate(R.layout.bottom_dialog_room_item, expandedItemsContainer, false)
            val textView = expandedItem.findViewById<TextView>(R.id.expanded_item_text)
            textView.setText(strResId)
            itemMap[key!!] = expandedItem
            expandedItemsContainer?.addView(expandedItem)
        }
    }

    //Change the title, and icon of the expand|collapse button on bottom sheet
    fun toggleExpand() {
        //Get text view, and image view
        val view = itemMap[KEY_EXPAND]!!
        val textView = view.findViewById<TextView>(R.id.collapsed_item_text)
        val imageView = view.findViewById<ImageView>(R.id.collapsed_item_img)

        //Check expanded state, and update title, icon
        if (textView.text == resources.getText(R.string.expand)) {
            textView.setText(R.string.collapse)
            imageView.setImageResource(R.drawable.ic_outline_arrow_downward_24)
        } else if (textView.text == resources.getText(R.string.collapse)) {
            textView.setText(R.string.expand)
            imageView.setImageResource(R.drawable.ic_outline_more_horiz_24)
        }
    }

    //Set Expand|Collapse button text, and image
    fun expand(expand: Boolean) {
        //Get text view, and image view
        val view = itemMap[KEY_EXPAND] ?: return
        val textView = view.findViewById<TextView>(R.id.collapsed_item_text)
        val imageView = view.findViewById<ImageView>(R.id.collapsed_item_img)
        if(expand) {
            textView.setText(R.string.collapse)
            imageView.setImageResource(R.drawable.ic_outline_arrow_downward_24)
        }
        else {
            textView.setText(R.string.expand)
            imageView.setImageResource(R.drawable.ic_outline_more_horiz_24)
        }
    }

    fun setInitCollapsed(view: View) { // Set init collapsed bottomsheet state
        val textView = view.findViewById<TextView>(R.id.collapsed_item_text)
        val imageView = view.findViewById<ImageView>(R.id.collapsed_item_img)

        textView.setText(R.string.expand)
        imageView.setImageResource(R.drawable.ic_outline_more_horiz_24)
    }
}
