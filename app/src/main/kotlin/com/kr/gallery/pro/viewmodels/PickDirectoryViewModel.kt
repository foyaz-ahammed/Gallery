package com.kr.gallery.pro.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.kr.gallery.pro.databases.GalleryDatabase
import com.kr.gallery.pro.models.Directory

/**
 * 화상선택화면에서 리용하는 ViewModel
 */
class PickDirectoryViewModel(application: Application): AndroidViewModel(application) {
    //Media table에서 화상만을 filter하여 등록부자료를 추출한다
    val imageDirectories: LiveData<List<Directory>>

    init {
        val mediumDao = GalleryDatabase.getInstance(application).MediumDao()
        imageDirectories = mediumDao.getImageDirectories()
    }
}
