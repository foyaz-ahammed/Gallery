package com.kr.gallery.pro.models

import android.content.Context
import androidx.room.*
import com.kr.commons.extensions.formatDate

@Entity(tableName = "directories", indices = [Index(value = ["path"], unique = true)])
data class Directory(
        @PrimaryKey(autoGenerate = true) var id: Long?,
        @ColumnInfo(name = "path") var path: String,            //등록부경로
        @ColumnInfo(name = "thumbnail") var tmb: String,        //Thumbnail경로
        @ColumnInfo(name = "path_name") var name: String,       //등록부이름
        @ColumnInfo(name = "media_count") var mediaCnt: Int,    //media개수
        @ColumnInfo(name = "last_modified") var modified: Long, //수정시간(media들의 수정시간의 최대값)
        @ColumnInfo(name = "date_taken") var dateTaken: Long,   //촬영시간(media들의 촬영시간의 최대값)
        @ColumnInfo(name = "size") var size: Long              //media총크기(media들의 크기총합)
) {
    constructor() : this(null, "", "", "", 0, 0L, 0L, 0)

    /**
     * @return 이동화면에서 scroller에 현시될 text
     */
    fun getBubbleText(context: Context, dateFormat: String? = null, timeFormat: String? = null)
            = modified.formatDate(context, dateFormat, timeFormat)

    /**
     * 다른 객체와 비교
     * @return 같으면 true, 다르면 false
     */
    override fun equals(other: Any?): Boolean {
        if(other is Directory){
            //Directory형일때 성원변수들을 모두 비교한다.
            return path == other.path && tmb == other.tmb && name == other.name && mediaCnt == other.mediaCnt &&
                    modified == other.modified && dateTaken == other.dateTaken && size == other.size
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + path.hashCode()
        result = 31 * result + tmb.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + mediaCnt
        result = 31 * result + modified.hashCode()
        result = 31 * result + dateTaken.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }
}
