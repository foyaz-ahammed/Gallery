package com.kr.gallery.pro.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.kr.gallery.pro.databases.GalleryDatabase
import com.kr.gallery.pro.models.HiddenDirectory

/**
 * 숨긴등록부목록을 자료로 하는 ViewModel
 * 숨긴등록부화면에서 리용한다.
 */
class HiddenMediaViewModel(application: Application): AndroidViewModel(application) {
    private val hiddenFolderDao = GalleryDatabase.getInstance(application).HiddenMediumDao()
    val hiddenDirectories: LiveData<List<HiddenDirectory>>

    init {
        hiddenDirectories = hiddenFolderDao.getHiddenDirectory()
    }
}
