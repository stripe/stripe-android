package com.stripe.android.paymentsheet.example.playground.model

import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.serialization.Serializable

enum class CheckoutMode(val value: String) {
    Setup("setup"),
    Payment("payment"),
    PaymentWithSetup("payment_with_setup")
}

enum class CheckoutCurrency(val value: String) {
    USD("usd"),
    EUR("eur")
}

sealed class CheckoutCustomer(val value: String) {
    object Guest : CheckoutCustomer("guest")
    object New : CheckoutCustomer("new")
    object Returning : CheckoutCustomer("returning")
    data class WithId(val customerId: String) : CheckoutCustomer(customerId)
}

@Serializable
data class CheckoutRequest(
    val customer: String,
    val currency: String,
    val mode: String,
    val set_shipping_address: Boolean,
    val automatic_payment_methods: Boolean
)

@Serializable
data class CheckoutResponse(
    val publishableKey: String,
    val intentClientSecret: String,
    val customerId: String? = null,
    val customerEphemeralKeySecret: String? = null
) {
    fun makeCustomerConfig() =
        if (customerId != null && customerEphemeralKeySecret != null) {
            PaymentSheet.CustomerConfiguration(
                id = customerId,
                ephemeralKeySecret = customerEphemeralKeySecret
            )
        } else {
            null
        }
}
