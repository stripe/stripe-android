package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ConfirmationTokenParams(
    val paymentMethodId: String? = null,
    val paymentMethodData: PaymentMethodCreateParams? = null,
    var returnUrl: String? = null,
    val setUpFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage? = null,
    val shipping: ConfirmPaymentIntentParams.Shipping? = null,
    val mandateDataParams: MandateDataParams? = null,
    val setAsDefaultPaymentMethod: Boolean? = null,
) : StripeParamsModel, Parcelable {
    override fun toParamMap(): Map<String, Any> {
        return buildMap {
            paymentMethodId?.let { put(PARAM_PAYMENT_METHOD, it) }
            paymentMethodData?.let { put(PARAM_PAYMENT_METHOD_DATA, it.toParamMap()) }
            returnUrl?.let { put(PARAM_RETURN_URL, it) }
            setUpFutureUsage?.let { put(PARAM_SETUP_FUTURE_USAGE, it.code) }
            shipping?.let { put(PARAM_SHIPPING, it.toParamMap()) }
            mandateDataParams?.let { put(PARAM_MANDATE_DATA, it.toParamMap()) }
            setAsDefaultPaymentMethod?.let { put(PARAM_SET_AS_DEFAULT_PAYMENT_METHOD, it) }
        }
    }

    private companion object {
        const val PARAM_PAYMENT_METHOD = "payment_method"
        const val PARAM_PAYMENT_METHOD_DATA = "payment_method_data"
        const val PARAM_RETURN_URL = "return_url"
        const val PARAM_SETUP_FUTURE_USAGE = "setup_future_usage"
        const val PARAM_SHIPPING = "shipping"
        const val PARAM_MANDATE_DATA = "mandate_data"
        const val PARAM_SET_AS_DEFAULT_PAYMENT_METHOD = "set_as_default_payment_method"
    }
}
