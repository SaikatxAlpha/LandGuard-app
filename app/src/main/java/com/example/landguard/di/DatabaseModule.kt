package com.example.landguard.di

import android.content.Context
import androidx.room.Room
import com.example.landguard.data.local.AlertDao
import com.example.landguard.data.local.LandGuardDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): LandGuardDatabase {
        return Room.databaseBuilder(
            context,
            LandGuardDatabase::class.java,
            "landguard.db"
        ).addMigrations(LandGuardDatabase.MIGRATION_1_2).build()
    }

    @Provides
    fun provideAlertDao(
        database: LandGuardDatabase
    ): AlertDao {
        return database.alertDao()
    }
}