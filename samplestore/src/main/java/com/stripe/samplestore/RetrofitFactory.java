package com.stripe.samplestore;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Factory instance to keep our Retrofit instance.
 */
public class RetrofitFactory {

    // Put your Base URL here. Unless you customized it, the URL will be something like
    // https://hidden-beach-12345.herokuapp.com/
    private static final String BASE_URL = "put your base url here";
    @Nullable private static Retrofit mInstance = null;

    @NonNull
    public static Retrofit getInstance() {
        if (mInstance == null) {
            // Set your desired log level. Use Level.BODY for debugging errors.
            final HttpLoggingInterceptor logging = new HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BASIC);

            final OkHttpClient httpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addNetworkInterceptor((new StethoInterceptor()))
                    .build();

            final Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            // Adding Rx so the calls can be Observable, and adding a Gson converter with
            // leniency to make parsing the results simple.
            mInstance = new Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .baseUrl(BASE_URL)
                    .client(httpClient)
                    .build();
        }

        return mInstance;
    }
}
