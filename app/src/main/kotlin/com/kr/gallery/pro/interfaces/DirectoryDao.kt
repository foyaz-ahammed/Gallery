package com.kr.gallery.pro.interfaces

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.kr.gallery.pro.models.Directory

/**
 * [Directory] table에 대한 query함수들을 정의해주었다.
 */
@Dao
interface DirectoryDao {
    @Query("SELECT * FROM directories ORDER BY path_name COLLATE NOCASE, media_count, size")
    fun getAll(): List<Directory>

    /**
     * 등록부목록을 이름, 개수, 크기 순서로 정렬하여 돌려준다.
     */
    @Query("SELECT path, thumbnail, path_name, media_count, last_modified, date_taken, size FROM directories ORDER BY path_name COLLATE NOCASE, media_count, size")
    fun getAllDirectories(): LiveData<List<Directory>>

    @Insert(onConflict = REPLACE)
    fun insert(directory: Directory)

    //등록부목록을 삽입한다.
    @Insert(onConflict = REPLACE)
    fun insertList(directories: List<Directory>)

    //등록부목록을 삭제한다.
    @Delete
    fun deleteList(mediaList: List<Directory>)

    /**
     * 삭제, 삽입을 하나의 Transaction 으로 묶어서 진행한다.
     * @param dirsToDelete: 삭제하려는 [Directory]목록
     * @param dirsToAdd: 삽입하려는 [Directory]목록
     */
    @Transaction
    fun deleteAndInsert(dirsToDelete: List<Directory>, dirsToAdd: List<Directory>){
        deleteList(dirsToDelete)
        insertList(dirsToAdd)
    }

    /**
     * 등록부 하나를 찾아서 지운다.
     * @param path 등록부경로
     */
    @Query("DELETE FROM directories WHERE path = :path COLLATE NOCASE")
    fun deleteDirPath(path: String)

    /**
     * @return 등록부경로로부터 thumbnail경로를 얻어낸다.
     * @param path: 등록부경로
     */
    @Query("SELECT thumbnail FROM directories WHERE path = :path")
    fun getDirectoryThumbnail(path: String): String?
}
