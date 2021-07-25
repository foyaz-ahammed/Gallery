package com.kr.gallery.pro.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// CoverPage Table
@Entity(tableName = "cover_page", indices = [Index(value = ["parent_path"], unique = true)])
data class CoverPage(
        @PrimaryKey(autoGenerate = true) val id: Int?,
        @ColumnInfo(name = "parent_path") val parentPath: String
)
