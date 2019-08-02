package com.stripe.example.service

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * A Retrofit service used to communicate with a server.
 */
interface StripeService {

    @FormUrlEncoded
    @POST("ephemeral_keys")
    fun createEphemeralKey(@FieldMap apiVersionMap: HashMap<String, String>): Observable<ResponseBody>

    @FormUrlEncoded
    @POST("create_intent")
    fun createPaymentIntent(@FieldMap params: HashMap<String, Any>): Observable<ResponseBody>

    @FormUrlEncoded
    @POST("create_setup_intent")
    fun createSetupIntent(@FieldMap params: HashMap<String, Any>): Observable<ResponseBody>
}
