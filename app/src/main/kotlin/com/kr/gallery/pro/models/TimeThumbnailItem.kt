package com.kr.gallery.pro.models

import org.json.JSONArray
import org.json.JSONException

/**
 * [TimeThumbnailItemJsonMedia] 변수를 입력으로 한다.
 * 기본 동작은 [TimeThumbnailItemJsonMedia.mediaEncryptJson]를 [mediaList]로 변환하는것이다.
 * @param data: 변환을 진행할 입력자료
 */
class TimeThumbnailItem(data: TimeThumbnailItemJsonMedia?) {
    //날자, 화상|동영상개수, media형태(화상|동영상)
    var date: String = ""
    var count: Int = 0
    var type: Int = 0

    //ThumbnailMediaItem 목록
    var mediaList = ArrayList<ThumbnailMediaItem>()

    //구성자
    init {
        if(data != null) {
            //date, count, type 는 그대로 리용한다.
            date = data.date
            count = data.count
            type = data.type

            val jsonMedia = data.mediaEncryptJson
            if (jsonMedia != null && jsonMedia.isNotEmpty()) {
                try {
                    //문자렬로부터 JSONArray를 생성한다.
                    val jsonMediaArray = JSONArray(jsonMedia)
                    for (i in 0 until jsonMediaArray.length()) {
                        //1개씩 순환하면서 ThumbnailMediaItem 객체를 생성하여 결과목록에 추가한다.
                        val jsonMediaObject = jsonMediaArray.getJSONObject(i)
                        var fullPath = "";
                        var fileName = "";
                        var dateTaken = 0L;

                        //화일경로, 화일이름, 촬영시간들을 얻어낸다.
                        try {
                            fullPath = jsonMediaObject.getString("full_path")
                        } catch (e: JSONException) {
                        }
                        try {
                            fileName = jsonMediaObject.getString("filename")
                        } catch (e: JSONException) {
                        }
                        try {
                            dateTaken = jsonMediaObject.getString("date_taken").toLong()
                        } catch (e: JSONException) {
                        }
                        mediaList.add(ThumbnailMediaItem(fullPath, fileName, dateTaken))
                    }

                    //촬영시간, 화일이름별로 정렬을 진행한다.
                    mediaList.sortWith(compareByDescending<ThumbnailMediaItem> { it.dateTaken }.thenBy { it.fileName })
                } catch (e: JSONException) {
                }
            }
        }
    }

    /**
     * 꼭같은 객체를 생성하여 돌려준다.
     */
    fun clone(): TimeThumbnailItem {
        val item = TimeThumbnailItem(null)
        item.let {
            it.date = date
            it.type = type
            it.count = count
            it.mediaList = ArrayList(mediaList)
        }
        return item
    }

    /**
     * 날자별보기에 필요한 화일정보들을 담고 있는 클라스
     * @param fullPath: 화일경로
     * @param fileName: 화일이름
     * @param dateTaken: 촬영시간
     */
    open class ThumbnailMediaItem(val fullPath: String, val fileName: String, val dateTaken: Long) {
        override fun equals(other: Any?): Boolean {
            if(other is ThumbnailMediaItem) {
                return fullPath == other.fullPath && fileName == other.fileName && dateTaken == other.dateTaken
            }
            return false
        }

        override fun hashCode(): Int {
            var result = fullPath.hashCode()
            result = 31 * result + fileName.hashCode()
            result = 31 * result + dateTaken.hashCode()
            return result
        }
    }
}

/**
 * @return 꼭같은 [TimeThumbnailItem]배렬객체를 돌려준다.
 */
fun List<TimeThumbnailItem>.cloneList(): List<TimeThumbnailItem> {
    val clone: MutableList<TimeThumbnailItem> = ArrayList(size)
    for (item in this) clone.add(item.clone())
    return clone
}
