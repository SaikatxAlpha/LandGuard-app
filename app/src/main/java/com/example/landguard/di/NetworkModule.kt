package com.example.landguard.di

import com.example.landguard.BuildConfig
import com.example.landguard.data.network.LandGuardApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * The LandGuard backend. Every build talks to the production API
 * (https://api.landguard.online/) — the same backend the authority control
 * center uses. There is no runtime URL override: a fresh install connects
 * automatically once it is online.
 */
object BackendConfig {
    val baseUrl: String get() = BuildConfig.LANDGUARD_API_BASE_URL
    val host: String get() = baseUrl.substringAfter("://").substringBefore('/')
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // Never log bodies (tokens, device IDs) in release builds.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BackendConfig.baseUrl)
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
