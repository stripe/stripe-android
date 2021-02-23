package com.stripe.android.test.e2e

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ServiceFactory {

    fun create(
        baseUrl: String
    ): Service {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(Service::class.java)
    }
}
