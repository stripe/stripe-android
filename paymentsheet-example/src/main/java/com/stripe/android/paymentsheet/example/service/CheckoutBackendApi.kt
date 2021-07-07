package com.stripe.android.paymentsheet.example.service

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

@Serializable
data class CheckoutRequest(
    val customer: String,
    val currency: String,
    val mode: String
)

@Serializable
data class CheckoutResponse(
    val publishableKey: String,
    val intentClientSecret: String,
    val customerId: String? = null,
    val customerEphemeralKeySecret: String? = null
)

interface CheckoutBackendApi {

    @Headers("Content-Type: application/json")
    @POST("checkout")
    suspend fun checkout(@Body checkoutRequest: CheckoutRequest): CheckoutResponse
}
