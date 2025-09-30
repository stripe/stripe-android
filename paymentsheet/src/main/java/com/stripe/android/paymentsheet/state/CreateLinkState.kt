package com.stripe.android.paymentsheet.state

import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.toLoginState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.toPaymentSheetSaveConsentBehavior
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.Flag.ELEMENTS_MOBILE_FORCE_SETUP_FUTURE_USE_BEHAVIOR_AND_NEW_MANDATE_TEXT
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.utils.sellerBusinessName
import com.stripe.android.payments.financialconnections.GetFinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import javax.inject.Inject

internal interface CreateLinkState {
    suspend operator fun invoke(
        elementsSession: ElementsSession,
        configuration: CommonConfiguration,
        customer: CustomerRepository.CustomerInfo?,
        initializationMode: PaymentElementLoader.InitializationMode,
        linkAppearance: LinkAppearance?
    ): LinkState?
}

internal class DefaultCreateLinkState @Inject constructor(
    private val retrieveCustomerEmail: RetrieveCustomerEmail,
    private val accountStatusProvider: LinkAccountStatusProvider,
    private val linkStore: LinkStore,
    private val linkGateFactory: LinkGate.Factory,
) : CreateLinkState {

    override suspend fun invoke(
        elementsSession: ElementsSession,
        configuration: CommonConfiguration,
        customer: CustomerRepository.CustomerInfo?,
        initializationMode: PaymentElementLoader.InitializationMode,
        linkAppearance: LinkAppearance?
    ): LinkState? {
        val linkConfig =
            createLinkConfiguration(
                configuration = configuration,
                customer = customer,
                elementsSession = elementsSession,
                initializationMode = initializationMode,
                linkAppearance = linkAppearance
            ) ?: return null
        return loadLinkState(
            configuration = configuration,
            linkConfiguration = linkConfig,
            elementsSession = elementsSession,
            linkSignUpDisabled = elementsSession.disableLinkSignup,
        )
    }

    private suspend fun loadLinkState(
        configuration: CommonConfiguration,
        linkConfiguration: LinkConfiguration,
        elementsSession: ElementsSession,
        linkSignUpDisabled: Boolean,
    ): LinkState {
        val accountStatus = accountStatusProvider(linkConfiguration)

        val loginState = accountStatus.toLoginState()

        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            intent = elementsSession.stripeIntent,
            paymentMethodSaveConsentBehavior = elementsSession.toPaymentSheetSaveConsentBehavior(),
            hasCustomerConfiguration = configuration.customer != null,
        )
        val hasUsedLink = linkStore.hasUsedLink()
        val signupToggleEnabled = elementsSession.linkSignUpOptInFeatureEnabled

        val linkSignupMode = when {
            // If signup toggle enabled, we show a future usage + link combined toggle
            signupToggleEnabled && !linkConfiguration.customerInfo.email.isNullOrBlank() -> {
                LinkSignupMode.InsteadOfSaveForFutureUse
            }
            // If inline signup is disabled or user has used Link, we don't show inline signup
            linkSignUpDisabled || hasUsedLink -> null
            // If inline signup and save for future use, we show it alongside save for future use
            isSaveForFutureUseValueChangeable -> LinkSignupMode.AlongsideSaveForFutureUse
            // If inline signup and save for future usage is not displayed, only show link signup
            else -> LinkSignupMode.InsteadOfSaveForFutureUse
        }

        return LinkState(
            configuration = linkConfiguration,
            loginState = loginState,
            signupMode = linkSignupMode.takeIf {
                val validFundingSource = linkConfiguration.stripeIntent.linkFundingSources
                    .contains(PaymentMethod.Type.Card.code)

                val notLoggedIn = accountStatus == AccountStatus.SignedOut

                validFundingSource && notLoggedIn
            },
        )
    }

    @Suppress("LongMethod")
    private suspend fun createLinkConfiguration(
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
