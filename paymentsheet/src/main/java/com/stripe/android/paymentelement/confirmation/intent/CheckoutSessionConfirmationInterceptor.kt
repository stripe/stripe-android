package com.stripe.android.paymentelement.confirmation.intent

import android.content.Context
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.ConfirmCheckoutSessionParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Confirmation interceptor for checkout sessions.
 *
 * This interceptor handles the confirmation flow for checkout sessions by:
 * 1. For new PMs: Creating a payment method, then calling confirm
 * 2. For saved PMs: Directly calling confirm with the existing PM ID
 * 3. Handling the response (complete, requires action, or error)
 *
 * The `/v1/payment_pages/{checkoutSessionId}/confirm` API accepts both newly created
 * and existing payment method IDs.
 */
@OptIn(CheckoutSessionPreview::class)
internal class CheckoutSessionConfirmationInterceptor @AssistedInject constructor(
    @Assisted private val integrationMetadata: IntegrationMetadata.CheckoutSession,
    @Assisted private val customerMetadata: CustomerMetadata?,
    @Assisted private val clientAttributionMetadata: ClientAttributionMetadata,
    context: Context,
    private val stripeRepository: StripeRepository,
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val requestOptions: ApiRequest.Options,
) : IntentConfirmationInterceptor {

    private val returnUrl: String = DefaultReturnUrl.create(context).value
    private val isSaveEnabled: Boolean =
        customerMetadata?.saveConsent is PaymentMethodSaveConsentBehavior.Enabled

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmationDefinition.Action<Args> {
        return stripeRepository.createPaymentMethod(
            paymentMethodCreateParams = confirmationOption.createParams,
            options = requestOptions,
        ).fold(
            onSuccess = { paymentMethod ->
                val params = createConfirmParams(
                    intent = intent,
                    paymentMethod = paymentMethod,
                    savePaymentMethod = confirmationOption.shouldSave.takeIf { isSaveEnabled },
                )
                confirmCheckoutSession(params)
            },
            onFailure = { error ->
                ConfirmationDefinition.Action.Fail(
                    cause = error,
                    message = error.stripeErrorMessage(),
                    errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            }
        )
    }

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmationDefinition.Action<Args> {
        // For saved payment methods, we don't need to create a new PM or save it again.
        val params = createConfirmParams(
            intent = intent,
            paymentMethod = confirmationOption.paymentMethod,
            savePaymentMethod = null,
        )
        return confirmCheckoutSession(params)
    }

    private fun createConfirmParams(
        intent: StripeIntent,
        paymentMethod: PaymentMethod,
        savePaymentMethod: Boolean?,
    ): ConfirmCheckoutSessionParams = when (intent) {
        is PaymentIntent -> ConfirmCheckoutSessionParams(
            paymentMethodId = paymentMethod.id,
            clientAttributionMetadata = clientAttributionMetadata,
            returnUrl = returnUrl,
            expectedAmount = intent.amount ?: 0L,
            savePaymentMethod = savePaymentMethod,
        )
        else -> ConfirmCheckoutSessionParams(
            paymentMethodId = paymentMethod.id,
            clientAttributionMetadata = clientAttributionMetadata,
            returnUrl = returnUrl,
        )
    }

    private suspend fun confirmCheckoutSession(
        params: ConfirmCheckoutSessionParams,
    ): ConfirmationDefinition.Action<Args> {
        return checkoutSessionRepository.confirm(
            id = integrationMetadata.id,
            params = params,
        ).fold(
            onSuccess = { response ->
                CheckoutInstances[integrationMetadata.instancesKey].forEach { checkout ->
                    checkout.updateWithResponse(response)
                }

                val intent: StripeIntent = response.paymentIntent ?: response.setupIntent
                    ?: run {
                        val exception = IllegalStateException(
                            "No PaymentIntent or SetupIntent in checkout session confirm response"
                        )
                        return@fold ConfirmationDefinition.Action.Fail(
                            cause = exception,
                            message = exception.stripeErrorMessage(),
                            errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                        )
                    }

                when {
                    intent.isConfirmed -> {
                        ConfirmationDefinition.Action.Complete(
                            intent = intent,
                            metadata = MutableConfirmationMetadata().apply {
                                set(DeferredIntentConfirmationTypeKey, DeferredIntentConfirmationType.Server)
                            },
                            completedFullPaymentFlow = true,
                        )
                    }
                    intent.requiresAction() -> {
                        ConfirmationDefinition.Action.Launch(
                            launcherArguments = Args.NextAction(intent, DeferredIntentConfirmationType.Server),
                            receivesResultInProcess = false,
                        )
                    }
                    else -> {
                        val exception = IllegalStateException("Intent has not attempted confirm.")
                        ConfirmationDefinition.Action.Fail(
                            cause = exception,
                            message = exception.stripeErrorMessage(),
                            errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                        )
                    }
                }
            },
            onFailure = { error ->
                ConfirmationDefinition.Action.Fail(
                    cause = error,
                    message = error.stripeErrorMessage(),
                    errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            }
        )
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
