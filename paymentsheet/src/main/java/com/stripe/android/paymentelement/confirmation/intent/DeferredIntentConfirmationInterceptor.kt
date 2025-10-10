package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.RadarOptions
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.setupFutureUsage
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
import com.stripe.android.paymentelement.confirmation.utils.ConfirmActionHelper
import com.stripe.android.paymentelement.confirmation.utils.toConfirmParamsSetupFutureUsage
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.DeferredIntentValidator
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.utils.hasIntentToSetup
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Named

internal class DeferredIntentConfirmationInterceptor @AssistedInject constructor(
    @Assisted private val intentConfiguration: PaymentSheet.IntentConfiguration,
    @Assisted private val createIntentCallback: CreateIntentCallback,
    private val stripeRepository: StripeRepository,
    private val requestOptions: ApiRequest.Options,
    @Named(ALLOWS_MANUAL_CONFIRMATION) private val allowsManualConfirmation: Boolean,
) : IntentConfirmationInterceptor {
    private val confirmActionHelper: ConfirmActionHelper = ConfirmActionHelper(requestOptions.apiKeyIsLiveMode)

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmationDefinition.Action<Args> {
        return handleNewPaymentMethod(
            intentConfiguration = intentConfiguration,
            intent = intent,
            confirmationOption = confirmationOption.updatedForDeferredIntent(intentConfiguration),
            shippingValues = shippingValues,
            customerRequestedSave = confirmationOption.shouldSave,
        )
    }

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        ephemeralKeySecret: String?,
    ): ConfirmationDefinition.Action<Args> {
        return handleSavedPaymentMethod(
            intentConfiguration = intentConfiguration,
            intent = intent,
            confirmationOption = confirmationOption.updatedForDeferredIntent(intentConfiguration),
            paymentMethod = confirmationOption.paymentMethod,
            shippingValues = shippingValues,
            hCaptchaToken = confirmationOption.hCaptchaToken
        )
    }

    private suspend fun handleNewPaymentMethod(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        customerRequestedSave: Boolean,
    ): ConfirmationDefinition.Action<Args> {
        return stripeRepository.createPaymentMethod(
            paymentMethodCreateParams = confirmationOption.createParams,
            options = requestOptions,
        ).fold(
            onSuccess = { paymentMethod ->
                handleDeferredIntentCreationFromPaymentMethod(
                    intent = intent,
                    intentConfiguration = intentConfiguration,
                    paymentMethod = paymentMethod,
                    confirmationOption = confirmationOption,
                    shippingValues = shippingValues,
                    shouldSavePaymentMethod = customerRequestedSave || shouldSavePaymentMethod(
                        paymentMethodOptionsParams = confirmationOption.optionsParams,
                        intentConfiguration = intentConfiguration
                    ),
                    hCaptchaToken = null
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

    private suspend fun handleSavedPaymentMethod(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        intent: StripeIntent,
        paymentMethod: PaymentMethod,
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        hCaptchaToken: String?
    ): ConfirmationDefinition.Action<Args> {
        return handleDeferredIntentCreationFromPaymentMethod(
            intent = intent,
            intentConfiguration = intentConfiguration,
            paymentMethod = paymentMethod,
            confirmationOption = confirmationOption,
            shippingValues = shippingValues,
            shouldSavePaymentMethod = shouldSavePaymentMethod(
                paymentMethodOptionsParams = confirmationOption.optionsParams,
                intentConfiguration = intentConfiguration
            ),
            hCaptchaToken = hCaptchaToken
        )
    }

    private suspend fun handleDeferredIntentCreationFromPaymentMethod(
        intent: StripeIntent,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        confirmationOption: PaymentMethodConfirmationOption,
        paymentMethod: PaymentMethod,
        shouldSavePaymentMethod: Boolean,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        hCaptchaToken: String?
    ): ConfirmationDefinition.Action<Args> {
        val result = createIntentCallback.onCreateIntent(
            paymentMethod = paymentMethod,
            shouldSavePaymentMethod = shouldSavePaymentMethod,
        )

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
                        confirmationOption = confirmationOption,
                        paymentMethod = paymentMethod,
                        shippingValues = shippingValues,
                        hCaptchaToken = hCaptchaToken
                    )
                }
            }

            is CreateIntentResult.Failure -> {
                val exception = CreateIntentCallbackFailureException(result.cause)
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
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        hCaptchaToken: String?
    ): ConfirmationDefinition.Action<Args> {
        return stripeRepository.retrieveStripeIntent(
            clientSecret = clientSecret,
            options = requestOptions,
        ).mapCatching { intent ->
            if (intent.isConfirmed) {
                failIfSetAsDefaultFeatureIsEnabled(
                    (confirmationOption as? PaymentMethodConfirmationOption.New)?.extraParams
                )

                ConfirmationDefinition.Action.Complete(
                    intent = intent,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
                    completedFullPaymentFlow = true,
                )
            } else if (intent.requiresAction()) {
                runCatching<ConfirmationDefinition.Action<Args>> {
                    DeferredIntentValidator.validatePaymentMethod(intent, paymentMethod)
                    ConfirmationDefinition.Action.Launch(
                        launcherArguments = Args.NextAction(clientSecret),
                        deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
                        receivesResultInProcess = false,
                    )
                }.getOrElse {
                    ConfirmationDefinition.Action.Fail(
                        cause = InvalidDeferredIntentUsageException(),
                        message = resolvableString(R.string.stripe_paymentsheet_invalid_deferred_intent_usage),
                        errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                    )
                }
            } else {
                DeferredIntentValidator.validate(intent, intentConfiguration, allowsManualConfirmation, paymentMethod)
                confirmActionHelper.createConfirmAction(
                    clientSecret,
                    intent,
                    shippingValues,
                    isDeferred = true
                ) {
                    create(
                        paymentMethod = paymentMethod,
                        optionsParams = confirmationOption.optionsParams,
                        extraParams = (confirmationOption as? PaymentMethodConfirmationOption.New)
                            ?.extraParams,
                        intentConfigSetupFutureUsage = intentConfiguration
                            .mode.setupFutureUse?.toConfirmParamsSetupFutureUsage(),
                        radarOptions = hCaptchaToken?.let { RadarOptions(it) },
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

    private fun failIfSetAsDefaultFeatureIsEnabled(paymentMethodExtraParams: PaymentMethodExtraParams?) {
        // Ideally, we would crash anytime the set as default checkbox is shown, rather than just when it is checked.
        // We could check if it is shown by asserting that setAsDefault != null instead of asserting that it is true.
        // However, we don't have good end-to-end test coverage of this for now, so if we made a change to start
        // sending the set as default flag as false more frequently, we could accidentally start failing here more
        // often as well.
        val setAsDefaultChecked = when (paymentMethodExtraParams) {
            is PaymentMethodExtraParams.Card -> paymentMethodExtraParams.setAsDefault == true
            is PaymentMethodExtraParams.USBankAccount -> paymentMethodExtraParams.setAsDefault == true
            is PaymentMethodExtraParams.Link -> paymentMethodExtraParams.setAsDefault == true
            is PaymentMethodExtraParams.SepaDebit -> paymentMethodExtraParams.setAsDefault == true
            is PaymentMethodExtraParams.BacsDebit, null -> false
        }

        if (setAsDefaultChecked && !requestOptions.apiKeyIsLiveMode) {
            throw IllegalStateException(
                "(Test-mode only error) The default payment methods feature is not yet supported with deferred " +
                    "server-side confirmation. Please contact us if you'd like to use this feature via a Github " +
                    "issue on stripe-android."
            )
        }
    }

    private fun shouldSavePaymentMethod(
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        intentConfiguration: PaymentSheet.IntentConfiguration
    ): Boolean {
        return paymentMethodOptionsParams?.setupFutureUsage()?.hasIntentToSetup() == true ||
            (intentConfiguration.mode as? PaymentSheet.IntentConfiguration.Mode.Payment)
                ?.setupFutureUse?.toConfirmParamsSetupFutureUsage()?.hasIntentToSetup() == true
    }

    @AssistedFactory
    interface Factory {
        fun create(
            intentConfiguration: PaymentSheet.IntentConfiguration,
            createIntentCallback: CreateIntentCallback
        ): DeferredIntentConfirmationInterceptor
    }
}
