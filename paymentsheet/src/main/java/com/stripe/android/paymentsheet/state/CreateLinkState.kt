package com.stripe.android.paymentsheet.state

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.toLoginState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.toPaymentSheetSaveConsentBehavior
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
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
    private val createLinkConfiguration: CreateLinkConfiguration,
    private val accountStatusProvider: LinkAccountStatusProvider,
    private val linkStore: LinkStore,
) : CreateLinkState {

    override suspend fun invoke(
        elementsSession: ElementsSession,
        configuration: CommonConfiguration,
        customer: CustomerRepository.CustomerInfo?,
        initializationMode: PaymentElementLoader.InitializationMode,
        linkAppearance: LinkAppearance?
    ): LinkState? {
        val linkConfig = createLinkConfiguration(
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
}
