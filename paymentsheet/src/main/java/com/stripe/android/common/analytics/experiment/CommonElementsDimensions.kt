package com.stripe.android.common.analytics.experiment

import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.utils.filterNotNullValues
import kotlin.collections.joinToString
import kotlin.collections.plus

internal object CommonElementsDimensions {
    fun getDimensions(paymentMethodMetadata: PaymentMethodMetadata): Map<String, String> {
        val amount = paymentMethodMetadata.amount()
        val paymentMethodTypes = paymentMethodMetadata.sortedSupportedPaymentMethods().map { it.code }
        val isGooglePayAvailable = paymentMethodMetadata.isGooglePayReady
        val isLinkDisplayed = paymentMethodMetadata.linkState != null

        return mapOf(
            "sdk_platform" to "android",
            "amount" to amount?.value,
            "currency" to amount?.currencyCode,
            "payment_method_types" to paymentMethodTypes.joinToString(","),
            "payment_method_types_including_wallets" to getPaymentMethodTypesPlusWallets(
                paymentMethodTypes,
                isGooglePayAvailable = isGooglePayAvailable,
                isLinkDisplayed = isLinkDisplayed,
            ).joinToString(","),
            "mobile_sdk_version" to StripeSdkVersion.VERSION_NAME,
            "is_google_pay_available" to isGooglePayAvailable,
            "link_displayed" to isLinkDisplayed,
            "livemode" to paymentMethodMetadata.stripeIntent.isLiveMode,
        ).filterNotNullValues().mapValues { entry -> entry.value.toString() }
    }

    private fun getPaymentMethodTypesPlusWallets(
        paymentMethodTypes: List<String>,
        isGooglePayAvailable: Boolean,
        isLinkDisplayed: Boolean,
    ): List<String> {
        return paymentMethodTypes.plus(
            if (isGooglePayAvailable) {
                "google_pay"
            } else {
                null
            }
        ).plus(
            if (isLinkDisplayed) {
                "link"
            } else {
                null
            }
        ).filterNotNull()
    }
}
