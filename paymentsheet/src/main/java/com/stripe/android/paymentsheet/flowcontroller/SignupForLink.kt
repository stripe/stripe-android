package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
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
    private val logger: Logger,
    private val eventReporter: EventReporter
) {

    suspend operator fun invoke(
        linkConfiguration: LinkConfiguration?,
        paymentSelection: PaymentSelection?
    ) {
        try {
            // Check if we should sign up to Link
            if (!signupToLinkToggleInteractor.getSignupToLinkValue()) return
            if (linkAccountHolder.linkAccountInfo.value.account != null) return
            if (linkConfiguration == null) return

            val billing = paymentSelection?.billingDetails
            val email = billing?.email
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
            logger.debug("Creating Link account with user input: $userInput")

            // Create Link account
            val accountResult = linkAccountManager.signInWithUserInput(userInput)
            if (accountResult.isSuccess) {
                logger.debug("Link account created successfully")
                eventReporter.onLinkUserSignupSucceeded()
                createCardPaymentDetailsIfNeeded(paymentSelection, linkAccountManager)
            } else {
                val error = accountResult.exceptionOrNull()
                val errorMessage = error?.message
                logger.debug("Failed to create Link account: $errorMessage")
                if (error != null) {
                    eventReporter.onLinkUserSignupFailed(error)
                }
            }
        } catch (e: StripeException) {
            logger.debug("Failed to create Link account: ${e.message}")
            eventReporter.onLinkUserSignupFailed(e)
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
                logger.debug("Card payment details created in Link successfully")
                eventReporter.onLinkUserPaymentDetailCreationCompleted(error = null)
            } else {
                val error = cardPaymentDetailsResult.exceptionOrNull()
                val errorMessage = error?.message
                logger.debug("Failed to create card payment details: $errorMessage")
                eventReporter.onLinkUserPaymentDetailCreationCompleted(error = error)
            }
        }
    }
}
