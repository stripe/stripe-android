package com.stripe.android.paymentelement.confirmation.intent

import android.content.Context
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
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
internal class CheckoutSessionConfirmationInterceptor @AssistedInject constructor(
    @Assisted private val checkoutSessionId: String,
    @Assisted private val clientAttributionMetadata: ClientAttributionMetadata,
    context: Context,
    private val stripeRepository: StripeRepository,
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val requestOptions: ApiRequest.Options,
) : IntentConfirmationInterceptor {

    private val returnUrl: String = DefaultReturnUrl.create(context).value

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
                confirmCheckoutSession(
                    paymentMethod = paymentMethod,
                    savePaymentMethod = confirmationOption.shouldSave,
                )
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
        return confirmCheckoutSession(
            paymentMethod = confirmationOption.paymentMethod,
            savePaymentMethod = null,
        )
    }

    /**
     * Confirms the checkout session with the payment method.
     *
     * @param paymentMethod The payment method to confirm with.
     * @param savePaymentMethod Whether to save the payment method for future use.
     *        Pass `true` if user checked "Save for future use", `false` otherwise.
     *        Pass `null` for saved payment methods (already saved).
     */
    private suspend fun confirmCheckoutSession(
        paymentMethod: PaymentMethod,
        savePaymentMethod: Boolean?,
    ): ConfirmationDefinition.Action<Args> {
        return checkoutSessionRepository.confirm(
            id = checkoutSessionId,
            params = ConfirmCheckoutSessionParams(
                paymentMethodId = paymentMethod.id,
                clientAttributionMetadata = clientAttributionMetadata,
                returnUrl = returnUrl,
                savePaymentMethod = savePaymentMethod,
            ),
        ).fold(
            onSuccess = { response ->
                val exception = IllegalStateException("No PaymentIntent in checkout session confirm response")
                val paymentIntent = response.paymentIntent
                    ?: return@fold ConfirmationDefinition.Action.Fail(
                        cause = exception,
                        message = exception.stripeErrorMessage(),
                        errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                    )

                when {
                    paymentIntent.isConfirmed -> {
                        ConfirmationDefinition.Action.Complete(
                            intent = paymentIntent,
                            metadata = MutableConfirmationMetadata().apply {
                                set(DeferredIntentConfirmationTypeKey, DeferredIntentConfirmationType.Server)
                            },
                            completedFullPaymentFlow = true,
                        )
                    }
                    paymentIntent.requiresAction() -> {
                        ConfirmationDefinition.Action.Launch(
                            launcherArguments = Args.NextAction(paymentIntent, DeferredIntentConfirmationType.Server),
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
            checkoutSessionId: String,
            clientAttributionMetadata: ClientAttributionMetadata,
        ): CheckoutSessionConfirmationInterceptor
    }
}
