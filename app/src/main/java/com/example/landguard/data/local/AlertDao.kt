package com.example.landguard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Upsert
    suspend fun upsertAll(alerts: List<AlertEntity>)

    @Upsert
    suspend fun upsert(alert: AlertEntity)

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AlertEntity?

    @Query("DELETE FROM alerts")
    suspend fun deleteAll()
}