package com.stripe.android.test.e2e

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface Service {
    @POST("create_pi")
    suspend fun createPaymentIntent(): CreatedPaymentIntent

    @GET("fetch_pi")
    suspend fun fetchPaymentIntent(
        @Query("pi") id: String
    ): FetchedPaymentIntent
}
