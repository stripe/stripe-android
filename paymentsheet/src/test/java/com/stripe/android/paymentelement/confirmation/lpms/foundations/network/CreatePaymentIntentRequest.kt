package com.stripe.android.paymentelement.confirmation.lpms.foundations.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CreatePaymentIntentRequest(
    @SerialName("create_params")
    val createParams: CreateParams,
    @SerialName("account")
    val country: MerchantCountry,
    @SerialName("version")
    val version: String?,
) {
    @Serializable
    data class CreateParams(
        @SerialName("amount")
        val amount: Int,
        @SerialName("currency")
        val currency: String,
        @SerialName("confirm")
        val confirm: Boolean,
        @SerialName("payment_method_types")
        val paymentMethodTypes: List<String>,
        @SerialName("payment_method")
        val paymentMethodId: String?,
        @SerialName("setup_future_usage")
        val setupFutureUsage: SetupFutureUsage?,
    ) {
        @Serializable
        enum class SetupFutureUsage {
            @SerialName("off_session")
            OffSession,
        }
    }

    @Serializable
    internal data class Response(
        @SerialName("intent")
        val intentId: String,
        @SerialName("secret")
        val intentClientSecret: String,
        @SerialName("status")
        val status: String,
        @SerialName("publishable_key")
        val publishableKey: String,
    )
}
