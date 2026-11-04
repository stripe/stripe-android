package com.stripe.android.paymentelement.confirmation.intent

import android.content.Context
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkout.toCheckoutAddress
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.ConfirmCheckoutSessionParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Confirms checkout sessions with new payment method data or an existing payment method ID.
 */
@OptIn(CheckoutSessionPreview::class)
internal class CheckoutSessionConfirmationInterceptor @AssistedInject constructor(
    @Assisted private val integrationMetadata: IntegrationMetadata.CheckoutSession,
    @Assisted private val customerMetadata: CustomerMetadata?,
    @Assisted private val clientAttributionMetadata: ClientAttributionMetadata,
    context: Context,
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val checkoutSessionTaxRegionUpdater: CheckoutSessionTaxRegionUpdater,
) : IntentConfirmationInterceptor {

    private val returnUrl: String = DefaultReturnUrl.create(context).value
    private val genericErrorMessage: String = context.getString(R.string.stripe_something_went_wrong)
    private val isSaveEnabled: Boolean =
        customerMetadata?.saveConsent is PaymentMethodSaveConsentBehavior.Enabled

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmationDefinition.Action<Args> {
        val params = createConfirmParams(
            intent = intent,
            confirmationOption = confirmationOption,
            savePaymentMethod = confirmationOption.shouldSave.takeIf { isSaveEnabled },
            shipping = shippingValues.toCheckoutSessionShipping(),
        )
        return confirmCheckoutSession(params)
    }

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmationDefinition.Action<Args> {
        // For saved payment methods, we don't need to create a new PM or save it again.
        maybeUpdateBillingDetailsForCheckoutSession(
            paymentMethod = confirmationOption.paymentMethod,
        ).getOrElse { error ->
            return ConfirmationDefinition.Action.Fail(
                cause = error,
                message = error.stripeErrorMessage(),
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Transaction(),
            )
        }

        val params = createConfirmParams(
            intent = intent,
            confirmationOption = confirmationOption,
            savePaymentMethod = null,
            // ECE Google Pay returns the buyer's final shipping address after confirmation starts.
            // It cannot be reconciled with the earlier shippingValues snapshot, so use it for this
            // confirmation and otherwise retain the controller shipping.
            shipping = confirmationOption.shippingInformation.toCheckoutSessionShipping()
                ?: shippingValues.toCheckoutSessionShipping(),
        )
        return confirmCheckoutSession(params)
    }

    private suspend fun maybeUpdateBillingDetailsForCheckoutSession(
        paymentMethod: PaymentMethod,
    ): Result<Unit> {
        val billingDetails = paymentMethod.billingDetails
        val checkoutSessionResponse = integrationMetadata.checkoutSessionResponse
        val initialEstimatedTotal = checkoutSessionResponse.amount
        val billingAddress = billingDetails?.address?.toCheckoutAddress()
        val updatedCheckoutSessionResponse = if (billingAddress != null) {
            checkoutSessionTaxRegionUpdater.updateServerStateIfNeeded(
                checkoutSessionResponse = checkoutSessionResponse,
                addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                address = billingAddress,
            ).getOrElse { error -> return Result.failure(error) }
        } else {
            checkoutSessionResponse
        }
        val finalEstimatedTotal = updatedCheckoutSessionResponse?.amount
        if (initialEstimatedTotal != finalEstimatedTotal) {
            val error = LocalStripeException(
                displayMessage = genericErrorMessage,
                analyticsValue = "checkoutSessionTotalChanged",
                errorCode = "checkout_session_total_changed",
            )
            return Result.failure(error)
        }
        return Result.success(Unit)
    }

    private fun createConfirmParams(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption,
        savePaymentMethod: Boolean?,
        shipping: ConfirmCheckoutSessionParams.Shipping?,
    ): ConfirmCheckoutSessionParams = ConfirmCheckoutSessionParams(
        paymentMethodId = (confirmationOption as? PaymentMethodConfirmationOption.Saved)?.paymentMethod?.id,
        paymentMethodData = (confirmationOption as? PaymentMethodConfirmationOption.New)?.createParams?.let { params ->
            // CheckoutClient expects the captcha token at the top level; retain other Radar data.
            params.copy(radarOptions = params.radarOptions?.copy(hCaptchaToken = null))
        },
        passiveCaptchaToken = when (confirmationOption) {
            is PaymentMethodConfirmationOption.New ->
                confirmationOption.createParams.radarOptions?.toParamMap()?.get("hcaptcha_token") as? String
            is PaymentMethodConfirmationOption.Saved -> confirmationOption.confirmationChallengeState.hCaptchaToken
        },
        expectedPaymentMethodType = when (confirmationOption) {
            is PaymentMethodConfirmationOption.New ->
                PaymentMethod.Type.fromCode(confirmationOption.createParams.typeCode)?.code
            is PaymentMethodConfirmationOption.Saved -> confirmationOption.paymentMethod.type?.code
        },
        clientAttributionMetadata = clientAttributionMetadata,
        returnUrl = returnUrl,
        expectedAmount = integrationMetadata.checkoutSessionResponse.amount,
        savePaymentMethod = savePaymentMethod.takeIf { intent is PaymentIntent },
        shipping = shipping,
        collectedInformation = integrationMetadata.collectedEmail
            ?.takeIf { integrationMetadata.checkoutSessionResponse.fixedEmail == null }
            ?.let { ConfirmCheckoutSessionParams.CollectedInformation(email = it) },
    )

    private suspend fun confirmCheckoutSession(
        params: ConfirmCheckoutSessionParams,
    ): ConfirmationDefinition.Action<Args> {
        return checkoutSessionRepository.confirm(
            id = integrationMetadata.id,
            clientSecret = integrationMetadata.checkoutSessionResponse.clientSecret,
            params = params,
        ).fold(
            onSuccess = ::handleConfirmResponse,
            onFailure = { error ->
                ConfirmationDefinition.Action.Fail(
                    cause = error,
                    message = error.stripeErrorMessage(),
                    errorType = ConfirmationHandler.Result.Failed.ErrorType.Transaction(),
                )
            }
        )
    }

    private fun handleConfirmResponse(
        response: CheckoutSessionResponse,
    ): ConfirmationDefinition.Action<Args> {
        val intent: StripeIntent = response.paymentIntent ?: response.setupIntent
            ?: run {
                val exception = IllegalStateException(
                    "No PaymentIntent or SetupIntent in checkout session confirm response"
                )
                return ConfirmationDefinition.Action.Fail(
                    cause = exception,
                    message = exception.stripeErrorMessage(),
                    errorType = ConfirmationHandler.Result.Failed.ErrorType.Transaction(),
                )
            }

        // Only next actions are handled here. Whether the session completed comes from polling after
        // confirmation, not from the Intent status.
        return if (intent.requiresAction()) {
            ConfirmationDefinition.Action.Launch(
                launcherArguments = Args.NextAction(
                    intent = intent,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
                    checkoutSessionResponse = response,
                ),
                receivesResultInProcess = false,
            )
        } else {
            ConfirmationDefinition.Action.Complete(
                intent = intent,
                metadata = MutableConfirmationMetadata().apply {
                    set(DeferredIntentConfirmationTypeKey, DeferredIntentConfirmationType.Server)
                    set(CheckoutSessionResponseKey, response)
                },
                completedFullPaymentFlow = true,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            integrationMetadata: IntegrationMetadata.CheckoutSession,
            customerMetadata: CustomerMetadata?,
            clientAttributionMetadata: ClientAttributionMetadata,
        ): CheckoutSessionConfirmationInterceptor
    }
}

private fun ShippingInformation?.toCheckoutSessionShipping(): ConfirmCheckoutSessionParams.Shipping? {
    return this?.takeIf {
        it.address?.toParamMap()?.isNotEmpty() == true && !it.name.isNullOrEmpty()
    }?.let {
        ConfirmCheckoutSessionParams.Shipping(
            name = it.name,
            address = it.address,
        )
    }
}

private fun ConfirmPaymentIntentParams.Shipping?.toCheckoutSessionShipping(): ConfirmCheckoutSessionParams.Shipping? {
    return this?.takeIf {
        it.getAddress().toParamMap().isNotEmpty() && !it.getName().isNullOrEmpty()
    }?.let {
        ConfirmCheckoutSessionParams.Shipping(
            name = it.getName(),
            address = it.getAddress(),
        )
    }
}
