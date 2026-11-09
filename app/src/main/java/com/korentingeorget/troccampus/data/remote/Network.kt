package com.korentingeorget.troccampus.data.remote

import android.content.Context
import android.util.Log
import com.korentingeorget.troccampus.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File

object Network {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun okHttp(context: Context): OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "http"), 10L * 1024 * 1024))
        .addInterceptor(HttpLoggingInterceptor { Log.d("TrocCampus", it) }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    fun syncApi(client: OkHttpClient): SyncApi {
        val base = if (BuildConfig.SYNC_BASE_URL.endsWith("/")) BuildConfig.SYNC_BASE_URL else "${BuildConfig.SYNC_BASE_URL}/"
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SyncApi::class.java)
    }

    fun httpbinApi(client: OkHttpClient): SyncApi {
        return Retrofit.Builder()
            .baseUrl("https://httpbin.org/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SyncApi::class.java)
    }
}
