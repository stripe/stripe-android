package com.stripe.android.paymentsheet.example.service

import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

@Serializable
data class CheckoutRequest(
    val customer: String,
    val currency: String,
    val mode: String,
    val set_shipping_address: Boolean
)

@Serializable
data class CheckoutResponse(
    val publishableKey: String,
    val intentClientSecret: String,
    val customerId: String? = null,
    val customerEphemeralKeySecret: String? = null,
    val paymentMethods: List<String>
) {
    internal fun makeCustomerConfig() =
        if (customerId != null && customerEphemeralKeySecret != null) {
            PaymentSheet.CustomerConfiguration(
                id = customerId,
                ephemeralKeySecret = customerEphemeralKeySecret
            )
        } else {
            null
        }
}

interface CheckoutBackendApi {

    @Headers("Content-Type: application/json")
    @POST("checkout")
    suspend fun checkout(@Body checkoutRequest: CheckoutRequest): CheckoutResponse
}
