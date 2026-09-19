package com.example.landguard.data.alerts

import android.content.Context
import android.util.Log
import com.example.landguard.data.network.LandGuardApiService
import com.example.landguard.data.network.ReceiptRequest
import com.example.landguard.domain.service.FcmTokenManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delivery confirmations back to the authority control center. Receipts
 * created while the phone is offline (e.g. an alert received over the mesh)
 * are queued on disk and sent when connectivity returns.
 */
@Singleton
class ReceiptSender @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: LandGuardApiService,
    private val tokens: FcmTokenManager
) {
    private data class Pending(val alertId: String, val event: String, val via: String, val hopCount: Int)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val gson = Gson()
    private val prefs get() = context.getSharedPreferences("LandGuardReceipts", Context.MODE_PRIVATE)

    fun send(alertId: String, event: String, via: String, hopCount: Int = 0) {
        scope.launch {
            if (!post(Pending(alertId, event, via, hopCount))) enqueue(Pending(alertId, event, via, hopCount))
        }
    }

    suspend fun flush() = mutex.withLock {
        val pending = read()
        if (pending.isEmpty()) return@withLock
        val failed = pending.filterNot { post(it) }
        write(failed)
        Log.i(TAG, "Flushed ${pending.size - failed.size}/${pending.size} queued receipts")
    }

    private suspend fun post(p: Pending): Boolean = try {
        val response = api.postReceipt(p.alertId, ReceiptRequest(tokens.installationId, p.event, p.via, p.hopCount))
        response.body()?.close()
        // 404: the backend does not know the alert (never retry); 4xx other than 429 are also final.
        response.isSuccessful || (response.code() in 400..499 && response.code() != 429)
    } catch (e: Exception) {
        false
    }

    private suspend fun enqueue(p: Pending) = mutex.withLock {
        write((read() + p).distinct().takeLast(500))
    }

    private fun read(): List<Pending> = runCatching {
        gson.fromJson<List<Pending>>(prefs.getString(KEY, "[]"), object : TypeToken<List<Pending>>() {}.type)
    }.getOrNull().orEmpty()

    private fun write(list: List<Pending>) {
        prefs.edit().putString(KEY, gson.toJson(list)).apply()
    }

    private companion object {
        const val TAG = "LandGuardReceipts"
        const val KEY = "pending"
    }
}
