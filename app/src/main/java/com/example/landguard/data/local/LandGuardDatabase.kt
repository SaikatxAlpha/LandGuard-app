package com.example.landguard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AlertEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LandGuardDatabase : RoomDatabase() {

    abstract fun alertDao(): AlertDao
}