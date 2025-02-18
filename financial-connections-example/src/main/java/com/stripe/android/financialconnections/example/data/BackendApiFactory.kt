package com.stripe.android.financialconnections.example.data

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Factory to generate our Retrofit instance.
 */
internal class BackendApiFactory(private val settings: Settings) {

    fun create(): BackendApiService {
        val logging = HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY)

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor(RetryOnSocketTimeoutInterceptor())
            .build()

        val json = Json {
            ignoreUnknownKeys = true
        }
        return Retrofit.Builder()
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .baseUrl(settings.backendUrl)
            .client(httpClient)
            .build()
            .create(BackendApiService::class.java)
    }

    private companion object {
        private const val TIMEOUT_SECONDS = 30L
    }
}

/**
 * Example servers go idle frequently, resulting in [SocketTimeoutException].
 *
 */
private class RetryOnSocketTimeoutInterceptor : Interceptor {
    companion object {
        private const val MAX_RETRIES = 3
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var retries = 0
        while (true) {
            try {
                return chain.proceed(request)
            } catch (e: SocketTimeoutException) {
                if (retries >= MAX_RETRIES) {
                    throw e
                }
                retries++
                // Exponential backoff
                val delay = (1 shl retries).toLong()
                Thread.sleep(delay)
            }
        }
    }
}
