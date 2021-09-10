package com.stripe.android.paymentsheet.example.service

import android.content.Context
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import com.jakewharton.espresso.OkHttp3IdlingResource
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.example.Settings
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Factory to generate our Retrofit instance.
 */
internal class BackendApiFactory internal constructor(private val backendUrl: String) {

    constructor(context: Context) : this(Settings(context).backendUrl)

    @OptIn(ExperimentalSerializationApi::class)
    fun createCheckout(): CheckoutBackendApi {
        val logging = HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY)

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .baseUrl(backendUrl)
            .client(httpClient)
            .build()
            .create(CheckoutBackendApi::class.java)
    }

    private companion object {
        private const val TIMEOUT_SECONDS = 15L
    }
}
