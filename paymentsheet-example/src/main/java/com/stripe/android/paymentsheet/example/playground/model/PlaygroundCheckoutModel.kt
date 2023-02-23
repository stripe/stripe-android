package com.stripe.android.paymentsheet.example.playground.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.serialization.Serializable

enum class InitializationType(val value: String) {
    Normal("normal"),
    Deferred("deferred"),
}

enum class CheckoutMode(val value: String) {
    Setup("setup"),
    Payment("payment"),
    PaymentWithSetup("payment_with_setup")
}

enum class Shipping(val value: String) {
    On("on"),
    OnWithDefaults("on_with_defaults"),
    Off("off"),
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
    val initialization: String,
    val customer: String,
    val googlePay: Boolean,
    val currency: String,
    val merchantCountryCode: String,
    val mode: String,
    val shippingAddress: String,
    val setAutomaticPaymentMethods: Boolean,
    val setDelayedPaymentMethods: Boolean,
    val setDefaultBillingAddress: Boolean,
    val link: Boolean
)

enum class Toggle(val key: String, val default: Any) {
    Initialization("initialization", InitializationType.Normal.value),
    Customer("customer", CheckoutCustomer.Guest.value),
    Link("link", true),
    GooglePay("googlePayConfig", true),
    Currency("currency", CheckoutCurrency.USD.value),
    MerchantCountryCode("merchantCountry", "US"),
    Mode("mode", CheckoutMode.Payment.value),
    ShippingAddress("shippingAddress", Shipping.On.value),
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

    companion object {
        fun valueOf(value: String): CheckoutCustomer {
            val possibleValues = listOf(Guest, New, Returning, Snapshot)
            return possibleValues.firstOrNull { it.value == value } ?: WithId(value)
        }
    }
}

@Serializable
@Keep
data class CheckoutRequest(
    @SerializedName("initialization")
    val initialization: String,
    @SerializedName("customer")
    val customer: String,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("mode")
    val mode: String,
    @SerializedName("set_shipping_address")
    val set_shipping_address: Boolean,
    @SerializedName("automatic_payment_methods")
    val automatic_payment_methods: Boolean,
    @SerializedName("use_link")
    val use_link: Boolean,
    @SerializedName("merchant_country_code")
    val merchant_country_code: String,
    @SerializedName("supported_payment_methods")
    val supported_payment_methods: List<String>?
)

@Serializable
@Keep
data class CheckoutResponse(
    @SerializedName("publishableKey")
    val publishableKey: String,
    @SerializedName("intentClientSecret")
    val intentClientSecret: String,
    @SerializedName("customerId")
    val customerId: String? = null,
    @SerializedName("customerEphemeralKeySecret")
    val customerEphemeralKeySecret: String? = null,
    @SerializedName("paymentMethodTypes")
    val paymentMethodTypes: List<String>? = null,
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
