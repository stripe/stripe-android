package com.stripe.example.service

import okhttp3.ResponseBody
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * A Retrofit service used to communicate with a server.
 */
interface BackendApi {

    @FormUrlEncoded
    @POST("ephemeral_keys")
    suspend fun createEphemeralKey(@FieldMap apiVersionMap: HashMap<String, String>): ResponseBody

    @FormUrlEncoded
    @POST("create_payment_intent")
    suspend fun createPaymentIntent(@FieldMap params: MutableMap<String, String>): ResponseBody

    @FormUrlEncoded
    @POST("create_setup_intent")
    suspend fun createSetupIntent(@FieldMap params: MutableMap<String, String>): ResponseBody

    @FormUrlEncoded
    @POST("confirm_payment_intent")
    suspend fun confirmPaymentIntent(@FieldMap params: MutableMap<String, String>): ResponseBody
}
