package com.example.landguard.di

import android.content.Context
import android.content.SharedPreferences
import com.example.landguard.BuildConfig
import com.example.landguard.data.network.LandGuardApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * The LandGuard backend. Release builds always use the production API
 * (https://api.landguard.online/) — the same backend the authority control
 * center uses. Debug builds may override it from More → Server connection.
 */
object BackendConfig {
    const val PREFS_NAME = "LandGuardNetworkPrefs"
    const val KEY_BASE_URL = "base_url"

    val productionBaseUrl: String get() = BuildConfig.LANDGUARD_API_BASE_URL

    /** A developer override is honoured only in debug builds. */
    fun baseUrl(prefs: SharedPreferences): String {
        if (!BuildConfig.DEBUG) return productionBaseUrl
        return prefs.getString(KEY_BASE_URL, null)?.takeIf { it.isNotBlank() } ?: productionBaseUrl
    }

    fun baseUrl(context: Context): String =
        baseUrl(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    const val PREFS_NAME = BackendConfig.PREFS_NAME
    const val KEY_BASE_URL = BackendConfig.KEY_BASE_URL

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Rewrites requests only when a debug override differs from the production base URL. */
    @Provides
    @Singleton
    fun provideDynamicUrlInterceptor(sharedPreferences: SharedPreferences): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val active = BackendConfig.baseUrl(sharedPreferences)
            if (active == BackendConfig.productionBaseUrl) return@Interceptor chain.proceed(request)
            val base = active.toHttpUrlOrNull() ?: return@Interceptor chain.proceed(request)
            val productionSegments = BackendConfig.productionBaseUrl.toHttpUrlOrNull()
                ?.pathSegments?.count { it.isNotEmpty() } ?: 0
            val relative = request.url.pathSegments.drop(productionSegments)
            val url = request.url.newBuilder()
                .scheme(base.scheme)
                .host(base.host)
                .port(base.port)
                .encodedPath("/")
                .apply {
                    base.pathSegments.filter { it.isNotEmpty() }.forEach { addPathSegment(it) }
                    relative.filter { it.isNotEmpty() }.forEach { addPathSegment(it) }
                }
                .build()
            chain.proceed(request.newBuilder().url(url).build())
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(dynamicUrlInterceptor: Interceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // Never log bodies (tokens, device IDs) in release builds.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BackendConfig.productionBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideLandGuardApiService(retrofit: Retrofit): LandGuardApiService {
        return retrofit.create(LandGuardApiService::class.java)
    }
}
