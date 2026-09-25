package com.rakshacall.safety.data.remote.client

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.rakshacall.safety.data.remote.api.ApiService
import com.rakshacall.safety.data.remote.config.AppConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Production OkHttp & Retrofit client builder with authentication interceptor,
 * timeouts, and structured error handling.
 */
object NetworkClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Volatile
    private var authToken: String? = null

    fun setAuthToken(token: String?) {
        authToken = token
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
            .header("User-Agent", "RakshaCall-Android/${AppConfig.APP_VERSION}")
            .header("Accept", "application/json")

        authToken?.let {
            builder.header("Authorization", "Bearer $it")
        }

        chain.proceed(builder.build())
    }

    private val retryInterceptor = Interceptor { chain ->
        var response: Response? = null
        var exception: IOException? = null
        var tryCount = 0
        val maxLimit = 2

        while (tryCount < maxLimit && (response == null || !response.isSuccessful)) {
            try {
                response?.close()
                response = chain.proceed(chain.request())
            } catch (e: IOException) {
                exception = e
            }
            tryCount++
        }

        if (response != null) {
            response
        } else {
            throw exception ?: IOException("Network request failed after $maxLimit attempts")
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(retryInterceptor)
            .build()
    }

    val apiService: ApiService by lazy {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(AppConfig.activeBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiService::class.java)
    }
}
