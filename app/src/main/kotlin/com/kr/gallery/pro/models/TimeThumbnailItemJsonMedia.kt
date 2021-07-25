package com.kr.gallery.pro.models

/**
 * Table에서 1차로 얻은 날자별로 가른 목록자료이다.
 * 날자, media개수, media형태, 그리고 media목록을 String자료로 가지고 있는다.
 * [TimeThumbnailItem]으로 변환하여 UI에 반영한다.
 */
class TimeThumbnailItemJsonMedia {
    //날자, media개수, media형태
    var date: String = ""
    var count: Int = 0
    var type: Int = 0

    //Media목록을 Json형태로 표시한 문자렬이다.
    var mediaEncryptJson: String? = null
}
