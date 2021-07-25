package com.kr.commons.adapters

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kr.commons.R
import com.kr.commons.activities.BaseSimpleActivity
import com.kr.commons.extensions.baseConfig
import com.kr.commons.helpers.VIEW_HOLDER_TAG
import com.kr.commons.interfaces.MyActionModeCallback
import com.kr.commons.views.FastScroller
import com.kr.commons.views.MyRecyclerView
import java.util.*
import kotlin.collections.LinkedHashSet

/**
 * RecyclerView adapter들에서 공통적으로 리용하는 기초클라스이다.
 */
abstract class MyListAdapter<T, DIFF_CALLBACK: DiffUtil.ItemCallback<T>>(
        val activity: BaseSimpleActivity, val recyclerView: MyRecyclerView, fastScroller: FastScroller? = null, var itemClick: ((Any, View?) -> Unit)?,
        diffCallback: DIFF_CALLBACK)
        : ListAdapter<T, MyListAdapter<T, DIFF_CALLBACK>.ViewHolder>(diffCallback) {

    protected val baseConfig = activity.baseConfig
    protected val resources = activity.resources!!
    protected val layoutInflater = activity.layoutInflater
    protected var textColor = baseConfig.textColor
    protected var actModeCallback: MyActionModeCallback
    var selectedKeys = LinkedHashSet<Int>()
    protected var positionOffset = 0

    private var actMode: ActionMode? = null
    private var actBarTextView: TextView? = null
    private var lastLongPressedItem = -1

    private var mSwipeDirectListener: MyRecyclerView.OnSwipeDirectListener? = null

    abstract fun getActionMenuId(): Int

    abstract fun prepareActionMode(menu: Menu)

    abstract fun actionItemPressed(id: Int)

    abstract fun getSelectableItemCount(): Int

    abstract fun getIsItemSelectable(position: Int): Boolean

    abstract fun getItemSelectionKey(position: Int): Int?

    abstract fun getItemKeyPosition(key: Int): Int

    abstract fun onActionModeCreated()

    abstract fun onActionModeDestroyed()

    protected fun isOneItemSelected() = selectedKeys.size == 1

    init {
        mSwipeDirectListener = object : MyRecyclerView.OnSwipeDirectListener {
            override fun onSwipeDirect() {
                activity.onSwipeDown()
            }
        }

        recyclerView.setOnSwipeDirectListener(mSwipeDirectListener)

        fastScroller?.resetScrollPositions()

        actModeCallback = object : MyActionModeCallback() {
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                return true
            }

            override fun onCreateActionMode(actionMode: ActionMode, menu: Menu?): Boolean {
                if (getActionMenuId() == 0) {
                    return true
                }

                isSelectable = true
                actMode = actionMode
                onActionModeCreated()
                return true
            }

            override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                prepareActionMode(menu)
                return true
            }

            override fun onDestroyActionMode(actionMode: ActionMode) {
                isSelectable = false
                (selectedKeys.clone() as HashSet<Int>).forEach {
                    val position = getItemKeyPosition(it)
                    if (position != -1) {
                        toggleItemSelection(false, position, false)
                    }
                }
                updateTitle()
                selectedKeys.clear()
                actMode = null
                lastLongPressedItem = -1
                onActionModeDestroyed()
            }
        }
    }

    protected fun toggleItemSelection(select: Boolean, pos: Int, updateTitle: Boolean = true) {
        if (!getIsItemSelectable(pos)) {
            return
        }

        val itemKey = getItemSelectionKey(pos) ?: return
        if ((select && selectedKeys.contains(itemKey)) || (!select && !selectedKeys.contains(itemKey))) {
            return
        }

        if (select) {
            selectedKeys.add(itemKey)
        } else {
            selectedKeys.remove(itemKey)
        }

        notifyItemChanged(pos + positionOffset)

        if (updateTitle) {
            updateTitle()
        }

        if (selectedKeys.isEmpty()) {
            finishActMode()
        }
    }

    fun getSelectedCounts(): Int {
        val selectableItemCount = getSelectableItemCount()
        return selectedKeys.size.coerceAtMost(selectableItemCount)
    }

    fun setSelectable(selectable: Boolean) {
        actModeCallback.isSelectable = selectable
    }

    open fun updateTitle() {
        val selectableItemCount = getSelectableItemCount()
        val selectedCount = selectedKeys.size.coerceAtMost(selectableItemCount)

        activity.updateSelectedCounts(selectedCount, allSelected())

        activity.actionBar?.hide()
        if(selectedCount > 0) {
            activity.showBottomSheet()
        } else {
            activity.showBottomShareBar(false)
            activity.hideBottomSheet()
            actModeCallback.isSelectable = false
        }
    }

    fun itemLongClicked(position: Int) {
        recyclerView.setDragSelectActive(position)
        lastLongPressedItem = if (lastLongPressedItem == -1) {
            position
        } else {
            val min = Math.min(lastLongPressedItem, position)
            val max = Math.max(lastLongPressedItem, position)
            for (i in min..max) {
                toggleItemSelection(true, i, false)
            }
            updateTitle()
            position
        }
    }

    open fun selectAll() {
        val cnt = itemCount - positionOffset
        for (i in 0 until cnt) {
            toggleItemSelection(true, i, true)
        }
        lastLongPressedItem = -1
        updateTitle()
    }

    open fun deselectAll() {
        val cnt = itemCount - positionOffset
        for (i in 0 until cnt) {
            toggleItemSelection(false, i, false)
        }
        lastLongPressedItem = -1
        updateTitle()
    }

    open fun allSelected(): Boolean {
        return getSelectedCounts() == itemCount
    }

    open fun clearSelection(){
        //선택을 해제한다.
        val tempSelKeys = selectedKeys.clone() as LinkedHashSet<Int>
        selectedKeys.clear()
        tempSelKeys.forEach {
            val position = getItemKeyPosition(it)
            if(position != -1) {
                notifyItemChanged(position + positionOffset)
            }
        }

        lastLongPressedItem = -1
        actModeCallback.isSelectable = false
    }

    protected fun setupDragListener(enable: Boolean) {
        if (enable) {
            recyclerView.setupDragListener(object : MyRecyclerView.MyDragListener {
                override fun selectItem(position: Int) {
                    toggleItemSelection(true, position, true)
                }

                override fun selectRange(initialSelection: Int, lastDraggedIndex: Int, minReached: Int, maxReached: Int) {
                    selectItemRange(initialSelection, Math.max(0, lastDraggedIndex - positionOffset), Math.max(0, minReached - positionOffset), maxReached - positionOffset)
                    if (minReached != maxReached) {
                        lastLongPressedItem = -1
                    }
                }
            })
        } else {
            recyclerView.setupDragListener(null)
        }
    }

    protected fun selectItemRange(from: Int, to: Int, min: Int, max: Int) {
        if (from == to) {
            (min..max).filter { it != from }.forEach { toggleItemSelection(false, it, true) }
            return
        }

        if (to < from) {
            for (i in to..from) {
                toggleItemSelection(true, i, true)
            }

            if (min > -1 && min < to) {
                (min until to).filter { it != from }.forEach { toggleItemSelection(false, it, true) }
            }

            if (max > -1) {
                for (i in from + 1..max) {
                    toggleItemSelection(false, i, true)
                }
            }
        } else {
            for (i in from..to) {
                toggleItemSelection(true, i, true)
            }

            if (max > -1 && max > to) {
                (to + 1..max).filter { it != from }.forEach { toggleItemSelection(false, it, true) }
            }

            if (min > -1) {
                for (i in min until from) {
                    toggleItemSelection(false, i, true)
                }
            }
        }
    }

    fun setupZoomListener(zoomListener: MyRecyclerView.MyZoomListener?) {
        recyclerView.setupZoomListener(zoomListener)
    }

    fun addVerticalDividers(add: Boolean) {
        if (recyclerView.itemDecorationCount > 0) {
            recyclerView.removeItemDecorationAt(0)
        }

        if (add) {
            DividerItemDecoration(activity, DividerItemDecoration.VERTICAL).apply {
                setDrawable(resources.getDrawable(R.drawable.divider, null))
                recyclerView.addItemDecoration(this)
            }
        }
    }

    fun finishActMode() {
        actMode?.finish()
    }

    protected fun createViewHolder(layoutType: Int, parent: ViewGroup?): ViewHolder {
        val view = layoutInflater.inflate(layoutType, parent, false)
        return ViewHolder(view)
    }

    /**
     * item view로부터 ViewHolder를 얻기 위해 tag로서 ViewHolder를 설정해준다
     * @param holder
     */
    protected fun bindViewHolder(holder: ViewHolder) {
        holder.itemView.setTag(VIEW_HOLDER_TAG, holder)
    }

    protected fun removeSelectedItems(positions: ArrayList<Int>) {
        positions.forEach {
            notifyItemRemoved(it)
        }
        finishActMode()
    }

    open inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(any: Any, transitionView: View?, directSet: Boolean, callback: (itemView: View) -> Unit): View {
            return itemView.apply {
                callback(this)

                if (directSet) {
                    transitionView?.setOnClickListener {
                        viewClicked(any, transitionView)
                    }
                    transitionView?.setOnLongClickListener{
                        viewLongClicked()
                        true
                    }
                } else {
                    setOnClickListener { viewClicked(any, transitionView) }
                    setOnLongClickListener { viewLongClicked(); true }
                }
            }
        }

        private fun viewClicked(any: Any, transitionView: View?) {
            if (actModeCallback.isSelectable || itemClick == null) {
                val currentPosition = adapterPosition - positionOffset
                val isSelected = selectedKeys.contains(getItemSelectionKey(currentPosition))
                toggleItemSelection(!isSelected, currentPosition, true)
            } else {
                itemClick!!.invoke(any, transitionView)
            }
            lastLongPressedItem = -1
        }

        private fun viewLongClicked() {
            val currentPosition = adapterPosition - positionOffset
            actModeCallback.isSelectable = true
            toggleItemSelection(true, currentPosition, true)
            itemLongClicked(currentPosition)
        }
    }
}
