package com.example.landguard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AlertEntity::class],
    version = 2,
    exportSchema = false
)
abstract class LandGuardDatabase : RoomDatabase() {

    abstract fun alertDao(): AlertDao

    companion object {
        /** v2 adds the shared alert-contract fields; existing alert history is kept. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alerts ADD COLUMN expiresAt TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE alerts ADD COLUMN serverStatus TEXT NOT NULL DEFAULT 'active'")
                db.execSQL("ALTER TABLE alerts ADD COLUMN source TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE alerts ADD COLUMN origin TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE alerts ADD COLUMN hopCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alerts ADD COLUMN receivedVia TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE alerts ADD COLUMN latitude REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE alerts ADD COLUMN longitude REAL DEFAULT NULL")
            }
        }
    }
}
