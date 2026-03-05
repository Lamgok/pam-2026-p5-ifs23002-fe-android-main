package org.delcom.pam_p5_ifs23002.network.todos.service

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.delcom.pam_p5_ifs23002.BuildConfig
import java.util.concurrent.TimeUnit

class TodoAppContainer: ITodoAppContainer {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    val okHttpClient = OkHttpClient.Builder().apply {
        if (BuildConfig.DEBUG) {
            addInterceptor(loggingInterceptor)
        }

        // Memperpendek timeout agar user tidak menunggu terlalu lama jika koneksi bermasalah
        connectTimeout(30, TimeUnit.SECONDS)
        readTimeout(30, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)
    }.build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    private val retrofitService: TodoApiService by lazy {
        retrofit.create(TodoApiService::class.java)
    }

    override val repository: ITodoRepository by lazy {
        TodoRepository(retrofitService)
    }
}