package com.kr.gallery.pro.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kr.gallery.pro.fragments.AllFragment
import com.kr.gallery.pro.fragments.TimeFragment

/**
 * 기본화면의 tab viewpager에서 리용되는 adapter
 */
class ScreenSlidePagerAdapter(fm: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fm, lifecycle) {

    //첫 페지: 사진첩보기화면
    //둘째 페지: 모두보기화면
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> TimeFragment()
            1 -> AllFragment()
            else -> null!!
        }
    }

    override fun getItemCount(): Int {
        return 2
    }
}
