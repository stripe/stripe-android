package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.paymentsheet.LinkHandler
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
            // Signup toggle is off
            if (!signupToLinkToggleInteractor.toggleValue.value) return
            // Link account already exists
            if (linkAccountHolder.linkAccountInfo.value.account != null) return
            // No email provided
            if (email == null) return

            // Attempt Link signup
            val linkAccountManager = linkHandler.linkConfigurationCoordinator
                .getComponent(linkConfiguration).linkAccountManager

            val userInput = UserInput.SignUpOptionalPhone(
                email = email,
                country = billing.address?.country,
                phone = billing.phone,
                name = billing.name,
                consentAction = SignUpConsentAction.Implied
            )

            // Create Link account
            val accountResult = linkAccountManager.signInWithUserInput(userInput)
            if (accountResult.isSuccess) {
                eventReporter.onLinkUserSignupSucceeded()
                createCardPaymentDetailsIfNeeded(paymentSelection, linkAccountManager)
            } else {
                val error = accountResult.exceptionOrNull()
                if (error != null) {
                    eventReporter.onLinkUserSignupFailed(error)
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
