package com.stripe.android.model

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ConfirmationTokenClientContextParams(
    /** The mode of this intent, either "payment" or "setup" */
    val mode: String,

    /** Three-letter ISO currency code */
    val currency: String,

    /** Indicates how the payment method is intended to be used in the future */
    val setupFutureUsage: String? = null,

    /** Controls when the funds will be captured (payment mode only) */
    val captureMethod: String? = null,

    /** The payment method types for the intent */
    val paymentMethodTypes: List<String>? = null,

    /** The account (if any) for which the funds of the intent are intended */
    val onBehalfOf: String? = null,

    /** Configuration ID for the selected payment method configuration */
    val paymentMethodConfiguration: String? = null,

    /** Customer ID */
    val customer: String? = null,

    /** Payment method specific options as a dictionary */
    val paymentMethodOptions: PaymentMethodOptionsParams? = null
) : StripeParamsModel {
    override fun toParamMap(): Map<String, Any> {
        return buildMap {
            put(PARAM_MODE, mode)
            put(PARAM_CURRENCY, currency)
            setupFutureUsage?.let { put(PARAM_SETUP_FUTURE_USAGE, it) }
            captureMethod?.let { put(PARAM_CAPTURE_METHOD, it) }
            paymentMethodTypes?.let { put(PARAM_PAYMENT_METHOD_TYPES, it) }
            onBehalfOf?.let { put(PARAM_ON_BEHALF_OF, it) }
            paymentMethodConfiguration?.let { put(PARAM_PAYMENT_METHOD_CONFIGURATION, it) }
            customer?.let { put(PARAM_CUSTOMER, it) }
            paymentMethodOptions?.toParamMap()?.let {
                if (it.isNotEmpty()) {
                    put(PARAM_PAYMENT_METHOD_OPTIONS, it)
                }
            }
        }
    }

    private companion object {
        const val PARAM_MODE = "mode"
        const val PARAM_CURRENCY = "currency"
        const val PARAM_SETUP_FUTURE_USAGE = "setup_future_usage"
        const val PARAM_CAPTURE_METHOD = "capture_method"
        const val PARAM_PAYMENT_METHOD_TYPES = "payment_method_types"
        const val PARAM_ON_BEHALF_OF = "on_behalf_of"
        const val PARAM_PAYMENT_METHOD_CONFIGURATION = "payment_method_configuration"
        const val PARAM_CUSTOMER = "customer"
        const val PARAM_PAYMENT_METHOD_OPTIONS = "payment_method_options"
    }
}