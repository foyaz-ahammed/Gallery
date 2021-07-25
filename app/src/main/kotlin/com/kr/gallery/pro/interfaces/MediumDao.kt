package com.kr.gallery.pro.interfaces

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.kr.gallery.pro.models.Directory
import com.kr.gallery.pro.models.Medium
import com.kr.gallery.pro.models.TimeThumbnailItemJsonMedia

/**
 * [Medium] table에 대한 query함수들을 정의해주었다.
 */
@Dao
interface MediumDao {
    @Query("SELECT filename, full_path, parent_path, last_modified, date_taken, date, size, type, video_duration, video_time FROM media WHERE parent_path = :path COLLATE NOCASE")
    fun getMediaFromPath(path: String): List<Medium>

    //전체 media들을 돌려준다
    @Query("SELECT * FROM media ORDER BY date_taken")
    fun getAllMedia(): List<Medium>

    //전체 media들을 시간을 기준으로 하여 돌려준다(Live data)
    @Query("SELECT * FROM media ORDER BY date DESC, type DESC, date_taken DESC, last_modified DESC, filename")
    fun getAllLiveMedia(): LiveData<List<Medium>>

    //등록부들에 해당한 media들을 돌려준다(Live data)
    @Query("SELECT * FROM media WHERE parent_path in (:directoryList) ORDER BY date_taken DESC, last_modified DESC, filename")
    fun getAllLiveMediaByDirectories(directoryList: List<String>): LiveData<List<Medium>>

    //숨김등록부에 해당한 media들을 돌려준다(Live data)
    @Query("SELECT * FROM hidden_media WHERE parent_path is (:directory) ORDER BY date_taken DESC, last_modified DESC, filename")
    fun getAllLiveMediaHiddenByDirectory(directory: String): LiveData<List<Medium>>

    //등록부들에 들어있는 모든 media들을 돌려준다
    @Query("SELECT filename, full_path, parent_path, last_modified, date_taken, date, size, type, video_duration, video_time FROM media WHERE parent_path in (:directoryList) ORDER BY date_taken DESC, last_modified DESC, filename")
    fun getMediaByDirectoryList(directoryList: List<String>): List<Medium>

    //등록부에 있는 모든 media들을 LiveData로 돌려준다.
    @Query("SELECT filename, full_path, parent_path, last_modified, date_taken, date, date_taken, size, type, video_duration, video_time FROM media WHERE parent_path = :directory ORDER BY date_taken DESC, last_modified DESC, filename")
    fun getMediaByDirectory(directory: String): LiveData<List<Medium>>

    //등록부에 있는 모든 화상 돌려주기
    @Query("SELECT filename, full_path, parent_path, last_modified, date_taken, date, date_taken, size, type, video_duration, video_time FROM media WHERE parent_path = :directory And type = 1 ORDER BY date_taken DESC, last_modified DESC, filename")
    fun getImagesByDirectory(directory: String): LiveData<List<Medium>>

    //등록부에 있는 모든 동영상 돌려주기
    @Query("SELECT filename, full_path, parent_path, last_modified, date_taken, date, date_taken, size, type, video_duration, video_time FROM media WHERE parent_path = :directory And type = 2 ORDER BY date_taken DESC, last_modified DESC, filename")
    fun getVideosByDirectory(directory: String): LiveData<List<Medium>>

    //등록부에 있는 숨겨진 media들을 돌려주기
    @Query("SELECT filename, full_path, parent_path, last_modified, date_taken, date, date_taken, size, type, video_duration, video_time FROM hidden_media WHERE parent_path = :directory ORDER BY date_taken DESC, last_modified DESC, filename")
    fun getHiddenMediaByDirectory(directory: String): LiveData<List<Medium>>

    /**
     * @return 날자에 해당한 화상|동영상들을 [LiveData]로 돌려준다
     * @param date: 날자(2021.1.21)
     * @param type: 1 - 화상, 2 - 동영상
     */
    @Query("SELECT filename, full_path, parent_path, last_modified, date_taken, date, date_taken, size, type, video_duration, video_time FROM media WHERE date = :date AND type = :type ORDER BY date_taken DESC, last_modified DESC, fileName")
    fun getMediaLiveDataByDate(date: String, type: Int): LiveData<List<Medium>>

    /**
     * @return 날자에 해당한 화상|동영상들을 List로 돌려준다
     * @param date: 날자(2021.1.21)
     * @param type: 1 - 화상, 2 - 동영상
     */
    @Query("SELECT filename, full_path, parent_path, last_modified, date_taken, date, date_taken, size, type, video_duration, video_time FROM media WHERE date = :date AND type = :type ORDER BY date_taken DESC, last_modified DESC, fileName")
    fun getMediaListByDate(date: String, type: Int): List<Medium>

    /**
     * Media목록을 삽입한다.
     * @param media: 삽입하려는 media목록
     */
    @Insert(onConflict = REPLACE)
    fun insertList(media: List<Medium>)

    /**
     * Media목록을 삭제한다.
     * @param mediaList: 삭제하려는 media목록
     */
    @Delete
    fun deleteList(mediaList: List<Medium>)

    /**
     * 삭제, 삽입을 하나의 Transaction 으로 묶어서 진행한다.
     * @param mediaToDelete: 삭제하려는 media목록
     * @param mediaToAdd: 삽입하려는 media목록
     */
    @Transaction
    fun deleteAndInsert(mediaToDelete: List<Medium>, mediaToAdd: List<Medium>) {
        deleteList(mediaToDelete)
        insertList(mediaToAdd)
    }

    /**
     * 1개 media 추가
     */
    @Insert(onConflict = REPLACE)
    fun insertItem(medium: Medium)

    /**
     * 1개 media삭제 (입력은 화일경로)
     */
    @Query("DELETE FROM media WHERE full_path = :path")
    fun deleteItem(path: String)

    @Query("SELECT video_time FROM media WHERE full_path = :path")
    fun getVideoTime(path: String): List<Int>

    @Query("UPDATE media SET video_time = :time WHERE full_path = :path")
    fun updateVideoTime(path: String, time: Int)

    //Table안의 media자료들로부터 Directory를 얻어내기
    @Query("SELECT parent_path as path, full_path as thumbnail, replace(parent_path, rtrim(parent_path, replace(parent_path, '/', '')), '') AS path_name, Count(*) as media_count, last_modified, date_taken, SUM(size) as size FROM (SELECT * FROM media ORDER BY parent_path COLLATE NOCASE, date_taken, last_modified, filename DESC) GROUP BY parent_path")
    fun getDirectories(): List<Directory>

    //화상만을 포함하여 directory생성
    @Query("SELECT parent_path as path, full_path as thumbnail, replace(parent_path, rtrim(parent_path, replace(parent_path, '/', '')), '') AS path_name, Count(*) as media_count, last_modified, date_taken, SUM(size) as size FROM (SELECT * FROM media WHERE type = 1 ORDER BY parent_path COLLATE NOCASE, date_taken, last_modified, filename DESC) GROUP BY parent_path")
    fun getImageDirectories(): LiveData<List<Directory>>

    /**
     * 전체 화상을 날자별로 갈라서 돌려준다.
     * @see TimeThumbnailItemJsonMedia
     */
    @Query("SELECT time_thumb.date, time_thumb.count, time_thumb.type, " +
            "'[' || GROUP_CONCAT('{\"full_path\":'||'\"'||media.full_path || '\", \"filename\":'||'\"'||media.filename || '\", \"date_taken\":'||'\"'||media.date_taken || '\"' ||'}')||']' mediaEncryptJson " +
            "FROM (" +
            "(SELECT id, date, count(id) as count, type FROM media GROUP BY date, type) " +
            "time_thumb) " +
            "LEFT JOIN media ON time_thumb.date=media.date AND time_thumb.type=media.type " +
            "GROUP BY time_thumb.id " +
            "ORDER BY time_thumb.date DESC"
    )
    fun getAllThumbnailWithMedia(): LiveData<List<TimeThumbnailItemJsonMedia>>
}
