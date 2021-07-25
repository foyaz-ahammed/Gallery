package com.kr.gallery.pro.repositories

import androidx.lifecycle.LiveData
import com.kr.gallery.pro.helpers.TYPE_IMAGES
import com.kr.gallery.pro.helpers.TYPE_VIDEOS
import com.kr.gallery.pro.interfaces.MediumDao
import com.kr.gallery.pro.models.Medium

/**
 * 날자나 등록부에 해당한 media들을 얻기위한 repository이다.
 * [com.kr.gallery.pro.viewmodels.MediaViewModel]에서 리용된다.
 *
 * @param mediaDao: MediumDao instance
 * @param mediaDirectory: 등록부경로, 날자에 해당한 media들을 얻을때는 이 파라메터가 ""(빈 문자렬)
 * @param showHidden: 숨긴등록부인가? 숨김등록부화면에서 media들을 얻을때 이 파라메터에 true가 들어온다.
 * @param mediaDate: 날자("2020-1-1"), 둥록부의 media들을 얻을때는 이 파라메터가 ""(빈 문자렬)
 * @param mediaType: Media 형태. 1: 화상, 2: 동영상
 */
class MediaRepository(mediaDao: MediumDao, mediaDirectory: String, showHidden: Boolean, mediaDate: String, mediaType: Int) {
    val mediaFiltered: LiveData<List<Medium>> =
            if(mediaDirectory.isNotEmpty()) {
                if(!showHidden) {
                    when (mediaType) {
                        //등록부의 화상들을 돌려준다.
                        TYPE_IMAGES -> {
                            mediaDao.getImagesByDirectory(mediaDirectory)
                        }

                        //등록부의 동영상들을 돌려준다.
                        TYPE_VIDEOS -> {
                            mediaDao.getVideosByDirectory(mediaDirectory)
                        }

                        //등록부의 화상, 동영상들을 모두 돌려준다.
                        else -> mediaDao.getMediaByDirectory(mediaDirectory)
                    }
                }
                //숨긴 등록부의 media들을 돌려준다.
                else
                    mediaDao.getHiddenMediaByDirectory(mediaDirectory)
            }

            //날자에 해당한 media들을 돌려준다.
            else{
                mediaDao.getMediaLiveDataByDate(mediaDate, mediaType)
            }
}
