package com.stripe.samplestore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Factory instance to keep our Retrofit instance.
 */
public class RetrofitFactory {

    // Put your Base URL here. Unless you customized it, the URL will be something like
    // https://hidden-beach-12345.herokuapp.com/
    private static final String BASE_URL = "put your base url here";
    private static Retrofit mInstance = null;

    public static Retrofit getInstance() {
        if (mInstance == null) {

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // Set your desired log level. Use Level.BODY for debugging errors.
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(logging);

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            // Adding Rx so the calls can be Observable, and adding a Gson converter with
            // leniency to make parsing the results simple.
            mInstance = new Retrofit.Builder()
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .baseUrl(BASE_URL)
                    .client(httpClient.build())
                    .build();
        }
        return mInstance;
    }
}
