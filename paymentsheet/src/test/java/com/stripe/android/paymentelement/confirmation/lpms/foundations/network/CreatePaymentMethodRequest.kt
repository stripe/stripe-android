package com.stripe.android.paymentelement.confirmation.lpms.foundations.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CreatePaymentMethodRequest(
    @SerialName("account")
    val country: MerchantCountry,
) {
    @Serializable
    internal data class Response(
        @SerialName("payment_method_id")
        val paymentMethodId: String,
        @SerialName("customer_id")
        val customerId: String,
    )
}
