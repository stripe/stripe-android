package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.NoLinkAccountFoundException
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuthResult
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.PaymentSheet.LinkSignupOptInState
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.billingDetails
import com.stripe.android.paymentsheet.ui.SignupToLinkToggleInteractor
import javax.inject.Inject

internal class SignupForLink @Inject constructor(
    private val linkAccountHolder: LinkAccountHolder,
    private val linkHandler: LinkHandler,
    private val signupToLinkToggleInteractor: SignupToLinkToggleInteractor,
    private val eventReporter: EventReporter
) {

    suspend operator fun invoke(
        linkConfiguration: LinkConfiguration?,
        paymentSelection: PaymentSelection?
    ) {
        runCatching {
            val billing = paymentSelection?.billingDetails
            val email = billing?.email
            // Link is disabled
            if (linkConfiguration == null) return
            // Signup toggle wasn't shown
            if (signupToLinkToggleInteractor.state.value == LinkSignupOptInState.Hidden) return
            // Signup is disabled from backend
            if (linkConfiguration.enableNewUserSignupAPI.not()) return
            // Signup toggle is off
            if (signupToLinkToggleInteractor.toggleValue.value.not()) return
            // Link account already exists
            if (linkAccountHolder.linkAccountInfo.value.account != null) return
            // No email provided
            if (email == null) return

            // Attempt Link signup
            val linkComponent = linkHandler.linkConfigurationCoordinator
                .getComponent(linkConfiguration)
            val linkAuth = linkComponent.linkAuth
            val linkAccountManager = linkComponent.linkAccountManager

            val signupResult = linkAuth.signUp(
                email = email,
                phoneNumber = billing.phone,
                country = billing.address?.country,
                name = billing.name,
                consentAction = SignUpConsentAction.Implied
            )

            when (signupResult) {
                is LinkAuthResult.AttestationFailed -> {
                    eventReporter.onLinkUserSignupFailed(signupResult.error)
                }
                is LinkAuthResult.Error -> {
                    eventReporter.onLinkUserSignupFailed(signupResult.error)
                }
                is LinkAuthResult.Success -> {
                    eventReporter.onLinkUserSignupSucceeded()
                    createCardPaymentDetailsIfNeeded(paymentSelection, linkAccountManager)
                }
                LinkAuthResult.NoLinkAccountFound -> {
                    eventReporter.onLinkUserSignupFailed(NoLinkAccountFoundException())
                }
                is LinkAuthResult.AccountError -> {
                    eventReporter.onLinkUserSignupFailed(signupResult.error)
                }
            }
        }.onFailure {
            eventReporter.onLinkUserSignupFailed(it)
        }
    }

    private suspend fun createCardPaymentDetailsIfNeeded(
        paymentSelection: PaymentSelection?,
        linkAccountManager: LinkAccountManager
    ) {
        // Create payment method in Link if we have card payment selection
        if (paymentSelection is PaymentSelection.New.Card) {
            val paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams
            val cardPaymentDetailsResult = linkAccountManager.createCardPaymentDetails(
                paymentMethodCreateParams
            )
            if (cardPaymentDetailsResult.isSuccess) {
                eventReporter.onLinkUserPaymentDetailCreationCompleted(error = null)
            } else {
                eventReporter.onLinkUserPaymentDetailCreationCompleted(
                    error = cardPaymentDetailsResult.exceptionOrNull()
                )
            }
        }
    }
}
