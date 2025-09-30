package com.stripe.android.paymentsheet.state

import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.toPaymentSheetSaveConsentBehavior
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.Flag.ELEMENTS_MOBILE_FORCE_SETUP_FUTURE_USE_BEHAVIOR_AND_NEW_MANDATE_TEXT
import com.stripe.android.paymentelement.confirmation.utils.sellerBusinessName
import com.stripe.android.payments.financialconnections.GetFinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import javax.inject.Inject

internal interface CreateLinkConfiguration {
    suspend operator fun invoke(
        configuration: CommonConfiguration,
        customer: CustomerRepository.CustomerInfo?,
        elementsSession: ElementsSession,
        initializationMode: PaymentElementLoader.InitializationMode,
        linkAppearance: LinkAppearance?,
    ): LinkConfiguration?
}

internal class DefaultCreateLinkConfiguration @Inject constructor(
    private val retrieveCustomerEmail: RetrieveCustomerEmail,
    private val linkGateFactory: LinkGate.Factory,
) : CreateLinkConfiguration {

    @Suppress("LongMethod")
    override suspend fun invoke(
        configuration: CommonConfiguration,
        customer: CustomerRepository.CustomerInfo?,
        elementsSession: ElementsSession,
        initializationMode: PaymentElementLoader.InitializationMode,
        linkAppearance: LinkAppearance?,
    ): LinkConfiguration? {
        if (!configuration.link.shouldDisplay || !elementsSession.isLinkEnabled) {
            return null
        }

        val isCardBrandFilteringRequired =
            elementsSession.linkPassthroughModeEnabled &&
                configuration.cardBrandAcceptance != PaymentSheet.CardBrandAcceptance.All

        val cardBrandFilter =
            if (isCardBrandFilteringRequired) {
                PaymentSheetCardBrandFilter(configuration.cardBrandAcceptance)
            } else {
                DefaultCardBrandFilter
            }

        val shippingDetails = configuration.shippingDetails

        val customerPhone = if (shippingDetails?.isCheckboxSelected == true) {
            shippingDetails.phoneNumber
        } else {
            configuration.defaultBillingDetails?.phone
        }

        val customerEmail = retrieveCustomerEmail(
            configuration = configuration,
            customer = customer
        )

        val customerInfo = LinkConfiguration.CustomerInfo(
            name = configuration.defaultBillingDetails?.name,
            email = customerEmail,
            phone = customerPhone,
            billingCountryCode = configuration.defaultBillingDetails?.address?.country,
        )

        val cardBrandChoice = elementsSession.cardBrandChoice?.let { cardBrandChoice ->
            LinkConfiguration.CardBrandChoice(
                eligible = cardBrandChoice.eligible,
                preferredNetworks = cardBrandChoice.preferredNetworks,
            )
        }

        val linkConfiguration = LinkConfiguration(
            stripeIntent = elementsSession.stripeIntent,
            merchantName = configuration.merchantDisplayName,
            sellerBusinessName = initializationMode.sellerBusinessName,
            merchantCountryCode = elementsSession.merchantCountry,
            merchantLogoUrl = elementsSession.merchantLogoUrl,
            customerInfo = customerInfo,
            shippingDetails = shippingDetails?.takeIf { it.isCheckboxSelected == true },
            passthroughModeEnabled = elementsSession.linkPassthroughModeEnabled,
            cardBrandChoice = cardBrandChoice,
            cardBrandFilter = cardBrandFilter,
            financialConnectionsAvailability = GetFinancialConnectionsAvailability(elementsSession = elementsSession),
            flags = elementsSession.linkFlags,
            useAttestationEndpointsForLink = elementsSession.useAttestationEndpointsForLink,
            suppress2faModal = elementsSession.suppressLink2faModal,
            disableRuxInFlowController = elementsSession.disableRuxInFlowController,
            enableDisplayableDefaultValuesInEce = elementsSession.linkEnableDisplayableDefaultValuesInEce,
            linkSignUpOptInFeatureEnabled = elementsSession.linkSignUpOptInFeatureEnabled,
            linkSignUpOptInInitialValue = elementsSession.linkSignUpOptInInitialValue,
            elementsSessionId = elementsSession.elementsSessionId,
            initializationMode = initializationMode,
            linkMode = elementsSession.linkSettings?.linkMode,
            billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration,
            defaultBillingDetails = configuration.defaultBillingDetails,
            allowDefaultOptIn = elementsSession.allowLinkDefaultOptIn,
            googlePlacesApiKey = configuration.googlePlacesApiKey,
            collectMissingBillingDetailsForExistingPaymentMethods =
                configuration.link.collectMissingBillingDetailsForExistingPaymentMethods,
            allowUserEmailEdits = configuration.link.allowUserEmailEdits,
            allowLogOut = configuration.link.allowLogOut,
            skipWalletInFlowController = elementsSession.linkMobileSkipWalletInFlowController,
            customerId = elementsSession.customer?.session?.customerId,
            linkAppearance = linkAppearance,
            saveConsentBehavior = elementsSession.toPaymentSheetSaveConsentBehavior(),
            forceSetupFutureUseBehaviorAndNewMandate = elementsSession
                .flags[ELEMENTS_MOBILE_FORCE_SETUP_FUTURE_USE_BEHAVIOR_AND_NEW_MANDATE_TEXT] == true,
            linkSupportedPaymentMethodsOnboardingEnabled =
                elementsSession.linkSettings?.linkSupportedPaymentMethodsOnboardingEnabled.orEmpty(),
        )

        val useWebLink = !linkGateFactory.create(linkConfiguration).useNativeLink

        if (isCardBrandFilteringRequired && useWebLink) {
            // CBF isn't currently supported in the web flow.
            return null
        }

        val collectsExtraBillingDetails = configuration.billingDetailsCollectionConfiguration.collectsAnything
        if (collectsExtraBillingDetails && useWebLink) {
            // Extra billing details collection isn't currently supported in the web flow.
            return null
        }

        return linkConfiguration
    }
}
