package com.stripe.android.test.e2e

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface Service {
    @POST("e2e/create_pi")
    suspend fun createCardPaymentIntent(): Response.CreatedCardPaymentIntent

    @GET("e2e/fetch_pi")
    suspend fun fetchCardPaymentIntent(
        @Query("pi") id: String
    ): Response.FetchedCardPaymentIntent

    @POST("create_payment_intent")
    suspend fun createPaymentIntent(
        @Body request: Request.CreatePaymentIntentParams
    ): Response.CreatedPaymentIntent

    @POST("create_setup_intent")
    suspend fun createSetupIntent(
        @Body request: Request.CreateSetupIntentParams
    ): Response.CreatedSetupIntent

    @POST("create_ephemeral_key")
    suspend fun createEphemeralKey(
        @Body request: Request.CreateEphemeralKeyParams
    ): Response.CreatedEphemeralKey
}
