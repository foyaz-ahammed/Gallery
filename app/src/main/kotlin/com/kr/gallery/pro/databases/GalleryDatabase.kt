package com.kr.gallery.pro.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kr.commons.helpers.CAMERA_PATH
import com.kr.gallery.pro.extensions.config
import com.kr.gallery.pro.interfaces.*
import com.kr.gallery.pro.models.*

/**
 * Database클라스
 */
@Database(entities = [
        Directory::class, Medium::class, Widget::class, CoverPage::class, HiddenMedium::class
    ], version = 10)
abstract class GalleryDatabase : RoomDatabase() {

    abstract fun DirectoryDao(): DirectoryDao

    abstract fun MediumDao(): MediumDao

    abstract fun WidgetsDao(): WidgetsDao

    abstract fun CoverPageDao(): CoverPageDao

    abstract fun HiddenMediumDao(): HiddenMediumDao

    companion object {
        private var db: GalleryDatabase? = null

        fun getInstance(context: Context): GalleryDatabase {
            if (db == null) {
                synchronized(GalleryDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(context.applicationContext, GalleryDatabase::class.java, "gallery.db")
                                .fallbackToDestructiveMigration()
                                .addMigrations(MIGRATION_4_5)
                                .addMigrations(MIGRATION_5_6)
                                .addMigrations(MIGRATION_6_7)
                                .build()

                        //자료기지가 처음 창조될때 cover page table에 camera등록부 추가
                        val config = context.config
                        if(config.isFirstDatabaseInstance()) {
                            config.clearFirstDatabaseInstance()
                            db!!.CoverPageDao().insert(CoverPage(null, CAMERA_PATH))
                        }
                    }
                }
            }
            return db!!
        }

        fun destroyInstance() {
            db = null
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE media ADD COLUMN video_duration INTEGER default 0 NOT NULL")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `widgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `widget_id` INTEGER NOT NULL, `folder_path` TEXT NOT NULL)")
                database.execSQL("CREATE UNIQUE INDEX `index_widgets_widget_id` ON `widgets` (`widget_id`)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE directories ADD COLUMN sort_value TEXT default '' NOT NULL")
            }
        }
    }
}
