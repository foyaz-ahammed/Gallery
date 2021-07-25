package com.kr.gallery.pro.custom

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kr.gallery.pro.viewmodels.ViewPagerViewModel

/**
 * ViewPagerViewModel에 파라메터들을 추가해주기 위하여 만든 ViewModelProvider클라스이다
 * @see ViewPagerViewModel
 * @see com.kr.gallery.pro.activities.ViewPagerActivity
 */
@Suppress("UNCHECKED_CAST")
class ViewPagerViewModelFactory(private val mApplication: Application, private val mDirectoryList: List<String>?, private val mShowHidden: Boolean): ViewModelProvider.Factory{
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ViewPagerViewModel(mApplication, mDirectoryList, mShowHidden) as T
    }
}
