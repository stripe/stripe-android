package com.stripe.android.link.confirmation

import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.Address
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.wallets.Wallet
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
    private val passiveCaptchaParams: PassiveCaptchaParams?
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

    override suspend fun confirm(
        paymentDetails: LinkPaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        billingPhone: String?
    ): Result {
        return confirm {
            confirmationArgs(
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

    private fun confirmationArgs(
        paymentDetails: LinkPaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        billingPhone: String?
    ): ConfirmationHandler.Args {
        return when (paymentDetails) {
            is LinkPaymentDetails.New -> {
                newConfirmationArgs(
                    paymentDetails = paymentDetails.paymentDetails,
                    linkAccount = linkAccount,
                    cvc = cvc,
                    billingPhone = billingPhone
                )
            }
            is LinkPaymentDetails.Saved -> {
                savedConfirmationArgs(
                    paymentDetails = paymentDetails,
                    cvc = cvc
                )
            }
        }
    }

    private fun newConfirmationArgs(
        paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        billingPhone: String?
    ): ConfirmationHandler.Args {
        val paymentMethodType = if (configuration.passthroughModeEnabled) {
            computeExpectedPaymentMethodType(configuration, paymentDetails)
        } else {
            PaymentMethod.Type.Link.code
        }

        val allowRedisplay = allowRedisplay(paymentMethodType = paymentMethodType)

        val confirmationOption = if (configuration.passthroughModeEnabled) {
            LinkPassthroughConfirmationOption(
                paymentDetailsId = paymentDetails.id,
                expectedPaymentMethodType = paymentMethodType,
                cvc = cvc,
                billingPhone = billingPhone,
                allowRedisplay = allowRedisplay,
                passiveCaptchaParams = passiveCaptchaParams,
                clientAttributionMetadata = configuration.clientAttributionMetadata,
            )
        } else {
            PaymentMethodConfirmationOption.New(
                createParams = createPaymentMethodCreateParams(
                    selectedPaymentDetails = paymentDetails,
                    consumerSessionClientSecret = linkAccount.clientSecret,
                    cvc = cvc,
                    billingPhone = billingPhone,
                    allowRedisplay = allowRedisplay,
                    clientAttributionMetadata = configuration.clientAttributionMetadata,
                ),
                extraParams = null,
                optionsParams = null,
                shouldSave = false,
                passiveCaptchaParams = passiveCaptchaParams,
                clientAttributionMetadata = configuration.clientAttributionMetadata,
            )
        }

        return ConfirmationHandler.Args(
            intent = configuration.stripeIntent,
            confirmationOption = confirmationOption,
            appearance = PaymentSheet.Appearance(),
            initializationMode = configuration.initializationMode,
            shippingDetails = configuration.shippingDetails,
        )
    }

    private fun allowRedisplay(paymentMethodType: String): PaymentMethod.AllowRedisplay? {
        val isSettingUp = when (val intent = configuration.stripeIntent) {
            is PaymentIntent -> intent.isSetupFutureUsageSet(paymentMethodType)
            is SetupIntent -> true
        }

        return if (isSettingUp || configuration.forceSetupFutureUseBehaviorAndNewMandate) {
            configuration.saveConsentBehavior.overrideAllowRedisplay ?: PaymentMethod.AllowRedisplay.LIMITED
        } else {
            PaymentMethod.AllowRedisplay.UNSPECIFIED
        }
    }

    private fun savedConfirmationArgs(
        paymentDetails: LinkPaymentDetails.Saved,
        cvc: String?
    ): ConfirmationHandler.Args {
        return ConfirmationHandler.Args(
            intent = configuration.stripeIntent,
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethod.Builder()
                    .setId(paymentDetails.paymentDetails.paymentMethodId)
                    .setCode(paymentDetails.paymentMethodCreateParams.typeCode)
                    .setCard(
                        PaymentMethod.Card(
                            last4 = paymentDetails.paymentDetails.last4,
                            wallet = Wallet.LinkWallet(paymentDetails.paymentDetails.last4),
                        )
                    )
                    .setType(PaymentMethod.Type.Card)
                    .build(),
                optionsParams = PaymentMethodOptionsParams.Card(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
                    cvc = cvc?.takeIf {
                        configuration.passthroughModeEnabled.not()
                    }
                ),
                passiveCaptchaParams = passiveCaptchaParams,
                clientAttributionMetadata = configuration.clientAttributionMetadata,
            ),
            appearance = PaymentSheet.Appearance(),
            initializationMode = configuration.initializationMode,
            shippingDetails = configuration.shippingDetails
        )
    }

    class Factory @Inject constructor(
        private val configuration: LinkConfiguration,
        private val passiveCaptchaParams: PassiveCaptchaParams?,
        private val logger: Logger,
    ) : LinkConfirmationHandler.Factory {
        override fun create(confirmationHandler: ConfirmationHandler): LinkConfirmationHandler {
            return DefaultLinkConfirmationHandler(
                confirmationHandler = confirmationHandler,
                logger = logger,
                configuration = configuration,
                passiveCaptchaParams = passiveCaptchaParams
            )
        }
    }
}

internal fun createPaymentMethodCreateParams(
    selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
    consumerSessionClientSecret: String,
    cvc: String?,
    billingPhone: String?,
    allowRedisplay: PaymentMethod.AllowRedisplay? = null,
    clientAttributionMetadata: ClientAttributionMetadata?,
): PaymentMethodCreateParams {
    val billingDetails = PaymentMethod.BillingDetails(
        address = selectedPaymentDetails.billingAddress?.let {
            Address(
                line1 = it.line1,
                line2 = it.line2,
                postalCode = it.postalCode,
                city = it.locality,
                state = it.administrativeArea,
                country = it.countryCode?.value,
            )
        },
        email = selectedPaymentDetails.billingEmailAddress,
        name = selectedPaymentDetails.billingAddress?.name,
        phone = billingPhone,
    )

    return PaymentMethodCreateParams.createLink(
        paymentDetailsId = selectedPaymentDetails.id,
        consumerSessionClientSecret = consumerSessionClientSecret,
        billingDetails = billingDetails.takeIf { it != PaymentMethod.BillingDetails() },
        extraParams = cvc?.let { mapOf("card" to mapOf("cvc" to cvc)) },
        allowRedisplay = allowRedisplay,
        clientAttributionMetadata = clientAttributionMetadata,
    )
}

internal fun computeExpectedPaymentMethodType(
    configuration: LinkConfiguration,
    paymentDetails: ConsumerPaymentDetails.PaymentDetails
): String {
    return when (paymentDetails) {
        is ConsumerPaymentDetails.BankAccount -> computeBankAccountExpectedPaymentMethodType(configuration)
        is ConsumerPaymentDetails.Card -> ConsumerPaymentDetails.Card.TYPE
        is ConsumerPaymentDetails.Passthrough -> ConsumerPaymentDetails.Card.TYPE
    }
}

private fun computeBankAccountExpectedPaymentMethodType(configuration: LinkConfiguration): String {
    val canAcceptACH = USBankAccount.code in configuration.stripeIntent.paymentMethodTypes
    val isLinkCardBrand = configuration.linkMode == LinkMode.LinkCardBrand

    return if (isLinkCardBrand && !canAcceptACH) {
        ConsumerPaymentDetails.Card.TYPE
    } else {
        ConsumerPaymentDetails.BankAccount.TYPE
    }
}

private val PaymentMethodSaveConsentBehavior.overrideAllowRedisplay: PaymentMethod.AllowRedisplay?
    get() = when (this) {
        is PaymentMethodSaveConsentBehavior.Disabled -> overrideAllowRedisplay
        is PaymentMethodSaveConsentBehavior.Enabled, PaymentMethodSaveConsentBehavior.Legacy -> null
    }
