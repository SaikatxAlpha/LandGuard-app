package com.example.landguard.data.regional

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class HttpStatusException(val code: Int, message: String) : IOException(message)

private val JSON = "application/json".toMediaType()

internal suspend fun OkHttpClient.getJson(url: String): JsonElement =
    execute(Request.Builder().url(url).get().build())

internal suspend fun OkHttpClient.postJson(url: String, body: JsonObject): JsonElement =
    execute(Request.Builder().url(url).post(body.toString().toRequestBody(JSON)).build())

/** One retry for transient network failures (DNS, resets, timeouts) — not for HTTP errors. */
private suspend fun OkHttpClient.execute(request: Request): JsonElement =
    try {
        executeOnce(request)
    } catch (e: HttpStatusException) {
        throw e
    } catch (e: IOException) {
        delay(1_500)
        executeOnce(request)
    }

private suspend fun OkHttpClient.executeOnce(request: Request): JsonElement {
    val text = suspendCancellableCoroutine<String> { cont ->
        val call = newCall(request)
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val bodyText = it.body?.string().orEmpty()
                    if (!it.isSuccessful) {
                        cont.resumeWithException(HttpStatusException(it.code, "HTTP ${it.code}: ${bodyText.take(200)}"))
                    } else {
                        cont.resume(bodyText)
                    }
                }
            }
        })
    }
    return withContext(Dispatchers.Default) { JsonParser.parseString(text) }
}

internal fun JsonObject.str(name: String): String? =
    get(name)?.takeUnless { it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }

internal fun JsonObject.dbl(name: String): Double? =
    get(name)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asDouble }.getOrNull() }

internal fun JsonObject.lng(name: String): Long? =
    get(name)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asLong }.getOrNull() }

internal fun JsonObject.int(name: String): Int? =
    get(name)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asInt }.getOrNull() }
