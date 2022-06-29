package com.stripe.android.paymentsheet.example.playground.model

import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.serialization.Serializable

enum class CheckoutMode(val value: String) {
    Setup("setup"),
    Payment("payment"),
    PaymentWithSetup("payment_with_setup")
}

data class CheckoutCurrency(val value: String) {
    companion object {
        val USD = CheckoutCurrency("usd")
        val EUR = CheckoutCurrency("eur")
        val AUD = CheckoutCurrency("aud")
        val GBP = CheckoutCurrency("gbp")
    }
}

data class SavedToggles(
    val customer: String,
    val googlePay: Boolean,
    val currency: String,
    val merchantCountryCode: String,
    val mode: String,
    val setShippingAddress: Boolean,
    val setAutomaticPaymentMethods: Boolean,
    val setDelayedPaymentMethods: Boolean,
    val setDefaultBillingAddress: Boolean,
    val link: Boolean
)

enum class Toggle(val key: String, val default: Any) {
    Customer("customer", CheckoutCustomer.Guest.value),
    Link("link", true),
    GooglePay("googlePayConfig", true),
    Currency("currency", CheckoutCurrency.USD.value),
    MerchantCountryCode("merchantCountry", "US"),
    Mode("mode", CheckoutMode.Payment.value),
    SetShippingAddress("setShippingAddress", true),
    SetDefaultBillingAddress("setDefaultBillingAddress", true),
    SetAutomaticPaymentMethods("setAutomaticPaymentMethods", true),
    SetDelayedPaymentMethods("setDelayedPaymentMethods", false)
}

sealed class CheckoutCustomer(val value: String) {
    object Guest : CheckoutCustomer("guest")
    object New : CheckoutCustomer("new")
    object Returning : CheckoutCustomer("returning")
    object Snapshot : CheckoutCustomer("snapshot")
    data class WithId(val customerId: String) : CheckoutCustomer(customerId)
}

@Serializable
data class CheckoutRequest(
    val customer: String,
    val currency: String,
    val mode: String,
    val set_shipping_address: Boolean,
    val automatic_payment_methods: Boolean,
    val use_link: Boolean,
    val merchant_country_code: String
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
