package com.pinnacle.smsorganizer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CategoryOverrideEntity::class,
        ThreadStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SmsDatabase : RoomDatabase() {

    abstract fun categoryOverrideDao(): CategoryOverrideDao
    abstract fun threadStateDao(): ThreadStateDao

    companion object {
        @Volatile
        private var INSTANCE: SmsDatabase? = null

        fun getInstance(context: Context): SmsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmsDatabase::class.java,
                    "pinnacle_sms_organizer.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
