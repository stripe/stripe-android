package com.stripe.android.iapexample

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

internal interface BackendService {
    @POST("create_checkout_session_url")
    suspend fun createCheckoutSessionUrl(
        @Body body: CreateRequest
    ): CheckoutSessionCreateResponse

    @POST("create_iap_customer_session")
    suspend fun createIapCustomerSession(
        @Body body: CreateRequest
    ): CustomerSessionCreateResponse
}

@Serializable
internal data class CheckoutSessionCreateResponse(
    @SerialName("url")
    val url: String,
)

@Serializable
internal data class CustomerSessionCreateResponse(
    @SerialName("customerSessionClientSecret") val customerSessionClientSecret: String,
)

@Serializable
internal data class CreateRequest(
    @SerialName("price_id") val priceId: String
)
