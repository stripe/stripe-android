package com.stripe.android.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.frauddetection.FraudDetectionData
import com.stripe.android.core.frauddetection.FraudDetectionDataParamsUtils
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SourceParams
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import javax.inject.Inject
import javax.inject.Named

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ConfirmationParamsFactory @Inject constructor(
    @Named(PRODUCT_USAGE) private val productUsageTokens: Set<String>,
) {
    private val fraudDetectionDataParamsUtils = FraudDetectionDataParamsUtils()

    fun create(
        params: Map<String, Any>,
        paymentMethodCreateParams: PaymentMethodCreateParams?,
        sourceParams: SourceParams?,
        fraudDetectionData: FraudDetectionData?,
    ): Map<String, *> {
        // Add payment_user_agent if the Payment Method or Source is being created on this call.
        val attributedParams = (params[ConfirmStripeIntentParams.PARAM_PAYMENT_METHOD_DATA] as? Map<*, *>)?.let {
            params.plus(
                ConfirmStripeIntentParams.PARAM_PAYMENT_METHOD_DATA to it.plus(
                    "payment_user_agent" to buildPaymentUserAgent(paymentMethodCreateParams?.attribution ?: emptySet())
                )
            )
        } ?: (params[ConfirmPaymentIntentParams.PARAM_SOURCE_DATA] as? Map<*, *>)?.let {
            params.plus(
                ConfirmPaymentIntentParams.PARAM_SOURCE_DATA to it.plus(
                    "payment_user_agent" to buildPaymentUserAgent(sourceParams?.attribution ?: emptySet())
                )
            )
        } ?: params

        return fraudDetectionDataParamsUtils.addFraudDetectionData(
            attributedParams,
            fraudDetectionData,
        )
    }

    internal fun buildPaymentUserAgent(attribution: Set<String>): String {
        return setOf("stripe-android/${StripeSdkVersion.VERSION_NAME}")
            .plus(productUsageTokens)
            .plus(attribution)
            .joinToString(";")
    }
}
