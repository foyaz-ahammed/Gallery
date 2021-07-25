package com.kr.gallery.pro.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.kr.gallery.pro.databases.GalleryDatabase
import com.kr.gallery.pro.models.Medium
import com.kr.gallery.pro.repositories.MediaRepository

/**
 * 사진첩보기화면, 모두보기화면, 숨김등록부화면에서 등록부|날자에 해당한 media들을 얻기 위해 리용하는 viewmodel
 * @param mediaDirectory 등록부경로
 * @param showHidden 숨긴등록부인가?
 * @param mediaDate 날자
 * @param mediaType media 형태. 1: 화상, 2: 동영상
 *
 * @see com.kr.gallery.pro.fragments.AllFragment
 * @see com.kr.gallery.pro.fragments.TimeFragment
 * @see com.kr.gallery.pro.activities.HiddenFoldersActivity
 */
class MediaViewModel(application: Application, mediaDirectory: String, showHidden: Boolean,
                     mediaDate: String, mediaType: Int): AndroidViewModel(application) {
    private val repository: MediaRepository
    val mediaFiltered: LiveData<List<Medium>>

    init {
        val mediaDao = GalleryDatabase.getInstance(application).MediumDao()
        repository = MediaRepository(mediaDao, mediaDirectory, showHidden, mediaDate, mediaType)
        mediaFiltered = repository.mediaFiltered
    }
}
