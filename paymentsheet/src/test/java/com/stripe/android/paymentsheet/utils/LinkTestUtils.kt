package com.stripe.android.paymentsheet.utils

import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.testing.PaymentMethodFactory
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal object LinkTestUtils {
    val LINK_SAVED_PAYMENT_DETAILS = LinkPaymentDetails.Saved(
        paymentMethod = PaymentMethodFactory.card(id = "pm_123"),
    )

    fun createLinkConfiguration(
        cardBrandChoice: LinkConfiguration.CardBrandChoice? = null,
        cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
    ): LinkConfiguration {
        return LinkConfiguration(
            stripeIntent = mock {
                on { linkFundingSources } doReturn listOf(
                    PaymentMethod.Type.Card.code
                )
            },
            customerInfo = LinkConfiguration.CustomerInfo(null, null, null, null),
            flags = mapOf(),
            merchantName = "Test merchant inc.",
            sellerBusinessName = null,
            merchantCountryCode = "US",
            merchantLogoUrl = null,
            passthroughModeEnabled = false,
            cardBrandChoice = cardBrandChoice,
            cardBrandFilter = cardBrandFilter,
            financialConnectionsAvailability = FinancialConnectionsAvailability.Full,
            shippingDetails = null,
            useAttestationEndpointsForLink = false,
            suppress2faModal = false,
            initializationMode = PaymentSheetFixtures.INITIALIZATION_MODE_PAYMENT_INTENT,
            elementsSessionId = "session_1234",
            linkMode = LinkMode.LinkPaymentMethod,
            allowDefaultOptIn = false,
            disableRuxInFlowController = false,
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            defaultBillingDetails = null,
            collectMissingBillingDetailsForExistingPaymentMethods = true,
            allowUserEmailEdits = true,
            allowLogOut = true,
            enableDisplayableDefaultValuesInEce = false,
            linkAppearance = null,
            linkSignUpOptInFeatureEnabled = false,
            linkSignUpOptInInitialValue = false,
            skipWalletInFlowController = false,
            customerId = null,
            saveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            forceSetupFutureUseBehaviorAndNewMandate = false,
            linkSupportedPaymentMethodsOnboardingEnabled = listOf("CARD"),
        )
    }
}
