package com.kr.gallery.pro.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.kr.gallery.pro.databases.GalleryDatabase
import com.kr.gallery.pro.models.Medium

/**
 * Cover page에 현시될 화상자료들을 얻기 위한 view model
 * @see com.kr.gallery.pro.adapters.TimeFolderAdapter.CoverViewHolder
 */
class CoverPageViewModel(application: Application): AndroidViewModel(application) {
    val allCoverPageItems: LiveData<List<Medium>>

    init {
        val coverPageDao = GalleryDatabase.getInstance(application).CoverPageDao()
        allCoverPageItems = coverPageDao.getAllMedia()
    }
}
