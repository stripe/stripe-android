package com.stripe.android.paymentsheet.example.playground.model

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class InitializationType(val value: String) {
    Normal("Normal"),
    DeferredClientSideConfirmation("Deferred CSC"),
    DeferredServerSideConfirmation("Deferred SSC"),
    DeferredManualConfirmation("Deferred SSC + MC"),
    DeferredMultiprocessor("Deferred SSC + MP"),
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

enum class BillingCollectionMode(val value: String) {
    Auto("auto"),
    Never("never"),
    Always("always");

    val asBillingDetailsCollectionConfigurationMode: BillingDetailsCollectionConfiguration.CollectionMode
        get() = when (this) {
            Always -> BillingDetailsCollectionConfiguration.CollectionMode.Always
            Never -> BillingDetailsCollectionConfiguration.CollectionMode.Never
            Auto -> BillingDetailsCollectionConfiguration.CollectionMode.Automatic
        }

    val asBillingAddressCollectionConfigurationMode: BillingDetailsCollectionConfiguration.AddressCollectionMode
        get() = when (this) {
            Always -> BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            Never -> BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
            Auto -> BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
        }
}

data class CheckoutCurrency(val value: String) {
    companion object {
        val USD = CheckoutCurrency("usd")
        val EUR = CheckoutCurrency("eur")
        val AUD = CheckoutCurrency("aud")
        val GBP = CheckoutCurrency("gbp")
        val PLN = CheckoutCurrency("pln")
        val SEK = CheckoutCurrency("sek")
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
    val attachDefaults: Boolean,
    val collectName: String,
    val collectEmail: String,
    val collectPhone: String,
    val collectAddress: String,
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
    AttachDefaults("attachDefaults", false),
    CollectName("collectName", BillingCollectionMode.Auto.value),
    CollectEmail("collectEmail", BillingCollectionMode.Auto.value),
    CollectPhone("collectPhone", BillingCollectionMode.Auto.value),
    CollectAddress("collectAddress", BillingCollectionMode.Auto.value),
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
class CheckoutRequest private constructor(
    @SerialName("initialization")
    val initialization: String?,
    @SerialName("customer")
    val customer: String?,
    @SerialName("currency")
    val currency: String?,
    @SerialName("mode")
    val mode: String?,
    @SerialName("set_shipping_address")
    val setShippingAddress: Boolean?,
    @SerialName("automatic_payment_methods")
    val automaticPaymentMethods: Boolean?,
    @SerialName("use_link")
    val useLink: Boolean?,
    @SerialName("merchant_country_code")
    val merchantCountryCode: String?,
    @SerialName("supported_payment_methods")
    val supportedPaymentMethods: List<String>?,
) {
    class Builder {
        private var initialization: String? = null
        private var customer: String? = null
        private var currency: String? = null
        private var mode: String? = null
        private var setShippingAddress: Boolean? = null
        private var automaticPaymentMethods: Boolean? = null
        private var useLink: Boolean? = null
        private var merchantCountryCode: String? = null
        private var supportedPaymentMethods: List<String>? = null

        fun initialization(initialization: String?) = apply {
            this.initialization = initialization
        }

        fun customer(customer: String?) = apply {
            this.customer = customer
        }

        fun currency(currency: String?) = apply {
            this.currency = currency
        }

        fun mode(mode: String?) = apply {
            this.mode = mode
        }

        fun setShippingAddress(setShippingAddress: Boolean?) = apply {
            this.setShippingAddress = setShippingAddress
        }

        fun automaticPaymentMethods(automaticPaymentMethods: Boolean?) = apply {
            this.automaticPaymentMethods = automaticPaymentMethods
        }

        fun useLink(useLink: Boolean?) = apply {
            this.useLink = useLink
        }

        fun merchantCountryCode(merchantCountryCode: String?) = apply {
            this.merchantCountryCode = merchantCountryCode
        }

        fun supportedPaymentMethods(supportedPaymentMethods: List<String>?) = apply {
            this.supportedPaymentMethods = supportedPaymentMethods
        }

        fun build(): CheckoutRequest {
            return CheckoutRequest(
                initialization = initialization,
                customer = customer,
                currency = currency,
                mode = mode,
                setShippingAddress = setShippingAddress,
                automaticPaymentMethods = automaticPaymentMethods,
                useLink = useLink,
                merchantCountryCode = merchantCountryCode,
                supportedPaymentMethods = supportedPaymentMethods,
            )
        }
    }
}

@Serializable
data class CheckoutResponse(
    @SerialName("publishableKey")
    val publishableKey: String,
    @SerialName("intentClientSecret")
    val intentClientSecret: String,
    @SerialName("customerId")
    val customerId: String? = null,
    @SerialName("customerEphemeralKeySecret")
    val customerEphemeralKeySecret: String? = null,
    @SerialName("amount")
    val amount: Long,
    @SerialName("paymentMethodTypes")
    val paymentMethodTypes: String? = null,
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

@Serializable
data class ConfirmIntentRequest(
    @SerialName("client_secret")
    val clientSecret: String,
    @SerialName("payment_method_id")
    val paymentMethodId: String,
    @SerialName("should_save_payment_method")
    val shouldSavePaymentMethod: Boolean,
    @SerialName("merchant_country_code")
    val merchantCountryCode: String,
    @SerialName("mode")
    val mode: String,
    @SerialName("return_url")
    val returnUrl: String,
)

@Serializable
data class ConfirmIntentResponse(
    @SerialName("client_secret")
    val clientSecret: String,
)
