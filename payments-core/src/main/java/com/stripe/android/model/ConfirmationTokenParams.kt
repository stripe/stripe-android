package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ConfirmationTokenParams(
    val paymentMethodId: String? = null,
    val paymentMethodData: PaymentMethodCreateParams? = null,
    val returnUrl: String? = null,
    val setUpFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage? = null,
    val shipping: ConfirmPaymentIntentParams.Shipping? = null,
    val mandateDataParams: MandateDataParams? = null,
    val setAsDefaultPaymentMethod: Boolean? = null,
    val cvc: String? = null,
    val clientContext: ConfirmationTokenClientContextParams? = null,
) : StripeParamsModel, Parcelable {
    override fun toParamMap(): Map<String, Any> {
        return buildMap {
            paymentMethodId?.let { put(PARAM_PAYMENT_METHOD, it) }
            paymentMethodData?.let { put(PARAM_PAYMENT_METHOD_DATA, it.toParamMap()) }
            returnUrl?.let { put(PARAM_RETURN_URL, it) }
            putNonEmptySfu(setUpFutureUsage)
            shipping?.let { put(PARAM_SHIPPING, it.toParamMap()) }
            mandateDataParams?.let { put(PARAM_MANDATE_DATA, it.toParamMap()) }
            setAsDefaultPaymentMethod?.let { put(PARAM_SET_AS_DEFAULT_PAYMENT_METHOD, it) }
            cvc?.let { put(PARAM_PAYMENT_METHOD_OPTIONS, mapOf("card" to mapOf("cvc" to it))) }
            clientContext?.let { put(PARAM_CLIENT_CONTEXT, it.toParamMap()) }
        }
    }

    private companion object {
        const val PARAM_PAYMENT_METHOD = "payment_method"
        const val PARAM_PAYMENT_METHOD_DATA = "payment_method_data"
        const val PARAM_RETURN_URL = "return_url"
        const val PARAM_SHIPPING = "shipping"
        const val PARAM_MANDATE_DATA = "mandate_data"
        const val PARAM_SET_AS_DEFAULT_PAYMENT_METHOD = "set_as_default_payment_method"
        const val PARAM_PAYMENT_METHOD_OPTIONS = "payment_method_options"
        const val PARAM_CLIENT_CONTEXT = "client_context"
    }
}

private const val PARAM_SETUP_FUTURE_USAGE = "setup_future_usage"
fun MutableMap<String, Any>.putNonEmptySfu(sfu: ConfirmPaymentIntentParams.SetupFutureUsage?) {
    sfu.takeIf {
        // Empty values are an attempt to unset a parameter;
        // however, setup_future_usage cannot be unset.
        it != null && it != ConfirmPaymentIntentParams.SetupFutureUsage.Blank
    }?.let {
        put(PARAM_SETUP_FUTURE_USAGE, it.code)
    }
}