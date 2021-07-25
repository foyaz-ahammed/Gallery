package com.kr.gallery.pro.interfaces

import androidx.lifecycle.LiveData
import androidx.room.*
import com.kr.gallery.pro.models.CoverPage
import com.kr.gallery.pro.models.Medium

/**
 * [CoverPage] table에 대한 query함수들을 정의해주었다.
 */
@Dao
interface CoverPageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(coverPage: CoverPage)

    /**
     * table에서 등록부경로들에 해당한 항목들을 삭제한다.
     * @param paths: 삭제하려는 등록부들의 경로 목록
     */
    @Query("DELETE FROM cover_page WHERE parent_path in (:paths)")
    fun deleteList(paths: List<String>)

    /**
     * table에 [CoverPage]자료목록을 추가한다.
     * @param data: 추가하려는 자료목록
     */
    @Insert
    fun insertList(data: List<CoverPage>)

    /**
     * 삭제, 삽입을 하나의 Transaction 으로 묶어서 진행한다.
     * @param paths: 삭제하려는 등록부들의 경로 목록
     * @param data: 삽입하려는 [CoverPage] 자료목록
     */
    @Transaction
    fun deleteAndInsert(paths: List<String>, data: List<CoverPage>){
        deleteList(paths)
        insertList(data)
    }

    /**
     * @return Cover page등록부들에 있는 media목록을 돌려준다.
     */
    @Query("SELECT media.filename, media.full_path, media.parent_path, media.last_modified, media.date_taken, media.date, media.size, type, media.video_duration, media.video_time " +
            "FROM cover_page LEFT JOIN media ON cover_page.parent_path=media.parent_path " +
            "WHERE media.filename != '' " +
            "ORDER BY media.date_taken desc")
    fun getAllMedia(): LiveData<List<Medium>>

    /**
     * @return Cover page의 등록부경로들 돌려준다.(숨겨졌거나 지워진 등록부는 제외)
     */
    @Query("SELECT parent_path FROM cover_page LEFT JOIN directories ON cover_page.parent_path = directories.path WHERE path != ''")
    fun getDirectories(): List<String>
}
