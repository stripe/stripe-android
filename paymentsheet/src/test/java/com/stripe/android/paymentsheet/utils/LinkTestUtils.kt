package com.stripe.android.paymentsheet.utils

import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal object LinkTestUtils {
    val LINK_SAVED_PAYMENT_DETAILS = LinkPaymentDetails.Saved(
        paymentDetails = ConsumerPaymentDetails.Passthrough(
            id = "csmrpd_123",
            last4 = "4242",
            paymentMethodId = "pm_123",
            billingAddress = ConsumerPaymentDetails.BillingAddress(
                name = null,
                line1 = null,
                line2 = null,
                locality = null,
                administrativeArea = null,
                countryCode = CountryCode.US,
                postalCode = "42424"
            )
        ),
        paymentMethodCreateParams = mock(),
    )

    val LINK_NEW_PAYMENT_DETAILS = LinkPaymentDetails.New(
        paymentDetails = ConsumerPaymentDetails.Card(
            id = "pm_123",
            last4 = "4242",
            expiryYear = 2024,
            expiryMonth = 4,
            brand = CardBrand.DinersClub,
            cvcCheck = CvcCheck.Fail,
            isDefault = false,
            networks = emptyList(),
            funding = "CREDIT",
            nickname = null,
            billingAddress = ConsumerPaymentDetails.BillingAddress(
                name = null,
                line1 = null,
                line2 = null,
                locality = null,
                administrativeArea = null,
                countryCode = CountryCode.US,
                postalCode = "42424"
            )
        ),
        paymentMethodCreateParams = mock(),
        originalParams = mock()
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
            enableDisplayableDefaultValuesInEce = false,
            linkAppearance = null,
            linkSignUpOptInFeatureEnabled = false,
            linkSignUpOptInInitialValue = false,
            skipWalletInFlowController = false,
            customerId = null,
            saveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
        )
    }
}
