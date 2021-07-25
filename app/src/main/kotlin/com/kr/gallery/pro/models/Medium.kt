package com.kr.gallery.pro.models

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bumptech.glide.signature.ObjectKey
import com.kr.commons.extensions.formatDate
import com.kr.commons.extensions.getFilenameExtension
import com.kr.gallery.pro.helpers.*
import java.io.Serializable
import java.util.*

@Entity(tableName = "media", indices = [(Index(value = ["full_path"], unique = true))])
data class Medium(
        @PrimaryKey(autoGenerate = true) var id: Long?,
        @ColumnInfo(name = "filename") var name: String,            //화일이름
        @ColumnInfo(name = "full_path") var path: String,           //화일경로
        @ColumnInfo(name = "parent_path") var parentPath: String,   //등록부경로
        @ColumnInfo(name = "last_modified") val modified: Long,     //수정시간
        @ColumnInfo(name = "date_taken") val dateTaken: Long,       //촬영시간
        @ColumnInfo(name = "date") val date: String,                //촬영날자
        @ColumnInfo(name = "size") val size: Long,                  //화일크기
        @ColumnInfo(name = "type") val type: Int,                   //media형태 1: 화상, 2: 동영상
        @ColumnInfo(name = "video_duration") val videoDuration: Int,    //동영상인 경우에 동영상길이
        @ColumnInfo(name = "video_time") val videoTime: Int)        //동영상인 경우에 동영상재생위치(동영상을 재생하다가 나오면 위치를 보관)
    : Serializable {

    companion object {
        private const val serialVersionUID = -6553149366975655L
    }

    fun isImage() = type == TYPE_IMAGES
    fun isVideo() = type == TYPE_VIDEOS

    /**
     * @return Scroll할때 scroller에 보여줄 text를 돌려준다
     */
    fun getBubbleText(context: Context, dateFormat: String, timeFormat: String) =
        dateTaken.formatDate(context, dateFormat, timeFormat)

    fun getSignature() = ObjectKey("$path-$modified-$size")

    /**
     * 동영상재생시간을 제외하고 나머지 성원변수들을 비교하여 같으면 두 [Medium]변수가 같은것으로 본다.
     */
    override fun equals(other: Any?): Boolean {
        if(other is Medium){
            return name == other.name && path == other.path && parentPath == other.parentPath &&
                    modified == other.modified && dateTaken == other.dateTaken && size == other.size && type == other.type && videoDuration == other.videoDuration
        }
        return super.equals(other)
    }
}
