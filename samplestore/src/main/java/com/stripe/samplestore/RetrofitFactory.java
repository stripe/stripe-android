package com.stripe.samplestore;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Factory instance to keep our Retrofit instance.
 */
public class RetrofitFactory {

    private static final String BASE_URL = "put your service url here";
    private static Retrofit mInstance = null;

    public static Retrofit getInstance() {
        if (mInstance == null) {
            mInstance = new Retrofit.Builder()
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(BASE_URL)
                    .build();
        }
        return mInstance;
    }
}
