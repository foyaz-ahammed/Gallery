package com.kr.gallery.pro.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.kr.gallery.pro.databases.GalleryDatabase
import com.kr.gallery.pro.models.Medium

/**
 * 화상보기화면에서 리용하는 view model
 * @param application
 * @param directoryList: 등록부목록
 * @param showHidden: 숨긴등록부인가?
 *
 * @see com.kr.gallery.pro.activities.ViewPagerActivity
 */
class ViewPagerViewModel(application: Application, directoryList: List<String>?, showHidden: Boolean): AndroidViewModel(application) {
    val mediaToShow: LiveData<List<Medium>>

    init {
        val mediumDao = GalleryDatabase.getInstance(application).MediumDao()
        mediaToShow =
                //전체 media얻기
                if(directoryList.isNullOrEmpty()){
                    mediumDao.getAllLiveMedia()
                }

                //등록부들에 해당한 media들 얻기
                else {
                    if(showHidden)
                        mediumDao.getAllLiveMediaHiddenByDirectory(directoryList[0])
                    else
                        mediumDao.getAllLiveMediaByDirectories(directoryList)
                }
    }
}
