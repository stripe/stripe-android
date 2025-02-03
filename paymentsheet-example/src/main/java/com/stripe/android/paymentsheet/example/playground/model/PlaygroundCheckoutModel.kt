package com.stripe.android.paymentsheet.example.playground.model

import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Suppress("TooManyFunctions")
class CheckoutRequest private constructor(
    @SerialName("initialization")
    val initialization: String?,
    @SerialName("customer")
    val customer: String?,
    @SerialName("customer_key_type")
    val customerKeyType: CustomerKeyType?,
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
    @SerialName("payment_method_configuration")
    val paymentMethodConfigurationId: String?,
    @SerialName("require_cvc_recollection")
    val requireCvcRecollection: Boolean?,
    @SerialName("customer_session_component_name")
    val customerSessionComponentName: String,
    @SerialName("customer_session_payment_method_save")
    val paymentMethodSaveFeature: FeatureState?,
    @SerialName("customer_session_payment_method_remove")
    val paymentMethodRemoveFeature: FeatureState?,
    @SerialName("customer_session_payment_method_remove_last")
    val paymentMethodRemoveLastFeature: FeatureState?,
    @SerialName("customer_session_payment_method_redisplay")
    val paymentMethodRedisplayFeature: FeatureState?,
    @SerialName("customer_session_payment_method_allow_redisplay_filters")
    val paymentMethodRedisplayFilters: List<AllowRedisplayFilter>?,
    @SerialName("customer_session_payment_method_save_allow_redisplay_override")
    val paymentMethodOverrideRedisplay: AllowRedisplayFilter?,
    @SerialName("customer_session_payment_method_set_as_default")
    val paymentMethodSetAsDefaultFeature: FeatureState?,
    @SerialName("customer_session_payment_method_sync_default")
    val paymentMethodSyncDefaultFeature: FeatureState?,
) {
    @Serializable
    enum class CustomerKeyType {
        @SerialName("customer_session")
        CustomerSession,

        @SerialName("legacy")
        Legacy
    }

    class Builder {
        private var initialization: String? = null
        private var customer: String? = null
        private var customerKeyType: CustomerKeyType? = null
        private var currency: String? = null
        private var mode: String? = null
        private var setShippingAddress: Boolean? = null
        private var automaticPaymentMethods: Boolean? = null
        private var useLink: Boolean? = null
        private var merchantCountryCode: String? = null
        private var supportedPaymentMethods: List<String>? = null
        private var paymentMethodConfigurationId: String? = null
        private var requireCvcRecollection: Boolean? = null
        private var paymentMethodSaveFeature: FeatureState = FeatureState.Enabled
        private var paymentMethodRemoveFeature: FeatureState = FeatureState.Enabled
        private var paymentMethodRemoveLastFeature: FeatureState = FeatureState.Enabled
        private var paymentMethodSetAsDefaultFeature: FeatureState = FeatureState.Disabled
        private var paymentMethodSyncDefaultFeature: FeatureState = FeatureState.Disabled
        private var paymentMethodRedisplayFeature: FeatureState = FeatureState.Enabled
        private var paymentMethodRedisplayFilters: List<AllowRedisplayFilter> = listOf(
            AllowRedisplayFilter.Unspecified,
            AllowRedisplayFilter.Limited,
            AllowRedisplayFilter.Always,
        )
        private var paymentMethodOverrideRedisplay: AllowRedisplayFilter? = null

        fun initialization(initialization: String?) = apply {
            this.initialization = initialization
        }

        fun customer(customer: String?) = apply {
            this.customer = customer
        }

        fun customerKeyType(customerKeyType: CustomerKeyType?) = apply {
            this.customerKeyType = customerKeyType
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

        fun paymentMethodConfigurationId(paymentMethodConfigurationId: String?) = apply {
            this.paymentMethodConfigurationId = paymentMethodConfigurationId
        }

        fun paymentMethodSaveFeature(state: FeatureState) {
            this.paymentMethodSaveFeature = state
        }

        fun paymentMethodRemoveFeature(state: FeatureState) {
            this.paymentMethodRemoveFeature = state
        }

        fun paymentMethodRemoveLastFeature(state: FeatureState) {
            this.paymentMethodRemoveLastFeature = state
        }

        fun paymentMethodSetAsDefaultFeature(state: FeatureState) {
            this.paymentMethodSetAsDefaultFeature = state
        }

        fun paymentMethodSyncDefaultFeature(state: FeatureState) {
            this.paymentMethodSyncDefaultFeature = state
        }

        fun paymentMethodRedisplayFeature(state: FeatureState) {
            this.paymentMethodRedisplayFeature = state
        }

        fun paymentMethodRedisplayFilters(filters: List<AllowRedisplayFilter>) {
            this.paymentMethodRedisplayFilters = filters
        }

        fun paymentMethodOverrideRedisplay(override: AllowRedisplayFilter?) {
            this.paymentMethodOverrideRedisplay = override
        }

        fun requireCvcRecollection(requireCvcRecollection: Boolean?) = apply {
            this.requireCvcRecollection = requireCvcRecollection
        }

        fun build(): CheckoutRequest {
            return CheckoutRequest(
                initialization = initialization,
                customer = customer,
                customerKeyType = customerKeyType,
                currency = currency,
                mode = mode,
                setShippingAddress = setShippingAddress,
                automaticPaymentMethods = automaticPaymentMethods,
                useLink = useLink,
                merchantCountryCode = merchantCountryCode,
                supportedPaymentMethods = supportedPaymentMethods,
                paymentMethodConfigurationId = paymentMethodConfigurationId,
                requireCvcRecollection = requireCvcRecollection,
                customerSessionComponentName = "mobile_payment_element",
                paymentMethodSaveFeature = paymentMethodSaveFeature,
                paymentMethodRemoveFeature = paymentMethodRemoveFeature,
                paymentMethodSetAsDefaultFeature = paymentMethodSetAsDefaultFeature,
                paymentMethodSyncDefaultFeature = paymentMethodSyncDefaultFeature,
                paymentMethodRemoveLastFeature = paymentMethodRemoveLastFeature,
                paymentMethodRedisplayFeature = paymentMethodRedisplayFeature,
                paymentMethodRedisplayFilters = paymentMethodRedisplayFilters,
                paymentMethodOverrideRedisplay = paymentMethodOverrideRedisplay,
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
    @SerialName("customerSessionClientSecret")
    val customerSessionClientSecret: String? = null,
    @SerialName("amount")
    val amount: Long,
    @SerialName("paymentMethodTypes")
    val paymentMethodTypes: String? = null,
) {
    @OptIn(ExperimentalCustomerSessionApi::class)
    fun makeCustomerConfig(
        customerKeyType: CheckoutRequest.CustomerKeyType?
    ) = customerId?.let { id ->
        when (customerKeyType) {
            CheckoutRequest.CustomerKeyType.CustomerSession -> {
                customerSessionClientSecret?.let { clientSecret ->
                    PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = id,
                        clientSecret = clientSecret,
                    )
                }
            }
            CheckoutRequest.CustomerKeyType.Legacy,
            null -> {
                customerEphemeralKeySecret?.let { ephemeralKeySecret ->
                    PaymentSheet.CustomerConfiguration(
                        id = id,
                        ephemeralKeySecret = ephemeralKeySecret
                    )
                }
            }
        }
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
