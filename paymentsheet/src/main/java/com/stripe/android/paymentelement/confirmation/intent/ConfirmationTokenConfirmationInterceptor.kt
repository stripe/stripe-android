package com.stripe.android.paymentelement.confirmation.intent

import android.content.Context
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmationToken
import com.stripe.android.model.ConfirmationTokenParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.RadarOptions
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
import com.stripe.android.paymentelement.confirmation.utils.ConfirmActionHelper
import com.stripe.android.paymentelement.confirmation.utils.toConfirmParamsSetupFutureUsage
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class ConfirmationTokenConfirmationInterceptor @AssistedInject constructor(
    @Assisted private val intentConfiguration: PaymentSheet.IntentConfiguration,
    @Assisted private val createIntentCallback: CreateIntentWithConfirmationTokenCallback,
    private val context: Context,
    private val stripeRepository: StripeRepository,
    private val requestOptions: ApiRequest.Options,
) : IntentConfirmationInterceptor {
    private val confirmActionHelper: ConfirmActionHelper = ConfirmActionHelper(requestOptions.apiKeyIsLiveMode)

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): ConfirmationDefinition.Action<Args> {
        val updatedConfirmationOption = confirmationOption.updatedForDeferredIntent(intentConfiguration)
        return stripeRepository.createConfirmationToken(
            confirmationTokenParams = ConfirmationTokenParams(
                returnUrl = DefaultReturnUrl.create(context).value,
                paymentMethodData = updatedConfirmationOption.createParams
            ),
            options = requestOptions,
        ).fold(
            onSuccess = { confirmationToken ->
                val paymentMethodType = confirmationToken.paymentMethodPreview?.type
                    ?: return ConfirmationDefinition.Action.Fail(
                        cause = IllegalStateException(ERROR_MISSING_PAYMENT_METHOD_TYPE),
                        message = ERROR_MISSING_PAYMENT_METHOD_TYPE.resolvableString,
                        errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                    )
                handleDeferredOnConfirmationTokenCreated(
                    intent = intent,
                    confirmationToken = confirmationToken,
                    intentConfiguration = intentConfiguration,
                    paymentMethodId = "",
                    paymentMethodType = paymentMethodType,
                    confirmationOption = updatedConfirmationOption,
                    shippingValues = shippingValues,
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
        ephemeralKeySecret: String?
    ): ConfirmationDefinition.Action<Args> {
        val paymentMethod = confirmationOption.paymentMethod
        return stripeRepository.createConfirmationToken(
            confirmationTokenParams = ConfirmationTokenParams(
                returnUrl = DefaultReturnUrl.create(context).value,
                paymentMethodId = paymentMethod.id
                    ?: return ConfirmationDefinition.Action.Fail(
                        cause = IllegalStateException(ERROR_MISSING_PAYMENT_METHOD_ID),
                        message = ERROR_MISSING_PAYMENT_METHOD_ID.resolvableString,
                        errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                    )
            ),
            options = requestOptions.copy(
                apiKey = ephemeralKeySecret
                    ?: return ConfirmationDefinition.Action.Fail(
                        cause = IllegalStateException(ERROR_MISSING_EPHEMERAL_KEY_SECRET),
                        message = ERROR_MISSING_EPHEMERAL_KEY_SECRET.resolvableString,
                        errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                    ),
            ),
        ).fold(
            onSuccess = { confirmationToken ->
                handleDeferredOnConfirmationTokenCreated(
                    intent = intent,
                    confirmationToken = confirmationToken,
                    intentConfiguration = intentConfiguration,
                    paymentMethodId = paymentMethod.id.orEmpty(),
                    paymentMethodType = paymentMethod.type ?: return ConfirmationDefinition.Action.Fail(
                        cause = IllegalStateException(ERROR_MISSING_PAYMENT_METHOD_TYPE),
                        message = ERROR_MISSING_PAYMENT_METHOD_TYPE.resolvableString,
                        errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                    ),
                    confirmationOption = confirmationOption,
                    shippingValues = shippingValues,
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

    private suspend fun handleDeferredOnConfirmationTokenCreated(
        intent: StripeIntent,
        confirmationToken: ConfirmationToken,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethodId: String,
        paymentMethodType: PaymentMethod.Type,
        confirmationOption: PaymentMethodConfirmationOption,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmationDefinition.Action<Args> {
        val result = createIntentCallback.onCreateIntent(confirmationToken)

        return when (result) {
            is CreateIntentResult.Success -> {
                if (result.clientSecret == IntentConfirmationInterceptor.COMPLETE_WITHOUT_CONFIRMING_INTENT) {
                    ConfirmationDefinition.Action.Complete(
                        intent = intent,
                        deferredIntentConfirmationType = DeferredIntentConfirmationType.None,
                        completedFullPaymentFlow = true,
                    )
                } else {
                    handleDeferredIntentCreationSuccess(
                        clientSecret = result.clientSecret,
                        intentConfiguration = intentConfiguration,
                        paymentMethodId = paymentMethodId,
                        paymentMethodType = paymentMethodType,
                        confirmationOption = confirmationOption,
                        shippingValues = shippingValues,
                    )
                }
            }

            is CreateIntentResult.Failure -> {
                val exception = CreateIntentWithConfirmationTokenCallbackFailureException(result.cause)
                ConfirmationDefinition.Action.Fail(
                    cause = exception,
                    message = result.displayMessage?.resolvableString
                        ?: exception.stripeErrorMessage(),
                    errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            }
        }
    }

    private suspend fun handleDeferredIntentCreationSuccess(
        clientSecret: String,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        confirmationOption: PaymentMethodConfirmationOption,
        paymentMethodId: String,
        paymentMethodType: PaymentMethod.Type,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmationDefinition.Action<Args> {
        return stripeRepository.retrieveStripeIntent(
            clientSecret = clientSecret,
            options = requestOptions,
        ).mapCatching { intent ->
            if (intent.isConfirmed) {
                ConfirmationDefinition.Action.Complete(
                    intent = intent,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
                    completedFullPaymentFlow = true,
                )
            } else if (intent.requiresAction()) {
                ConfirmationDefinition.Action.Launch<Args>(
                    launcherArguments = Args.NextAction(clientSecret),
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
                    receivesResultInProcess = false,
                )
            } else {
                confirmActionHelper.createConfirmAction(
                    clientSecret,
                    intent,
                    shippingValues,
                    isDeferred = true
                ) {
                    create(
                        paymentMethodId = paymentMethodId,
                        paymentMethodType = paymentMethodType,
                        optionsParams = confirmationOption.optionsParams,
                        extraParams = (confirmationOption as? PaymentMethodConfirmationOption.New)
                            ?.extraParams,
                        intentConfigSetupFutureUsage = intentConfiguration
                            .mode.setupFutureUse?.toConfirmParamsSetupFutureUsage(),
                        radarOptions = (confirmationOption as? PaymentMethodConfirmationOption.Saved)
                            ?.hCaptchaToken?.let { RadarOptions(it) },
                        clientAttributionMetadata = confirmationOption.clientAttributionMetadata,
                    )
                }
            }
        }.getOrElse { error ->
            ConfirmationDefinition.Action.Fail(
                cause = error,
                message = error.stripeErrorMessage(),
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            intentConfiguration: PaymentSheet.IntentConfiguration,
            createIntentCallback: CreateIntentWithConfirmationTokenCallback,
        ): ConfirmationTokenConfirmationInterceptor
    }

    companion object {
        private const val ERROR_MISSING_PAYMENT_METHOD_ID = "PaymentMethod must have an ID"
        private const val ERROR_MISSING_PAYMENT_METHOD_TYPE = "Failed to fetch PaymentMethod Type"
        private const val ERROR_MISSING_EPHEMERAL_KEY_SECRET =
            "Ephemeral key secret is required to confirm with saved payment method"
    }
}

internal class CreateIntentWithConfirmationTokenCallbackFailureException(
    override val cause: Throwable?
) : StripeException() {
    override fun analyticsValue(): String = "merchantReturnedCreateIntentWithConfirmationTokenCallbackFailure"
}
