package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.lpmfoundations.paymentmethod.link.LinkInlineConfiguration
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.ExternalPaymentMethodSpec
import com.stripe.android.ui.core.elements.LpmSerializer
import com.stripe.android.ui.core.elements.SharedDataSpec

internal object PaymentMethodMetadataFactory {
    fun create(
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration(),
        allowsDelayedPaymentMethods: Boolean = true,
        allowsPaymentMethodsRequiringShippingAddress: Boolean = false,
        financialConnectionsAvailable: Boolean = true,
        paymentMethodOrder: List<String> = emptyList(),
        shippingDetails: AddressDetails? = null,
        cbcEligibility: CardBrandChoiceEligibility = CardBrandChoiceEligibility.Ineligible,
        hasCustomerConfiguration: Boolean = false,
        sharedDataSpecs: List<SharedDataSpec> = createSharedDataSpecs(),
        externalPaymentMethodSpecs: List<ExternalPaymentMethodSpec> = emptyList(),
        isGooglePayReady: Boolean = false,
        paymentMethodSaveConsentBehavior: PaymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
        linkInlineConfiguration: LinkInlineConfiguration? = null,
        linkMode: LinkMode? = LinkMode.LinkPaymentMethod,
    ): PaymentMethodMetadata {
        return PaymentMethodMetadata(
            stripeIntent = stripeIntent,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
            allowsPaymentMethodsRequiringShippingAddress = allowsPaymentMethodsRequiringShippingAddress,
            financialConnectionsAvailable = financialConnectionsAvailable,
            paymentMethodOrder = paymentMethodOrder,
            cbcEligibility = cbcEligibility,
            merchantName = PaymentSheetFixtures.MERCHANT_DISPLAY_NAME,
            defaultBillingDetails = PaymentSheet.BillingDetails(),
            shippingDetails = shippingDetails,
            hasCustomerConfiguration = hasCustomerConfiguration,
            sharedDataSpecs = sharedDataSpecs,
            paymentMethodSaveConsentBehavior = paymentMethodSaveConsentBehavior,
            externalPaymentMethodSpecs = externalPaymentMethodSpecs,
            isGooglePayReady = isGooglePayReady,
            linkInlineConfiguration = linkInlineConfiguration,
            linkMode = linkMode,
        )
    }

    private fun createSharedDataSpecs(): List<SharedDataSpec> {
        val inputStream = PaymentMethodMetadataFactory::class.java.classLoader!!.getResourceAsStream("lpms.json")
        val specsString = inputStream.bufferedReader().use { it.readText() }
        return LpmSerializer.deserializeList(specsString).getOrThrow()
    }
}
