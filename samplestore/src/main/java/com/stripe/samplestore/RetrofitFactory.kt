package com.stripe.samplestore

import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Factory instance to keep our Retrofit instance.
 */
object RetrofitFactory {
    val instance: Retrofit

    init {
        // Set your desired log level. Use Level.BODY for debugging errors.
        // Adding Rx so the calls can be Observable, and adding a Gson converter with
        // leniency to make parsing the results simple.

        val logging = HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BASIC)

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addNetworkInterceptor(StethoInterceptor())
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()
        instance = Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(Settings.BASE_URL)
            .client(httpClient)
            .build()
    }
}
