package com.stripe.android.paymentsheet.example.playground.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SharedPaymentTokenCreateSessionRequest(
    @SerialName("customerId")
    val customerId: String?,
    @SerialName("isMobile")
    val isMobile: Boolean,
)

@Serializable
class SharedPaymentTokenCreateSessionResponse(
    @SerialName("customerId")
    val customerId: String,
    @SerialName("customerSessionClientSecret")
    val customerSessionClientSecret: String,
)

@Serializable
class SharedPaymentTokenCreateIntentRequest(
    @SerialName("customerId")
    val customerId: String,
    @SerialName("paymentMethod")
    val paymentMethod: String,
    @SerialName("shipping")
    val shipping: String?,
)

@Serializable
class SharedPaymentTokenCreateIntentResponse(
    @SerialName("paymentIntent")
    val paymentIntentId: String,
    @SerialName("spt")
    val sharedPaymentTokenId: String,
    @SerialName("requiresAction")
    val requiresAction: Boolean?,
    @SerialName("nextActionValue")
    val nextActionValue: String?,
)
