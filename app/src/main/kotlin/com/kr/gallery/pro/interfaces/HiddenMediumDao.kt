package com.kr.gallery.pro.interfaces

import androidx.lifecycle.LiveData
import androidx.room.*
import com.kr.gallery.pro.models.HiddenDirectory
import com.kr.gallery.pro.models.HiddenMedium

/**
 * [HiddenMedium] table에 대한 query함수들을 정의해주었다.
 */
@Dao
interface HiddenMediumDao {
    /**
     * 숨겨진 media들을 등록부별로 갈라서 돌려준다.
     */
    @Query("SELECT parent_path as folderPath, full_path as thumbnail, COUNT(*) as mediaCount FROM (SELECT * FROM hidden_media ORDER BY parent_path, date_taken, last_modified, filename DESC) GROUP BY parent_path ORDER BY parent_path")
    fun getHiddenDirectory(): LiveData<List<HiddenDirectory>>

    /**
     * 파라메터로 들어온 등록부들에 있는 숨겨진 media들을 돌려준다.
     * @param pathList '등록부경로' 목록
     */
    @Query("SELECT full_path FROM hidden_media WHERE parent_path in (:pathList)")
    fun getHiddenMediaByDirectory(pathList: List<String>): List<String>

    /**
     * 하나의 [HiddenMedium]자료를 tale에 삽입
     * @param hiddenMedium 삽입하려는 [HiddenMedium]자료
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(hiddenMedium: HiddenMedium)

    /**
     * Table에서 해당한 경로를 가진 항목 삭제
     * @param path: 삭제하려는 화일경로
     */
    @Query("DELETE FROM hidden_media WHERE full_path = :path")
    fun deleteItem(path: String)
}
