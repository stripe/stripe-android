package com.stripe.android.paymentelement.confirmation.intent

import android.content.Context
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentPageConfirmParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.paymentsheet.model.amount
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class CheckoutSessionConfirmationInterceptor @AssistedInject constructor(
    @Assisted private val checkoutSessionId: String,
    @Assisted private val clientAttributionMetadata: ClientAttributionMetadata,
    private val context: Context,
    private val stripeRepository: StripeRepository,
    private val requestOptions: ApiRequest.Options,
) : IntentConfirmationInterceptor {
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
                confirm(
                    intent = intent,
                    paymentMethod = paymentMethod,
                    shippingValues = shippingValues,
                    hCaptchaToken = null,
                    attestationToken = null,
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
        return confirm(
            intent = intent,
            paymentMethod = confirmationOption.paymentMethod,
            shippingValues = shippingValues,
            hCaptchaToken = confirmationOption.hCaptchaToken,
            attestationToken = confirmationOption.attestationToken,
        )
    }

    private suspend fun confirm(
        intent: StripeIntent,
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        hCaptchaToken: String?,
        attestationToken: String?,
    ): ConfirmationDefinition.Action<Args> {
        // TODO: Probably need to pass in shipping values or something?
        // TODO: Should be able to attach hcaptcha/attestation
        return stripeRepository.confirmPaymentPage(
            checkoutSessionId = checkoutSessionId,
            params = PaymentPageConfirmParams(
                paymentMethodId = paymentMethod.id,
                expectedPaymentMethodType = paymentMethod.type?.code,
                expectedAmount = intent.amount ?: 0L,
                clientAttributionMetadata = clientAttributionMetadata,
                returnUrl = DefaultReturnUrl.create(context).value,
                passiveCaptchaToken = hCaptchaToken,
            ),
            options = requestOptions,
        ).fold(
            onSuccess = { response ->
                val intent = requireNotNull(response.intent)
                when {
                    intent.isConfirmed -> {
                        ConfirmationDefinition.Action.Complete(
                            intent = intent,
                            deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
                            completedFullPaymentFlow = true,
                        )
                    }
                    intent.requiresAction() -> {
                        ConfirmationDefinition.Action.Launch(
                            launcherArguments = Args.NextAction(intent),
                            deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
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
            onFailure = { exception ->
                ConfirmationDefinition.Action.Fail(
                    cause = exception,
                    message = exception.stripeErrorMessage(),
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
