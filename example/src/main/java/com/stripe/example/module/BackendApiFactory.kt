package com.stripe.example.module

import android.content.Context
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.stripe.example.Settings
import com.stripe.example.service.BackendApi
import com.stripe.example.service.CheckoutBackendApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory to generate our Retrofit instance.
 */
internal class BackendApiFactory internal constructor(private val backendUrl: String) {

    constructor(context: Context) : this(Settings(context).backendUrl)

    fun create(): BackendApi {
        // Set your desired log level. Use Level.BODY for debugging errors.
        // Adding Rx so the calls can be Observable, and adding a Gson converter with
        // leniency to make parsing the results simple.
        val logging = HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY)

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(backendUrl)
            .client(httpClient)
            .build()
            .create(BackendApi::class.java)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun createCheckout(): CheckoutBackendApi {
        if (Settings.PAYMENT_SHEET_BASE_URL.isNullOrEmpty() ||
            Settings.PAYMENT_SHEET_PUBLISHABLE_KEY.isNullOrEmpty()
        ) {
            error(
                "Settings.PAYMENT_SHEET_BASE_URL and Settings.PAYMENT_SHEET_PUBLISHABLE_KEY " +
                    "must be set for PaymentSheet example"
            )
        }

        val logging = HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY)

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .baseUrl(Settings.PAYMENT_SHEET_BASE_URL)
            .client(httpClient)
            .build()
            .create(CheckoutBackendApi::class.java)
    }

    private companion object {
        private const val TIMEOUT_SECONDS = 15L
    }
}
