package com.stripe.android.link.confirmation

import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkPassthroughConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import javax.inject.Inject

internal class DefaultLinkConfirmationHandler @Inject constructor(
    private val configuration: LinkConfiguration,
    private val logger: Logger,
    private val confirmationHandler: ConfirmationHandler,
) : LinkConfirmationHandler {
    override suspend fun confirm(
        paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        billingPhone: String?
    ): Result {
        return confirm {
            newConfirmationArgs(
                paymentDetails = paymentDetails,
                linkAccount = linkAccount,
                cvc = cvc,
                billingPhone = billingPhone
            )
        }
    }

    private suspend fun confirm(
        createArgs: () -> ConfirmationHandler.Args
    ): Result {
        return runCatching {
            val args = createArgs()
            confirmationHandler.start(args)
            val result = confirmationHandler.awaitResult()
            transformResult(result)
        }.getOrElse { error ->
            logger.error(
                msg = "DefaultLinkConfirmationHandler: Failed to confirm payment",
                t = error
            )
            Result.Failed(R.string.stripe_something_went_wrong.resolvableString)
        }
    }

    private fun transformResult(result: ConfirmationHandler.Result?): Result {
        return when (result) {
            is ConfirmationHandler.Result.Canceled -> Result.Canceled
            is ConfirmationHandler.Result.Failed -> {
                logger.error(
                    msg = "DefaultLinkConfirmationHandler: Failed to confirm payment",
                    t = result.cause
                )
                Result.Failed(result.message)
            }
            is ConfirmationHandler.Result.Succeeded -> Result.Succeeded
            null -> {
                logger.error("DefaultLinkConfirmationHandler: Payment confirmation returned null")
                Result.Failed(R.string.stripe_something_went_wrong.resolvableString)
            }
        }
    }

    private fun newConfirmationArgs(
        paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        billingPhone: String?
    ): ConfirmationHandler.Args {
        val confirmationOption = if (configuration.passthroughModeEnabled) {
            LinkPassthroughConfirmationOption(
                paymentDetailsId = paymentDetails.id,
                expectedPaymentMethodType = computeExpectedPaymentMethodType(paymentDetails),
                cvc = cvc,
                billingPhone = billingPhone
            )
        } else {
            PaymentMethodConfirmationOption.New(
                createParams = createPaymentMethodCreateParams(
                    selectedPaymentDetails = paymentDetails,
                    linkAccount = linkAccount,
                    cvc = cvc
                ),
                extraParams = null,
                optionsParams = null,
                shouldSave = false
            )
        }

        return ConfirmationHandler.Args(
            intent = configuration.stripeIntent,
            confirmationOption = confirmationOption,
            appearance = PaymentSheet.Appearance(),
            initializationMode = configuration.initializationMode,
            shippingDetails = configuration.shippingDetails
        )
    }

    private fun createPaymentMethodCreateParams(
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?
    ): PaymentMethodCreateParams {
        return PaymentMethodCreateParams.createLink(
            paymentDetailsId = selectedPaymentDetails.id,
            consumerSessionClientSecret = linkAccount.clientSecret,
            extraParams = cvc?.let { mapOf("card" to mapOf("cvc" to cvc)) },
        )
    }

    private fun computeExpectedPaymentMethodType(
        paymentDetails: ConsumerPaymentDetails.PaymentDetails
    ): String {
        return when (paymentDetails) {
            is ConsumerPaymentDetails.BankAccount -> computeBankAccountExpectedPaymentMethodType()
            is ConsumerPaymentDetails.Card -> ConsumerPaymentDetails.Card.TYPE
        }
    }

    private fun computeBankAccountExpectedPaymentMethodType(): String {
        val canAcceptACH = USBankAccount.code in configuration.stripeIntent.paymentMethodTypes
        val isLinkCardBrand = configuration.linkMode == LinkMode.LinkCardBrand

        return if (isLinkCardBrand && !canAcceptACH) {
            ConsumerPaymentDetails.Card.TYPE
        } else {
            ConsumerPaymentDetails.BankAccount.TYPE
        }
    }

    class Factory @Inject constructor(
        private val configuration: LinkConfiguration,
        private val logger: Logger,
    ) : LinkConfirmationHandler.Factory {
        override fun create(confirmationHandler: ConfirmationHandler): LinkConfirmationHandler {
            return DefaultLinkConfirmationHandler(
                confirmationHandler = confirmationHandler,
                logger = logger,
                configuration = configuration,
            )
        }
    }
}
