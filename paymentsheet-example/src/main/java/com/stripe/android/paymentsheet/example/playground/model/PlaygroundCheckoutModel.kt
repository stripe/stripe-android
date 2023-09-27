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
    val set_shipping_address: Boolean?,
    @SerialName("automatic_payment_methods")
    val automatic_payment_methods: Boolean?,
    @SerialName("use_link")
    val use_link: Boolean?,
    @SerialName("merchant_country_code")
    val merchant_country_code: String?,
    @SerialName("supported_payment_methods")
    val supported_payment_methods: List<String>?,
) {
    class Builder() {
        private var initialization: String? = null
        private var customer: String? = null
        private var currency: String? = null
        private var mode: String? = null
        private var set_shipping_address: Boolean? = null
        private var automatic_payment_methods: Boolean? = null
        private var use_link: Boolean? = null
        private var merchant_country_code: String? = null
        private var supported_payment_methods: List<String>? = null

        fun initialization(initialization: String?): Builder {
            this.initialization = initialization
            return this
        }

        fun customer(customer: String?): Builder {
            this.customer = customer
            return this
        }

        fun currency(currency: String?): Builder {
            this.currency = currency
            return this
        }

        fun mode(mode: String?): Builder {
            this.mode = mode
            return this
        }

        fun setShippingAddress(setShippingAddress: Boolean?): Builder {
            this.set_shipping_address = setShippingAddress
            return this
        }

        fun automaticPaymentMethods(automaticPaymentMethods: Boolean?): Builder {
            this.automatic_payment_methods = automaticPaymentMethods
            return this
        }

        fun useLink(useLink: Boolean?): Builder {
            this.use_link = useLink
            return this
        }

        fun merchantCountryCode(merchantCountryCode: String?): Builder {
            this.merchant_country_code = merchantCountryCode
            return this
        }

        fun supportedPaymentMethods(supportedPaymentMethods: List<String>?): Builder {
            this.supported_payment_methods = supportedPaymentMethods
            return this
        }

        fun build(): CheckoutRequest {
            return CheckoutRequest(
                initialization = initialization,
                customer = customer,
                currency = currency,
                mode = mode,
                set_shipping_address = set_shipping_address,
                automatic_payment_methods = automatic_payment_methods,
                use_link = use_link,
                merchant_country_code = merchant_country_code,
                supported_payment_methods = supported_payment_methods,
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
