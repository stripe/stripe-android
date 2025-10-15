package com.stripe.android.model

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ConfirmationTokenClientContextParams(
    /** The mode of this intent, either "payment" or "setup" */
    val mode: String,

    /** Three-letter ISO currency code */
    val currency: String?,

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
    val paymentMethodOptions: PaymentMethodOptionsParams? = null,

    /** Indicates whether the customer is required to re-enter their CVC code */
    val requireCvcRecollection: Boolean? = null,
) : StripeParamsModel {
    override fun toParamMap(): Map<String, Any> {
        return buildMap {
            put(PARAM_MODE, mode)
            currency?.let { put(PARAM_CURRENCY, it) }
            setupFutureUsage?.let { put(PARAM_SETUP_FUTURE_USAGE, it) }
            captureMethod?.let { put(PARAM_CAPTURE_METHOD, it) }
            paymentMethodTypes?.let {
                if (it.isNotEmpty()) {
                    // Empty values are an attempt to unset a parameter;
                    // however, paymentMethodTypes cannot be unset.
                    put(PARAM_PAYMENT_METHOD_TYPES, it)
                }
            }
            onBehalfOf?.let { put(PARAM_ON_BEHALF_OF, it) }
            paymentMethodConfiguration?.let { put(PARAM_PAYMENT_METHOD_CONFIGURATION, it) }
            customer?.let { put(PARAM_CUSTOMER, it) }
            preparePaymentMethodOptionsParamMap(
                paymentMethodOptions,
                requireCvcRecollection,
            )?.let {
                put(PARAM_PAYMENT_METHOD_OPTIONS, it)
            }
        }
    }

    /**
     * https://stripe.sourcegraphcloud.com/stripe-internal/pay-server/-/blob/lib/elements/api/client_context/param.rb
     */
    private fun preparePaymentMethodOptionsParamMap(
        paymentMethodOptions: PaymentMethodOptionsParams?,
        requireCvcRecollection: Boolean?,
    ): Map<String, Any>? {
        if (paymentMethodOptions?.type == null) return null

        val sfu = paymentMethodOptions.setupFutureUsage()
        val valueMap = buildMap {
            sfu.takeIf {
                // Empty values are an attempt to unset a parameter;
                // however, setup_future_usage cannot be unset.
                it != null && it != ConfirmPaymentIntentParams.SetupFutureUsage.Blank
            }?.let {
                put(PARAM_SETUP_FUTURE_USAGE, it.code)
            }

            if (paymentMethodOptions.type == PaymentMethod.Type.Card) {
                requireCvcRecollection?.let {
                    put(PARAM_REQUIRE_CVC_RECOLLECTION, it)
                }
            }
        }.takeIf { it.isNotEmpty() }

        return valueMap?.let {
            mapOf(paymentMethodOptions.type.code to it)
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
        const val PARAM_REQUIRE_CVC_RECOLLECTION = "require_cvc_recollection"
    }
}
