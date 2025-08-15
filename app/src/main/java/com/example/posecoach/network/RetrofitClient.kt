package com.example.posecoach.network

import com.example.posecoach.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val client: OkHttpClient by lazy {
        val log = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(log)
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val api: PoseApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.POSE_SERVER_BASE_URL) // 예: http://10.0.2.2:5001/
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PoseApi::class.java)
    }
}
