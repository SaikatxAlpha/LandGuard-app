package com.example.landguard.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * HTTP client for public Earth-observation / geodata APIs
 * (Microsoft Planetary Computer, Open-Meteo, NASA landslide catalog).
 *
 * Deliberately separate from the backend client in [NetworkModule]: that
 * client rewrites every request to the configured LandGuard backend host,
 * which must never happen to third-party data requests.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicDataClient

@Module
@InstallIn(SingletonComponent::class)
object PublicDataModule {

    @Provides
    @Singleton
    @PublicDataClient
    fun providePublicDataClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
}
