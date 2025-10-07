package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.common.model.CommonConfiguration
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
import com.stripe.android.model.LinkDisabledReason
import com.stripe.android.model.LinkSignupDisabledReason
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.utils.sellerBusinessName
import com.stripe.android.payments.financialconnections.GetFinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

internal interface CreateLinkState {
    suspend operator fun invoke(
        elementsSession: ElementsSession,
        configuration: CommonConfiguration,
        customer: CustomerRepository.CustomerInfo?,
        initializationMode: PaymentElementLoader.InitializationMode,
    ): LinkStateResult
}

internal sealed interface LinkSignupModeResult : Parcelable {
    val mode: LinkSignupMode?
    val disabledReasons: List<LinkSignupDisabledReason>?

    @Parcelize
    data object AlreadyRegistered : LinkSignupModeResult {
        override val mode: LinkSignupMode? get() = null
        override val disabledReasons: List<LinkSignupDisabledReason>? get() = null
    }

    @Parcelize
    data class Enabled(override val mode: LinkSignupMode) : LinkSignupModeResult {
        override val disabledReasons: List<LinkSignupDisabledReason>? get() = null
    }

    @Parcelize
    data class Disabled(override val disabledReasons: List<LinkSignupDisabledReason>) : LinkSignupModeResult {
        override val mode: LinkSignupMode? get() = null
    }
}

internal class DefaultCreateLinkState @Inject constructor(
    private val accountStatusProvider: LinkAccountStatusProvider,
    private val retrieveCustomerEmail: RetrieveCustomerEmail,
    private val linkStore: LinkStore,
    private val linkGateFactory: LinkGate.Factory,
) : CreateLinkState {

    override suspend fun invoke(
        elementsSession: ElementsSession,
        configuration: CommonConfiguration,
        customer: CustomerRepository.CustomerInfo?,
        initializationMode: PaymentElementLoader.InitializationMode,
    ): LinkStateResult {
        val linkDisabledReasons = getLinkDisabledReasons(
            elementsSession = elementsSession,
            configuration = configuration
        )

        val isLinkDisabled = linkDisabledReasons.isNotEmpty()
        if (isLinkDisabled) {
            return LinkDisabledState(linkDisabledReasons)
        }

        val linkConfiguration = createLinkConfigurationWithoutValidation(
            configuration = configuration,
            customer = customer,
            elementsSession = elementsSession,
            initializationMode = initializationMode,
        )
        val accountStatus = accountStatusProvider(linkConfiguration)
        val loginState = accountStatus.toLoginState()
        return LinkState(
            configuration = linkConfiguration,
            loginState = loginState,
            signupModeResult = getLinkSignupMode(
                accountStatus = accountStatus,
                elementsSession = elementsSession,
                configuration = configuration,
                linkConfiguration = linkConfiguration,
            )
        )
    }

    private fun getLinkDisabledReasons(
        elementsSession: ElementsSession,
        configuration: CommonConfiguration,
    ): List<LinkDisabledReason> = buildList {
        if (!elementsSession.isLinkEnabled) {
            add(LinkDisabledReason.NotSupportedInElementsSession)
        }

        if (!configuration.link.shouldDisplay) {
            add(LinkDisabledReason.LinkConfiguration)
        }

        val useWebLink = !linkGateFactory.create(elementsSession).useNativeLink
        val isCardBrandFilteringRequired =
            elementsSession.linkPassthroughModeEnabled &&
                configuration.cardBrandAcceptance != PaymentSheet.CardBrandAcceptance.All
        if (isCardBrandFilteringRequired && useWebLink) {
            // CBF isn't currently supported in the web flow.
            add(LinkDisabledReason.CardBrandFiltering)
        }

        val collectsExtraBillingDetails = configuration.billingDetailsCollectionConfiguration.collectsAnything
        if (collectsExtraBillingDetails && useWebLink) {
            // Extra billing details collection isn't currently supported in the web flow.
            add(LinkDisabledReason.BillingDetailsCollection)
        }
    }

    private fun getLinkSignupDisabledReasons(
        elementsSession: ElementsSession,
        linkConfiguration: LinkConfiguration,
    ): List<LinkSignupDisabledReason> = buildList {
        val validFundingSource = elementsSession.stripeIntent.linkFundingSources.contains(PaymentMethod.Type.Card.code)
        if (!validFundingSource) {
            add(LinkSignupDisabledReason.LinkCardNotSupported)
        }

        if (elementsSession.disableLinkSignup && !elementsSession.linkSignUpOptInFeatureEnabled) {
            add(LinkSignupDisabledReason.DisabledInElementsSession)
        }

        if (elementsSession.linkSignUpOptInFeatureEnabled && linkConfiguration.customerInfo.email.isNullOrBlank()) {
            add(LinkSignupDisabledReason.SignupOptInFeatureNoEmailProvided)
        }

        if (linkStore.hasUsedLink()) {
            add(LinkSignupDisabledReason.LinkUsedBefore)
        }
    }

    private fun getLinkSignupMode(
        accountStatus: AccountStatus,
        elementsSession: ElementsSession,
        configuration: CommonConfiguration,
        linkConfiguration: LinkConfiguration,
    ): LinkSignupModeResult {
        if (accountStatus != AccountStatus.SignedOut) {
            return LinkSignupModeResult.AlreadyRegistered
        }

        val disabledReasons = getLinkSignupDisabledReasons(
            elementsSession = elementsSession,
            linkConfiguration = linkConfiguration,
        )

        if (disabledReasons.isNotEmpty()) {
            return LinkSignupModeResult.Disabled(disabledReasons)
        }

        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            intent = elementsSession.stripeIntent,
            paymentMethodSaveConsentBehavior = elementsSession.toPaymentSheetSaveConsentBehavior(),
            hasCustomerConfiguration = configuration.customer != null,
        )
        val signupMode = when {
            // If signup toggle enabled, we show a future usage + link combined toggle
            elementsSession.linkSignUpOptInFeatureEnabled && !linkConfiguration.customerInfo.email.isNullOrBlank() -> {
                LinkSignupMode.InsteadOfSaveForFutureUse
            }
            // If inline signup and save for future use, we show it alongside save for future use
            isSaveForFutureUseValueChangeable ->
                LinkSignupMode.AlongsideSaveForFutureUse
            // If inline signup and save for future usage is not displayed, only show link signup
            else ->
                LinkSignupMode.InsteadOfSaveForFutureUse
        }
        return LinkSignupModeResult.Enabled(signupMode)
    }

    private suspend fun createLinkConfigurationWithoutValidation(
        configuration: CommonConfiguration,
        customer: CustomerRepository.CustomerInfo?,
        elementsSession: ElementsSession,
        initializationMode: PaymentElementLoader.InitializationMode,
    ): LinkConfiguration {
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

        return LinkConfiguration(
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
            linkAppearance = configuration.linkAppearance,
            saveConsentBehavior = elementsSession.toPaymentSheetSaveConsentBehavior(),
            forceSetupFutureUseBehaviorAndNewMandate = elementsSession
                .flags[ELEMENTS_MOBILE_FORCE_SETUP_FUTURE_USE_BEHAVIOR_AND_NEW_MANDATE_TEXT] == true,
            linkSupportedPaymentMethodsOnboardingEnabled =
            elementsSession.linkSettings?.linkSupportedPaymentMethodsOnboardingEnabled.orEmpty(),
        )
    }
}
