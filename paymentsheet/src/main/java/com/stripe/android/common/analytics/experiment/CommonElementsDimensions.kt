package com.stripe.android.common.analytics.experiment

import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.ui.core.Amount
import com.stripe.android.utils.filterNotNullValues

internal data class CommonElementsDimensions(
    val amount: Amount?,
    val orderedPaymentMethods: List<String>,
    val mobileSdkVersion: String,
    val isGooglePayAvailable: Boolean,
    val isLinkDisplayed: Boolean,
    val liveMode: Boolean?,
) {
    companion object {
        fun create(paymentMethodMetadata: PaymentMethodMetadata): CommonElementsDimensions {
            return CommonElementsDimensions(
                orderedPaymentMethods = paymentMethodMetadata.sortedSupportedPaymentMethods().map { it.code },
                mobileSdkVersion = StripeSdkVersion.VERSION_NAME,
                isGooglePayAvailable = paymentMethodMetadata.isGooglePayReady,
                isLinkDisplayed = paymentMethodMetadata.linkState != null,
                amount = paymentMethodMetadata.amount(),
                liveMode = paymentMethodMetadata.stripeIntent?.isLiveMode, // TODO: use live mode provider
            )
        }
    }

    // TODO: check dimensions names against what buyer xp uses on web to make sure they match generally.
    fun toDimensions(): Map<String, String> = mapOf(
        "sdk_platform" to "android",
        "amount" to amount?.value,
        "currency" to amount?.currencyCode,
        "payment_method_types_including_wallets" to orderedPaymentMethods.plus {
            if (isGooglePayAvailable) {
                "google_pay"
            } else {
                null
            }
        }.plus {
            if (isLinkDisplayed) {
                "link"
            } else {
                null
            }
        }.joinToString { "," },
        "mobile_sdk_version" to mobileSdkVersion,
        "is_google_pay_available" to isGooglePayAvailable,
        "link_displayed" to isLinkDisplayed,
        "livemode" to liveMode,
        "buyer_region" to TODO(),
    ).filterNotNullValues().mapValues { it.toString() }
}