package com.stripe.android.common.analytics.experiment

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.analytics.EventReporter
import kotlin.collections.joinToString
import kotlin.collections.plus

internal object CommonElementsDimensions {
    fun getDimensions(
        paymentMethodMetadata: PaymentMethodMetadata,
        mode: EventReporter.Mode,
    ): Map<String, String> {
        val paymentMethodTypes = paymentMethodMetadata.sortedSupportedPaymentMethods().map { it.code }
        val isGooglePayAvailable = paymentMethodMetadata.isGooglePayReady
        val isLinkDisplayed = paymentMethodMetadata.linkState != null
        val integrationType = when (mode) {
            EventReporter.Mode.Complete -> "paymentsheet"
            EventReporter.Mode.Custom -> "flowcontroller"
            EventReporter.Mode.Embedded -> "embedded"
        }

        return mapOf(
            "displayed_payment_method_types" to paymentMethodTypes.joinToString(","),
            "displayed_payment_method_types_including_wallets" to getPaymentMethodTypesPlusWallets(
                paymentMethodTypes,
                isGooglePayAvailable = isGooglePayAvailable,
                isLinkDisplayed = isLinkDisplayed,
            ).joinToString(","),
            "in_app_elements_integration_type" to integrationType,
        )
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
