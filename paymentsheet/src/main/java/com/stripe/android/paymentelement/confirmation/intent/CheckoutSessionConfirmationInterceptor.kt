package com.stripe.android.paymentelement.confirmation.intent

import android.content.Context
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.injection.ApplicationContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.resolvableString
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
import com.stripe.android.paymentsheet.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Confirmation interceptor for checkout sessions.
 *
 * This interceptor handles the confirmation flow for checkout sessions by:
 * 1. Creating a new payment method from the user's input
 * 2. Calling the `/v1/payment_pages/{checkoutSessionId}/confirm` API
 * 3. Handling the response (complete, requires action, or error)
 *
 * Currently only supports new payment methods. Saved payment method support
 * will be added in a follow-up PR.
 */
internal class CheckoutSessionConfirmationInterceptor @AssistedInject constructor(
    @Assisted private val checkoutSessionId: String,
    @Assisted private val clientAttributionMetadata: ClientAttributionMetadata,
    @ApplicationContext context: Context,
    private val stripeRepository: StripeRepository,
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
                confirmCheckoutSession(paymentMethod)
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
        // Saved payment method support will be implemented in a follow-up PR
        return ConfirmationDefinition.Action.Fail(
            cause = NotImplementedError("Saved payment methods not yet supported for checkout sessions"),
            message = resolvableString(R.string.stripe_something_went_wrong),
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
        )
    }

    /**
     * Confirms the checkout session with the created payment method.
     */
    private suspend fun confirmCheckoutSession(
        paymentMethod: PaymentMethod,
    ): ConfirmationDefinition.Action<Args> {
        return stripeRepository.confirmCheckoutSession(
            checkoutSessionId = checkoutSessionId,
            paymentMethodId = paymentMethod.id,
            clientAttributionMetadata = clientAttributionMetadata,
            returnUrl = returnUrl,
            options = requestOptions,
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
