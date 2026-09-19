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

    /** Alerts that may still be passed on over the offline mesh (expiry is checked by the caller). */
    @Query("SELECT * FROM alerts WHERE serverStatus = 'active' AND expiresAt IS NOT NULL ORDER BY timestamp DESC")
    suspend fun activeWithExpiry(): List<AlertEntity>

    @Query("UPDATE alerts SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE alerts SET serverStatus = :serverStatus WHERE id = :id")
    suspend fun updateServerStatus(id: String, serverStatus: String)

    @Query("DELETE FROM alerts")
    suspend fun deleteAll()
}
