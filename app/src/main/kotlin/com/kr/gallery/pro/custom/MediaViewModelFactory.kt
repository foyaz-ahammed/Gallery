package com.kr.gallery.pro.custom

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kr.gallery.pro.viewmodels.MediaViewModel

/**
 * MediaViewModel에 파라메터들을 추가하기 위하여 만든 ViewModelProvider클라스
 * @see MediaViewModel
 * @see com.kr.gallery.pro.activities.MediaActivity
 */
@Suppress("UNCHECKED_CAST")
class MediaViewModelFactory(private val mApplication: Application, private val mMediaDirectory: String, private val mShowHidden: Boolean,
                            private val mMediaDate: String, private val mMediaType: Int): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MediaViewModel(mApplication, mMediaDirectory, mShowHidden, mMediaDate, mMediaType) as T
    }
}
