package com.stripe.android.financialconnections.example.data.model

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class PaymentIntentBody(
    @SerialName("flow")
    val flow: String? = null,
    @SerialName("country")
    val country: String? = "US",
    @SerialName("customer_id")
    val customerId: String? = null,
    @SerialName("supported_payment_methods")
    val supportedPaymentMethods: String? = null,
    @SerialName("custom_pk")
    val publishableKey: String? = null,
    @SerialName("custom_sk")
    val secretKey: String? = null,
    @SerialName("permissions")
    val permissions: String? = null,
    @SerialName("customer_email")
    val customerEmail: String? = null,
    @SerialName("test_environment")
    val testEnvironment: String? = null,
    @SerialName("test_mode")
    val testMode: Boolean? = null,
    @SerialName("stripe_account_id")
    val stripeAccountId: String? = null,
    @SerialName("link_mode")
    val linkMode: String? = null,
    @SerialName("relink_authorization")
    val relinkAuthorization: String? = null,
)
