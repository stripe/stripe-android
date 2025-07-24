package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.billingDetails
import com.stripe.android.paymentsheet.ui.SignupToLinkToggleInteractor
import javax.inject.Inject

internal class SignupForLink @Inject constructor(
    private val linkAccountHolder: LinkAccountHolder,
    private val linkHandler: LinkHandler,
    private val signupToLinkToggleInteractor: SignupToLinkToggleInteractor,
    private val logger: Logger
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
            val phone = billing?.phone
            if (email == null) return

            // Attempt Link signup
            val linkAccountManager = linkHandler.linkConfigurationCoordinator
                .getComponent(linkConfiguration).linkAccountManager

            val userInput = UserInput.SignUpOptionalPhone(
                email = email,
                country = billing.address?.country ?: "US",
                phone = phone,
                name = billing.name,
                consentAction = SignUpConsentAction.Implied
            )
            logger.debug("Creating Link account with user input: $userInput")

            // Create Link account
            val accountResult = linkAccountManager.signInWithUserInput(userInput)
            if (accountResult.isSuccess) {
                logger.debug("Link account created successfully")
                createCardPaymentDetailsIfNeeded(paymentSelection, linkAccountManager)
            } else {
                val errorMessage = accountResult.exceptionOrNull()?.message
                logger.debug("Failed to create Link account: $errorMessage")
            }
        } catch (e: StripeException) {
            logger.debug("Failed to create Link account: ${e.message}")
        }
    }

    private suspend fun createCardPaymentDetailsIfNeeded(
        paymentSelection: PaymentSelection?,
        linkAccountManager: LinkAccountManager
    ) {
        // Create payment method in Link if we have card payment selection
        if (paymentSelection is PaymentSelection.New) {
            val paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams
            val cardPaymentDetailsResult = linkAccountManager.createCardPaymentDetails(
                paymentMethodCreateParams
            )
            if (cardPaymentDetailsResult.isSuccess) {
                logger.debug("Card payment details created in Link successfully")
            } else {
                val errorMessage = cardPaymentDetailsResult.exceptionOrNull()?.message
                logger.debug("Failed to create card payment details: $errorMessage")
            }
        }
    }
}
