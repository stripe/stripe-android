package com.stripe.samplestore.service

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * The [retrofit2.Retrofit] interface that creates our API service.
 */
interface StripeService {

    /**
     * Returns the PaymentIntent client secret in the format shown below.
     *
     * {"secret": "pi_1Eu5SqCRMb_secret_O2Avhk5V0Pjeo"}
     */
    @POST("capture_payment")
    fun capturePayment(@Body params: HashMap<String, Any>): Observable<ResponseBody>

    /**
     * Used for Payment Intent Manual confirmation
     *
     * @see [Manual Confirmation Flow](https://stripe.com/docs/payments/payment-intents/quickstart.manual-confirmation-flow)
     *
     * Returns the PaymentIntent client secret in the format shown below.
     *
     * {"secret": "pi_1Eu5SqCRMb_secret_O2Avhk5V0Pjeo"}
     */
    @POST("confirm_payment")
    fun confirmPayment(@Body params: HashMap<String, Any>): Observable<ResponseBody>

    @POST("create_setup_intent")
    fun createSetupIntent(@Body params: HashMap<String, Any>): Observable<ResponseBody>

    @FormUrlEncoded
    @POST("ephemeral_keys")
    fun createEphemeralKey(@FieldMap apiVersionMap: HashMap<String, String>): Observable<ResponseBody>
}
