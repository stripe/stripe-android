package com.stripe.example.service

import okhttp3.ResponseBody
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * A Retrofit service used to communicate with a server.
 */
interface BackendApi {

    @FormUrlEncoded
    @POST("ephemeral_keys")
    suspend fun createEphemeralKey(
        @FieldMap apiVersionMap: HashMap<String, String>
    ): ResponseBody

    @FormUrlEncoded
    @POST("{create_pi_path}")
    suspend fun createPaymentIntent(
        @FieldMap params: MutableMap<String, String>,
        @Path("create_pi_path") createPaymentIntentEndpoint: String = DEFAULT_CREATE_PI_ENDPOINT,
    ): ResponseBody

    @FormUrlEncoded
    @POST("{create_si_path}")
    suspend fun createSetupIntent(
        @FieldMap params: MutableMap<String, String>,
        @Path("create_si_path") createSetupIntentEndpoint: String = DEFAULT_CREATE_SI_ENDPOINT,
    ): ResponseBody

    companion object {
        const val DEFAULT_CREATE_PI_ENDPOINT = "create_payment_intent"
        const val DEFAULT_CREATE_SI_ENDPOINT = "create_setup_intent"
    }
}
