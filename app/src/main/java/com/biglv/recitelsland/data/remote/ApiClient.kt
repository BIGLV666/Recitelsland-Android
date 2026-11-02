package com.biglv.recitelsland.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    fun create(tokenProvider: () -> String?): ReciteApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val token = tokenProvider()
                val request = chain.request().newBuilder().apply {
                    if (!token.isNullOrBlank()) {
                        header("Authorization", "Bearer $token")
                    }
                    header("Accept", "application/json")
                }.build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ReciteApi::class.java)
    }
}
