package com.kr.gallery.pro.adapters

import androidx.recyclerview.widget.*

/**
 * Header view를 가진 Listadapter
 * @see ListAdapter
 * @see TimeFolderAdapter
 */
abstract class ListAdapterWithHeader<T, VH: RecyclerView.ViewHolder>(
        private val diffCallback: DiffUtil.ItemCallback<T>,
        private val headerOffset: Int = 1
): RecyclerView.Adapter<VH>() {
    private val mHelper by lazy {
        AsyncListDiffer<T> (
                OffsetListUpdateCallback(this, headerOffset),
                AsyncDifferConfig.Builder<T>(diffCallback).build()
        )
    }

    fun submitList(list: List<T>?) {
        mHelper.submitList(list)
    }

    fun getItem(position: Int): T {
        return mHelper.currentList[position - headerOffset]
    }

    override fun getItemCount(): Int {
        return mHelper.currentList.size + headerOffset
    }

    fun getItemPosition(originalPosition: Int): Int {
        return originalPosition - headerOffset
    }

    private class OffsetListUpdateCallback (
            private val adapter: RecyclerView.Adapter<*>,
            private val offset: Int
    ): ListUpdateCallback {

        fun offsetPosition(originalPosition: Int): Int {
            return originalPosition + offset
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            adapter.notifyItemRangeChanged(offsetPosition(position), count, payload)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            adapter.notifyItemMoved(offsetPosition(fromPosition), offsetPosition(toPosition))
        }

        override fun onInserted(position: Int, count: Int) {
            adapter.notifyItemRangeInserted(offsetPosition(position), count)
        }

        override fun onRemoved(position: Int, count: Int) {
            adapter.notifyItemRangeRemoved(offsetPosition(position), count)
        }
    }
}
