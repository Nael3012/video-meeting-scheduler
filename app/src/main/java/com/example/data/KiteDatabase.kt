package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MeetingEntity::class,
        ReminderEntity::class,
        UserAccountEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class KiteDatabase : RoomDatabase() {
    abstract fun kiteDao(): KiteDao

    companion object {
        @Volatile
        private var INSTANCE: KiteDatabase? = null

        fun getDatabase(context: Context): KiteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KiteDatabase::class.java,
                    "kite_meetings_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
