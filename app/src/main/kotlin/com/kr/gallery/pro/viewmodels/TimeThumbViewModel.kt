package com.kr.gallery.pro.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.kr.gallery.pro.databases.GalleryDatabase
import com.kr.gallery.pro.models.TimeThumbnailItemJsonMedia

/**
 * 사진첩보기페지에서 날자별보기에 리용되는 view model
 * @see com.kr.gallery.pro.fragments.TimeFragment
 */
class TimeThumbViewModel(application: Application): AndroidViewModel(application) {
    val allTimeThumbItemsWithMedia: LiveData<List<TimeThumbnailItemJsonMedia>>

    init {
        val mediumDao = GalleryDatabase.getInstance(application).MediumDao()
        allTimeThumbItemsWithMedia = mediumDao.getAllThumbnailWithMedia()
    }
}
