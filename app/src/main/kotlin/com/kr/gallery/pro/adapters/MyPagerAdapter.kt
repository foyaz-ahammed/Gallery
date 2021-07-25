package com.kr.gallery.pro.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.adapter.FragmentViewHolder
import com.kr.gallery.pro.activities.ViewPagerActivity
import com.kr.gallery.pro.fragments.PhotoFragment
import com.kr.gallery.pro.fragments.VideoThumbFragment
import com.kr.gallery.pro.fragments.ViewPagerFragment
import com.kr.gallery.pro.helpers.MEDIUM
import com.kr.gallery.pro.helpers.SHOULD_INIT_FRAGMENT
import com.kr.gallery.pro.models.Medium

/**
 * 화상보기화면[ViewPagerActivity]의 ViewPager에서 리용하는 adapter
 */
class MyPagerAdapter(val activity: ViewPagerActivity, private val mStartTransitionKey: String) : FragmentStateAdapter(activity) {
    //화상목록
    var media = ArrayList<Medium>()

    //자식 fragment들을 보관해둔다.
    var shouldInitFragment = true

    override fun getItemCount() = media.size

    override fun getItemId(position: Int): Long {
        return if (position > -1 && position < media.size) media[position].path.hashCode().toLong()
        else -1
    }

    override fun containsItem(itemId: Long): Boolean {
        return media.any { it.path.hashCode().toLong() == itemId }
    }

    /**
     * fragment를 창조한다
     * @param position: 위치
     * 화상: [PhotoFragment]
     * 동영상: [VideoThumbFragment]
     */
    override fun createFragment(position: Int): Fragment {
        //Bundle정보 만들기
        val medium = media[position]
        val bundle = Bundle()
        bundle.putSerializable(MEDIUM, medium)
        bundle.putBoolean(SHOULD_INIT_FRAGMENT, shouldInitFragment)

        //fragment창조
        val fragment = if (medium.isVideo()) {
            VideoThumbFragment(mStartTransitionKey)
        } else {
            PhotoFragment(mStartTransitionKey)
        }
        fragment.arguments = bundle

        return fragment
    }

    fun getMedium(position: Int): Medium {
        return media[position]
    }

    /**
     * 화상목록을 설정하고 adapter갱신을 진행한다
     * @param newMedia: 화상목록
     */
    fun setItems(newMedia: List<Medium>) {
        val callback = MediumDiffUtil(media.clone() as ArrayList<Medium>, newMedia)
        val diff = DiffUtil.calculateDiff(callback)

        media.clear()
        media.addAll(newMedia)

        diff.dispatchUpdatesTo(this)
    }

    /**
     * @see DiffUtil.Callback
     * 꼭같은 자료가 들어왔을때 불필요하게 view를 refresh시키는 동작을 없애기 위하여 Diff Callback을 적용한다.
     */
    open class MediumDiffUtil(private val oldList: List<Medium>, private val newList: List<Medium>) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].path == newList[newItemPosition].path
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem == newItem
        }
    }
}
