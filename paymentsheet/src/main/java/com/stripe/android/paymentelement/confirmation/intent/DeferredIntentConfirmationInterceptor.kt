package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.ConfirmStripeIntentParamsFactory
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.RadarOptions
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.setupFutureUsage
import com.stripe.android.model.updateSetupFutureUsageWithPmoSfu
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor.Companion.PROVIDER_FETCH_INTERVAL
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor.Companion.PROVIDER_FETCH_TIMEOUT
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.DeferredIntentValidator
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.utils.hasIntentToSetup
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Named
import javax.inject.Provider
import kotlin.time.Duration.Companion.seconds
import com.stripe.android.R as PaymentsCoreR

internal class DeferredIntentConfirmationInterceptor @AssistedInject constructor(
    @Assisted private val intentConfiguration: PaymentSheet.IntentConfiguration,
    private val stripeRepository: StripeRepository,
    private val errorReporter: ErrorReporter,
    private val intentCreationCallbackProvider: Provider<CreateIntentCallback?>,
    private val requestOptions: ApiRequest.Options,
    @Named(ALLOWS_MANUAL_CONFIRMATION) private val allowsManualConfirmation: Boolean,
) : IntentConfirmationInterceptor {

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmationDefinition.Action<Args> {
        return handleNewPaymentMethod(
            intentConfiguration = intentConfiguration,
            intent = intent,
            paymentMethodCreateParams = confirmationOption.createParams,
            paymentMethodOptionsParams = confirmationOption.optionsParams,
            paymentMethodExtraParams = confirmationOption.extraParams,
            shippingValues = shippingValues,
            customerRequestedSave = confirmationOption.shouldSave,
        )
    }

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmationDefinition.Action<Args> {
        return handleSavedPaymentMethod(
            intentConfiguration = intentConfiguration,
            intent = intent,
            paymentMethod = confirmationOption.paymentMethod,
            paymentMethodOptionsParams = confirmationOption.optionsParams,
            paymentMethodExtraParams = null,
            shippingValues = shippingValues,
            hCaptchaToken = confirmationOption.hCaptchaToken
        )
    }

    internal suspend fun handleNewPaymentMethod(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        intent: StripeIntent,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        customerRequestedSave: Boolean,
    ): ConfirmationDefinition.Action<Args> {
        val productUsage = buildSet {
            addAll(paymentMethodCreateParams.attribution)
            add("deferred-intent")
            if (intentConfiguration.paymentMethodTypes.isEmpty()) {
                add("autopm")
            }
        }

        val params = paymentMethodCreateParams.copy(
            productUsage = productUsage,
        )

        return stripeRepository.createPaymentMethod(
            paymentMethodCreateParams = params,
            options = requestOptions,
        ).fold(
            onSuccess = { paymentMethod ->
                handleDeferredIntent(
                    intent = intent,
                    intentConfiguration = intentConfiguration,
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = updatePaymentMethodOptionsParams(
                        code = paymentMethodCreateParams.typeCode,
                        intentConfiguration = intentConfiguration,
                        paymentMethodOptionsParams = paymentMethodOptionsParams
                    ),
                    paymentMethodExtraParams = paymentMethodExtraParams,
                    shippingValues = shippingValues,
                    shouldSavePaymentMethod = customerRequestedSave || shouldSavePaymentMethod(
                        paymentMethodOptionsParams = paymentMethodOptionsParams,
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

    internal suspend fun handleSavedPaymentMethod(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        intent: StripeIntent,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        hCaptchaToken: String?
    ): ConfirmationDefinition.Action<Args> {
        val updatedPaymentMethodOptionsParams = updatePaymentMethodOptionsParams(
            code = paymentMethod.type?.code,
            intentConfiguration = intentConfiguration,
            paymentMethodOptionsParams = paymentMethodOptionsParams
        )
        return handleDeferredIntent(
            intent = intent,
            intentConfiguration = intentConfiguration,
            paymentMethod = paymentMethod,
            paymentMethodOptionsParams = updatedPaymentMethodOptionsParams,
            paymentMethodExtraParams = paymentMethodExtraParams,
            shippingValues = shippingValues,
            shouldSavePaymentMethod = shouldSavePaymentMethod(
                paymentMethodOptionsParams = updatedPaymentMethodOptionsParams,
                intentConfiguration = intentConfiguration
            ),
            hCaptchaToken = hCaptchaToken
        )
    }

    internal suspend fun handleDeferredIntent(
        intent: StripeIntent,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        shouldSavePaymentMethod: Boolean,
        hCaptchaToken: String?
    ): ConfirmationDefinition.Action<Args> {
        return waitForIntentCallback()?.let { callback ->
            handleDeferredIntentCreationFromPaymentMethod(
                intent = intent,
                createIntentCallback = callback,
                intentConfiguration = intentConfiguration,
                paymentMethod = paymentMethod,
                paymentMethodOptionsParams = paymentMethodOptionsParams,
                paymentMethodExtraParams = paymentMethodExtraParams,
                shouldSavePaymentMethod = shouldSavePaymentMethod,
                shippingValues = shippingValues,
                hCaptchaToken = hCaptchaToken
            )
        } ?: run {
            val error = "${CreateIntentCallback::class.java.simpleName} must be implemented " +
                "when using IntentConfiguration with PaymentSheet"

            errorReporter.report(ErrorReporter.ExpectedErrorEvent.CREATE_INTENT_CALLBACK_NULL)

            return ConfirmationDefinition.Action.Fail(
                cause = IllegalStateException(error),
                message = if (requestOptions.apiKeyIsLiveMode) {
                    PaymentsCoreR.string.stripe_internal_error.resolvableString
                } else {
                    error.resolvableString
                },
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        }
    }

    private fun retrieveCallback(): CreateIntentCallback? {
        return intentCreationCallbackProvider.get()
    }

    private suspend fun waitForIntentCallback(): CreateIntentCallback? {
        return retrieveCallback() ?: run {
            val callback = withTimeoutOrNull(PROVIDER_FETCH_TIMEOUT.seconds) {
                var intentCallback: CreateIntentCallback? = null

                while (intentCallback == null) {
                    delay(PROVIDER_FETCH_INTERVAL)
                    intentCallback = retrieveCallback()
                }

                intentCallback
            }

            if (callback != null) {
                errorReporter.report(ErrorReporter.SuccessEvent.FOUND_CREATE_INTENT_CALLBACK_WHILE_POLLING)
            }

            callback
        }
    }

    private suspend fun handleDeferredIntentCreationFromPaymentMethod(
        intent: StripeIntent,
        createIntentCallback: CreateIntentCallback,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
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
                        paymentMethod = paymentMethod,
                        paymentMethodOptionsParams = paymentMethodOptionsParams,
                        paymentMethodExtraParams = paymentMethodExtraParams,
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
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        hCaptchaToken: String?
    ): ConfirmationDefinition.Action<Args> {
        return stripeRepository.retrieveStripeIntent(
            clientSecret = clientSecret,
            options = requestOptions,
        ).mapCatching { intent ->
            if (intent.isConfirmed) {
                failIfSetAsDefaultFeatureIsEnabled(paymentMethodExtraParams)

                ConfirmationDefinition.Action.Complete(
                    intent = intent,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
                    completedFullPaymentFlow = true,
                )
            } else if (intent.requiresAction()) {
                createNextAction(clientSecret, intent, paymentMethod)
            } else {
                DeferredIntentValidator.validate(intent, intentConfiguration, allowsManualConfirmation, paymentMethod)
                createConfirmAction(
                    clientSecret,
                    intent,
                    shippingValues,
                    paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                    isDeferred = true,
                    intentConfigSetupFutureUsage = intentConfiguration
                        .mode.setupFutureUse?.toConfirmParamsSetupFutureUsage(),
                    hCaptchaToken = hCaptchaToken
                )
            }
        }.getOrElse { error ->
            ConfirmationDefinition.Action.Fail(
                cause = error,
                message = error.stripeErrorMessage(),
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        }
    }

    private fun createNextAction(
        clientSecret: String,
        intent: StripeIntent,
        paymentMethod: PaymentMethod
    ): ConfirmationDefinition.Action<Args> {
        return runCatching<ConfirmationDefinition.Action<Args>> {
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
    }

    private fun createConfirmAction(
        clientSecret: String,
        intent: StripeIntent,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        isDeferred: Boolean,
        intentConfigSetupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
        hCaptchaToken: String?
    ): ConfirmationDefinition.Action<Args> {
        val factory = ConfirmStripeIntentParamsFactory.createFactory(
            clientSecret = clientSecret,
            intent = intent,
            shipping = shippingValues,
        ) ?: run {
            val exception = InvalidClientSecretException(clientSecret, intent)

            return createFailAction(exception, exception.message)
        }

        val confirmParams = factory.create(
            paymentMethod = paymentMethod,
            optionsParams = paymentMethodOptionsParams,
            extraParams = paymentMethodExtraParams,
            intentConfigSetupFutureUsage = intentConfigSetupFutureUsage,
            radarOptions = hCaptchaToken?.let { RadarOptions(it) }
        )
        return ConfirmationDefinition.Action.Launch(
            launcherArguments = Args.Confirm(confirmParams),
            deferredIntentConfirmationType = DeferredIntentConfirmationType.Client.takeIf { isDeferred },
            receivesResultInProcess = false,
        )
    }

    private fun createFailAction(
        exception: Exception,
        message: String,
    ): ConfirmationDefinition.Action<Args> {
        return ConfirmationDefinition.Action.Fail(
            cause = exception,
            message = if (requestOptions.apiKeyIsLiveMode) {
                PaymentsCoreR.string.stripe_internal_error.resolvableString
            } else {
                message.resolvableString
            },
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
        )
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

    private fun PaymentSheet.IntentConfiguration.SetupFutureUse.toConfirmParamsSetupFutureUsage():
        ConfirmPaymentIntentParams.SetupFutureUsage {
        return when (this) {
            PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession -> {
                ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
            }
            PaymentSheet.IntentConfiguration.SetupFutureUse.OnSession -> {
                ConfirmPaymentIntentParams.SetupFutureUsage.OnSession
            }
            PaymentSheet.IntentConfiguration.SetupFutureUse.None -> {
                ConfirmPaymentIntentParams.SetupFutureUsage.None
            }
        }
    }

    /**
     * [PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions] does not require setting PMO SFU on the
     * intent. If PMO SFU value exists in the configuration, set it in the PaymentMethodOptionsParams.
     */
    @OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
    private fun updatePaymentMethodOptionsParams(
        code: PaymentMethodCode?,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?
    ): PaymentMethodOptionsParams? {
        val paymentMethodType = PaymentMethod.Type.fromCode(code) ?: return paymentMethodOptionsParams
        return (intentConfiguration.mode as? PaymentSheet.IntentConfiguration.Mode.Payment)
            ?.paymentMethodOptions
            ?.setupFutureUsageValues?.let { values ->
                values[paymentMethodType]?.toConfirmParamsSetupFutureUsage()?.let { configPmoSfu ->
                    if (paymentMethodOptionsParams != null) {
                        paymentMethodOptionsParams.updateSetupFutureUsageWithPmoSfu(configPmoSfu)
                    } else {
                        PaymentMethodOptionsParams.SetupFutureUsage(
                            paymentMethodType = paymentMethodType,
                            setupFutureUsage = configPmoSfu
                        )
                    }
                }
            } ?: paymentMethodOptionsParams
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
        fun create(intentConfiguration: PaymentSheet.IntentConfiguration): DeferredIntentConfirmationInterceptor
    }
}
