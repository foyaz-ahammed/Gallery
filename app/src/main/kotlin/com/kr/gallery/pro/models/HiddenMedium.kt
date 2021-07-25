package com.kr.gallery.pro.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_media", indices = [Index(value = ["full_path"], unique = true)])
data class HiddenMedium(
        @PrimaryKey(autoGenerate = true) var id: Long?,
        @ColumnInfo(name = "filename") var name: String,
        @ColumnInfo(name = "full_path") var path: String,
        @ColumnInfo(name = "parent_path") var parentPath: String,
        @ColumnInfo(name = "last_modified") val modified: Long,
        @ColumnInfo(name = "date_taken") val dateTaken: Long,
        @ColumnInfo(name = "date") val date: String,
        @ColumnInfo(name = "size") val size: Long,
        @ColumnInfo(name = "type") val type: Int,
        @ColumnInfo(name = "video_duration") val videoDuration: Int,
        @ColumnInfo(name = "video_time") val videoTime: Int
){
    constructor(medium: Medium): this(null, medium.name, medium.path, medium.parentPath, medium.modified, medium.dateTaken, medium.date, medium.size, medium.type,
            medium.videoDuration, medium.videoTime)
}
