@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded

import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.common.model.containsVolatileDifferences
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.model.PaymentSelection
import javax.inject.Inject
import kotlin.collections.contains

internal fun interface EmbeddedSelectionChooser {
    fun choose(
        paymentMethodMetadata: PaymentMethodMetadata,
        paymentMethods: List<PaymentMethod>?,
        previousSelection: PaymentSelection?,
        newSelection: PaymentSelection?,
        previousConfiguration: EmbeddedPaymentElement.Configuration?,
        newConfiguration: EmbeddedPaymentElement.Configuration,
    ): PaymentSelection?
}

internal class DefaultEmbeddedSelectionChooser @Inject constructor() : EmbeddedSelectionChooser {
    override fun choose(
        paymentMethodMetadata: PaymentMethodMetadata,
        paymentMethods: List<PaymentMethod>?,
        previousSelection: PaymentSelection?,
        newSelection: PaymentSelection?,
        previousConfiguration: EmbeddedPaymentElement.Configuration?,
        newConfiguration: EmbeddedPaymentElement.Configuration,
    ): PaymentSelection? {
        return previousSelection?.takeIf { selection ->
            canUseSelection(
                paymentMethodMetadata = paymentMethodMetadata,
                paymentMethods = paymentMethods,
                selection = selection,
            ) && previousConfiguration?.asCommonConfiguration()?.containsVolatileDifferences(
                newConfiguration.asCommonConfiguration()
            ) != true
        } ?: newSelection
    }

    private fun canUseSelection(
        paymentMethodMetadata: PaymentMethodMetadata,
        paymentMethods: List<PaymentMethod>?,
        selection: PaymentSelection,
    ): Boolean {
        // The types that are allowed for this intent, as returned by the backend
        val allowedTypes = paymentMethodMetadata.supportedPaymentMethodTypes()

        return when (selection) {
            is PaymentSelection.New -> {
                val code = selection.paymentMethodCreateParams.typeCode
                code in allowedTypes
            }
            is PaymentSelection.Saved -> {
                val paymentMethod = selection.paymentMethod
                val code = paymentMethod.type?.code
                code in allowedTypes && paymentMethod in (paymentMethods ?: emptyList())
            }
            is PaymentSelection.GooglePay -> {
                paymentMethodMetadata.isGooglePayReady
            }
            is PaymentSelection.Link -> {
                paymentMethodMetadata.linkState != null
            }
            is PaymentSelection.ExternalPaymentMethod -> {
                paymentMethodMetadata.isExternalPaymentMethod(selection.type)
            }
        }
    }
}
