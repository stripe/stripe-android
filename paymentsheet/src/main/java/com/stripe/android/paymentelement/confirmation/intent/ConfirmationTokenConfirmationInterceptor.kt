package com.stripe.android.paymentelement.confirmation.intent

import android.content.Context
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmationToken
import com.stripe.android.model.ConfirmationTokenClientContextParams
import com.stripe.android.model.ConfirmationTokenParams
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
import com.stripe.android.paymentelement.confirmation.utils.ConfirmActionHelper
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.toDeferredIntentParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class ConfirmationTokenConfirmationInterceptor @AssistedInject constructor(
    @Assisted private val intentConfiguration: PaymentSheet.IntentConfiguration,
    @Assisted private val createIntentCallback: CreateIntentWithConfirmationTokenCallback,
    @Assisted private val ephemeralKeySecret: String?,
    private val context: Context,
    private val stripeRepository: StripeRepository,
    private val requestOptions: ApiRequest.Options,
    private val userFacingLogger: UserFacingLogger,
) : IntentConfirmationInterceptor {
    private val confirmActionHelper: ConfirmActionHelper = ConfirmActionHelper(requestOptions.apiKeyIsLiveMode)

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): ConfirmationDefinition.Action<Args> {
        return stripeRepository.createConfirmationToken(
            confirmationTokenParams = prepareConfirmationTokenParams(confirmationOption),
            options = requestOptions,
        ).fold(
            onSuccess = { confirmationToken ->
                handleDeferredOnConfirmationTokenCreated(
                    intent = intent,
                    confirmationToken = confirmationToken,
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
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): ConfirmationDefinition.Action<Args> {
        val paymentMethod = confirmationOption.paymentMethod
        return stripeRepository.createConfirmationToken(
            confirmationTokenParams = ConfirmationTokenParams(
                returnUrl = DefaultReturnUrl.create(context).value,
                paymentMethodId = paymentMethod.id ?: "".also {
                    userFacingLogger.logWarningWithoutPii(ERROR_MISSING_PAYMENT_METHOD_ID)
                }
            ),
            options = if (paymentMethod.customerId != null) {
                requestOptions.copy(
                    apiKey = ephemeralKeySecret ?: "".also {
                        userFacingLogger.logWarningWithoutPii(ERROR_MISSING_EPHEMERAL_KEY_SECRET)
                    }
                )
            } else {
                requestOptions
            },
        ).fold(
            onSuccess = { confirmationToken ->
                handleDeferredOnConfirmationTokenCreated(
                    intent = intent,
                    confirmationToken = confirmationToken,
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
                        confirmationTokenId = confirmationToken.id,
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
        confirmationTokenId: String,
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
                        confirmationTokenId = confirmationTokenId
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

    private fun prepareConfirmationTokenParams(confirmationOption: PaymentMethodConfirmationOption):
        ConfirmationTokenParams {
        val updatedConfirmationOption = confirmationOption.updatedForDeferredIntent(intentConfiguration)
        return ConfirmationTokenParams(
            returnUrl = DefaultReturnUrl.create(context).value,
            paymentMethodData = (updatedConfirmationOption as? PaymentMethodConfirmationOption.New)?.createParams,
            clientContext = prepareConfirmationTokenClientContextParams(
                confirmationOption.optionsParams
            )
        )
    }

    private fun prepareConfirmationTokenClientContextParams(paymentMethodOptions: PaymentMethodOptionsParams?):
        ConfirmationTokenClientContextParams {
        return with (intentConfiguration.toDeferredIntentParams()) {
            ConfirmationTokenClientContextParams(
                mode = mode.code,
                currency = mode.currency,
                setupFutureUsage = mode.setupFutureUsage?.code,
                captureMethod = (mode as? DeferredIntentParams.Mode.Payment)?.captureMethod?.code,
                paymentMethodTypes = paymentMethodTypes,
                onBehalfOf = onBehalfOf,
                paymentMethodConfiguration = paymentMethodConfigurationId,
                customer = TODO(),
                paymentMethodOptions = paymentMethodOptions,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            intentConfiguration: PaymentSheet.IntentConfiguration,
            createIntentCallback: CreateIntentWithConfirmationTokenCallback,
            ephemeralKeySecret: String?,
        ): ConfirmationTokenConfirmationInterceptor
    }

    companion object {
        private const val ERROR_MISSING_PAYMENT_METHOD_ID = "PaymentMethod must have an ID"
        private const val ERROR_MISSING_EPHEMERAL_KEY_SECRET =
            "Ephemeral key secret is required to confirm with saved payment method"
    }
}

internal class CreateIntentWithConfirmationTokenCallbackFailureException(
    override val cause: Throwable?
) : StripeException() {
    override fun analyticsValue(): String = "merchantReturnedCreateIntentWithConfirmationTokenCallbackFailure"
}
