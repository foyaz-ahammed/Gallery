package com.kr.gallery.pro.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.kr.gallery.pro.databases.GalleryDatabase
import com.kr.gallery.pro.models.Directory

/**
 * '등록부목록'을 자료로 하는 ViewModel
 * 모두보기화면과 '사진첩에 그림추가'화면들에서 리용한다.
 */
class DirectoryViewModel(application: Application): AndroidViewModel(application) {
    val allDirectories: LiveData<List<Directory>>

    init {
        val directoryDao = GalleryDatabase.getInstance(application).DirectoryDao()
        allDirectories = directoryDao.getAllDirectories()
    }
}
