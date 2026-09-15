package com.example.landguard.di

import android.content.Context
import android.content.SharedPreferences
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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val DUMMY_BASE_URL = "http://unconfigured.local/"
    const val PREFS_NAME = "LandGuardNetworkPrefs"
    const val KEY_BASE_URL = "base_url"

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideDynamicUrlInterceptor(sharedPreferences: SharedPreferences): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val baseUrlString = sharedPreferences.getString(KEY_BASE_URL, null)
            val activeBaseUrl = if (!baseUrlString.isNullOrBlank()) baseUrlString else DUMMY_BASE_URL
            val newBaseUrl = activeBaseUrl.toHttpUrlOrNull()
            
            if (newBaseUrl != null) {
                android.util.Log.d("LandGuardBackend", "Using backend URL = $activeBaseUrl")
                
                // Combine the base URL path with the requested endpoint path
                val combinedSegments = mutableListOf<String>()
                combinedSegments.addAll(newBaseUrl.pathSegments.filter { it.isNotEmpty() })
                combinedSegments.addAll(request.url.pathSegments.filter { it.isNotEmpty() })

                val newUrlBuilder = request.url.newBuilder()
                    .scheme(newBaseUrl.scheme)
                    .host(newBaseUrl.host)
                    .port(newBaseUrl.port)
                    .encodedPath("/") // Reset path

                for (segment in combinedSegments) {
                    newUrlBuilder.addPathSegment(segment)
                }
                
                val newRequest = request.newBuilder()
                    .url(newUrlBuilder.build())
                    .build()
                chain.proceed(newRequest)
            } else {
                chain.proceed(request)
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(dynamicUrlInterceptor: Interceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(DUMMY_BASE_URL)
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
