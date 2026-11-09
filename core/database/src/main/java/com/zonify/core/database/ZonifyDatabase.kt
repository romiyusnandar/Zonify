package com.zonify.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zonify.core.database.dao.PlaylistDao
import com.zonify.core.database.dao.TrackDao
import com.zonify.core.database.entity.PlaylistEntity
import com.zonify.core.database.entity.PlaylistTrackCrossRef
import com.zonify.core.database.entity.TrackEntity

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ZonifyDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        private const val DATABASE_NAME = "zonify_music.db"

        @Volatile
        private var INSTANCE: ZonifyDatabase? = null

        fun getInstance(context: Context): ZonifyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZonifyDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
