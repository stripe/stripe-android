package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.ConfirmStripeIntentParamsFactory
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.setupFutureUsage
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor.NextStep
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.DeferredIntentValidator
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.seconds
import com.stripe.android.R as PaymentsCoreR

internal interface IntentConfirmationInterceptor {

    sealed interface NextStep {

        val deferredIntentConfirmationType: DeferredIntentConfirmationType?

        data class Fail(
            val cause: Throwable,
            val message: ResolvableString,
        ) : NextStep {

            override val deferredIntentConfirmationType: DeferredIntentConfirmationType?
                get() = null
        }

        data class Confirm(
            val confirmParams: ConfirmStripeIntentParams,
            val isDeferred: Boolean,
        ) : NextStep {

            override val deferredIntentConfirmationType: DeferredIntentConfirmationType?
                get() = DeferredIntentConfirmationType.Client.takeIf { isDeferred }
        }

        data class HandleNextAction(val clientSecret: String) : NextStep {

            override val deferredIntentConfirmationType: DeferredIntentConfirmationType
                get() = DeferredIntentConfirmationType.Server
        }

        data class Complete(val isForceSuccess: Boolean) : NextStep {

            override val deferredIntentConfirmationType: DeferredIntentConfirmationType
                get() = if (isForceSuccess) {
                    DeferredIntentConfirmationType.None
                } else {
                    DeferredIntentConfirmationType.Server
                }
        }
    }

    suspend fun intercept(
        initializationMode: PaymentElementLoader.InitializationMode,
        intent: StripeIntent,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
        paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        customerRequestedSave: Boolean,
    ): NextStep

    suspend fun intercept(
        initializationMode: PaymentElementLoader.InitializationMode,
        intent: StripeIntent,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): NextStep

    companion object {
        var createIntentCallback: CreateIntentCallback? = null

        const val COMPLETE_WITHOUT_CONFIRMING_INTENT = "COMPLETE_WITHOUT_CONFIRMING_INTENT"
    }
}

internal enum class DeferredIntentConfirmationType(val value: String) {
    Client("client"),
    Server("server"),
    None("none")
}

internal class InvalidDeferredIntentUsageException : StripeException() {
    override fun analyticsValue(): String = "invalidDeferredIntentUsage"

    override val message: String = """
        The payment method on the intent doesn't match the one provided in the createIntentCallback. When using deferred
        intent creation, ensure you're either creating a new intent with the correct payment method or updating an
        existing intent with the new payment method ID.
    """.trimIndent()
}

internal class CreateIntentCallbackFailureException(override val cause: Throwable?) : StripeException() {
    override fun analyticsValue(): String = "merchantReturnedCreateIntentCallbackFailure"
}

internal class InvalidClientSecretException(
    val clientSecret: String,
    val intent: StripeIntent,
) : StripeException() {
    private val intentType = when (intent) {
        is PaymentIntent -> "PaymentIntent"
        is SetupIntent -> "SetupIntent"
    }

    override fun analyticsValue(): String = "invalidClientSecretProvided"

    override val message: String = """
        Encountered an invalid client secret "$clientSecret" for intent type "$intentType"
    """.trimIndent()
}

internal class DefaultIntentConfirmationInterceptor @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val errorReporter: ErrorReporter,
    @Named(ALLOWS_MANUAL_CONFIRMATION) private val allowsManualConfirmation: Boolean,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
) : IntentConfirmationInterceptor {
    private val requestOptions: ApiRequest.Options
        get() = ApiRequest.Options(
            apiKey = publishableKeyProvider(),
            stripeAccount = stripeAccountIdProvider(),
        )

    override suspend fun intercept(
        initializationMode: PaymentElementLoader.InitializationMode,
        intent: StripeIntent,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        customerRequestedSave: Boolean,
    ): NextStep {
        return when (initializationMode) {
            is PaymentElementLoader.InitializationMode.DeferredIntent -> {
                /*
                 * We don't pass the created intent in `DeferredIntent` mode because we rely on the created intent
                 * from the merchant through `CreateIntentCallback`. The intent passed through here is created from
                 * `PaymentSheet.Configuration` in order to populate `Payment Element` data.
                 */
                handleDeferredIntent(
                    intentConfiguration = initializationMode.intentConfiguration,
                    shippingValues = shippingValues,
                    paymentMethodCreateParams = paymentMethodCreateParams,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                    customerRequestedSave = customerRequestedSave,
                )
            }

            is PaymentElementLoader.InitializationMode.PaymentIntent -> {
                createConfirmStep(
                    clientSecret = initializationMode.clientSecret,
                    intent = intent,
                    shippingValues = shippingValues,
                    paymentMethodCreateParams = paymentMethodCreateParams,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                )
            }

            is PaymentElementLoader.InitializationMode.SetupIntent -> {
                createConfirmStep(
                    clientSecret = initializationMode.clientSecret,
                    intent = intent,
                    shippingValues = shippingValues,
                    paymentMethodCreateParams = paymentMethodCreateParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                )
            }
        }
    }

    override suspend fun intercept(
        initializationMode: PaymentElementLoader.InitializationMode,
        intent: StripeIntent,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): NextStep {
        return when (initializationMode) {
            is PaymentElementLoader.InitializationMode.DeferredIntent -> {
                val offSession = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                handleDeferredIntent(
                    intentConfiguration = initializationMode.intentConfiguration,
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                    shippingValues = shippingValues,
                    shouldSavePaymentMethod = paymentMethodOptionsParams?.setupFutureUsage() == offSession,
                )
            }

            is PaymentElementLoader.InitializationMode.PaymentIntent -> {
                createConfirmStep(
                    clientSecret = initializationMode.clientSecret,
                    intent = intent,
                    shippingValues = shippingValues,
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                    isDeferred = false,
                )
            }

            is PaymentElementLoader.InitializationMode.SetupIntent -> {
                createConfirmStep(
                    clientSecret = initializationMode.clientSecret,
                    intent = intent,
                    shippingValues = shippingValues,
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                    isDeferred = false,
                )
            }
        }
    }

    private suspend fun handleDeferredIntent(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        customerRequestedSave: Boolean,
    ): NextStep {
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

        return createPaymentMethod(params).fold(
            onSuccess = { paymentMethod ->
                handleDeferredIntent(
                    intentConfiguration = intentConfiguration,
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                    shippingValues = shippingValues,
                    shouldSavePaymentMethod = customerRequestedSave,
                )
            },
            onFailure = { error ->
                NextStep.Fail(
                    cause = error,
                    message = resolvableString(GENERIC_STRIPE_MESSAGE),
                )
            }
        )
    }

    private suspend fun handleDeferredIntent(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        shouldSavePaymentMethod: Boolean,
    ): NextStep {
        return when (val callback = waitForIntentCallback()) {
            is CreateIntentCallback -> {
                handleDeferredIntentCreationFromPaymentMethod(
                    createIntentCallback = callback,
                    intentConfiguration = intentConfiguration,
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                    shouldSavePaymentMethod = shouldSavePaymentMethod,
                    shippingValues = shippingValues,
                )
            }

            else -> {
                val error = "${CreateIntentCallback::class.java.simpleName} must be implemented " +
                    "when using IntentConfiguration with PaymentSheet"

                errorReporter.report(ErrorReporter.ExpectedErrorEvent.CREATE_INTENT_CALLBACK_NULL)

                NextStep.Fail(
                    cause = IllegalStateException(error),
                    message = if (requestOptions.apiKeyIsLiveMode) {
                        PaymentsCoreR.string.stripe_internal_error.resolvableString
                    } else {
                        error.resolvableString
                    }
                )
            }
        }
    }

    private suspend fun createPaymentMethod(
        params: PaymentMethodCreateParams,
    ): Result<PaymentMethod> {
        return stripeRepository.createPaymentMethod(
            paymentMethodCreateParams = params,
            options = requestOptions,
        )
    }

    private suspend fun waitForIntentCallback(): CreateIntentCallback? {
        return retrieveCallback() ?: run {
            val callback = withTimeoutOrNull(INTENT_CALLBACK_FETCH_TIMEOUT.seconds) {
                var intentCallback: CreateIntentCallback? = null

                while (intentCallback == null) {
                    delay(INTENT_CALLBACK_FETCH_INTERVAL)
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

    private fun retrieveCallback(): CreateIntentCallback? {
        return IntentConfirmationInterceptor.createIntentCallback
    }

    private suspend fun handleDeferredIntentCreationFromPaymentMethod(
        createIntentCallback: CreateIntentCallback,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shouldSavePaymentMethod: Boolean,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): NextStep {
        val result = createIntentCallback.onCreateIntent(
            paymentMethod = paymentMethod,
            shouldSavePaymentMethod = shouldSavePaymentMethod,
        )

        return when (result) {
            is CreateIntentResult.Success -> {
                if (result.clientSecret == IntentConfirmationInterceptor.COMPLETE_WITHOUT_CONFIRMING_INTENT) {
                    NextStep.Complete(isForceSuccess = true)
                } else {
                    handleDeferredIntentCreationSuccess(
                        clientSecret = result.clientSecret,
                        intentConfiguration = intentConfiguration,
                        paymentMethod = paymentMethod,
                        paymentMethodOptionsParams = paymentMethodOptionsParams,
                        paymentMethodExtraParams = paymentMethodExtraParams,
                        shippingValues = shippingValues,
                    )
                }
            }

            is CreateIntentResult.Failure -> {
                NextStep.Fail(
                    cause = CreateIntentCallbackFailureException(result.cause),
                    message = result.displayMessage?.resolvableString
                        ?: resolvableString(GENERIC_STRIPE_MESSAGE),
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
    ): NextStep {
        return retrieveStripeIntent(clientSecret).mapCatching { intent ->
            if (intent.isConfirmed) {
                failIfSetAsDefaultFeatureIsEnabled(paymentMethodExtraParams)
                NextStep.Complete(isForceSuccess = false)
            } else if (intent.requiresAction()) {
                val attachedPaymentMethodId = intent.paymentMethodId

                if (attachedPaymentMethodId != null && attachedPaymentMethodId != paymentMethod.id) {
                    NextStep.Fail(
                        cause = InvalidDeferredIntentUsageException(),
                        message = resolvableString(R.string.stripe_paymentsheet_invalid_deferred_intent_usage),
                    )
                } else {
                    NextStep.HandleNextAction(clientSecret)
                }
            } else {
                DeferredIntentValidator.validate(intent, intentConfiguration, allowsManualConfirmation)
                createConfirmStep(
                    clientSecret,
                    intent,
                    shippingValues,
                    paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                    isDeferred = true,
                )
            }
        }.getOrElse { error ->
            NextStep.Fail(
                cause = error,
                message = resolvableString(GENERIC_STRIPE_MESSAGE),
            )
        }
    }

    private suspend fun retrieveStripeIntent(clientSecret: String): Result<StripeIntent> {
        return stripeRepository.retrieveStripeIntent(
            clientSecret = clientSecret,
            options = requestOptions,
        )
    }

    private fun createConfirmStep(
        clientSecret: String,
        intent: StripeIntent,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        isDeferred: Boolean,
    ): NextStep {
        val factory = ConfirmStripeIntentParamsFactory.createFactory(
            clientSecret = clientSecret,
            intent = intent,
            shipping = shippingValues,
        ) ?: run {
            val exception = InvalidClientSecretException(clientSecret, intent)

            return createFailStep(exception, exception.message)
        }

        val confirmParams = factory.create(
            paymentMethod = paymentMethod,
            optionsParams = paymentMethodOptionsParams,
            extraParams = paymentMethodExtraParams,
        )
        return NextStep.Confirm(
            confirmParams = confirmParams,
            isDeferred = isDeferred,
        )
    }

    private fun createConfirmStep(
        clientSecret: String,
        intent: StripeIntent,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
        paymentMethodExtraParams: PaymentMethodExtraParams? = null,
    ): NextStep {
        val paramsFactory = ConfirmStripeIntentParamsFactory.createFactory(
            clientSecret = clientSecret,
            intent = intent,
            shipping = shippingValues,
        ) ?: run {
            val exception = InvalidClientSecretException(clientSecret, intent)

            return createFailStep(exception, exception.message)
        }

        val confirmParams = paramsFactory.create(
            paymentMethodCreateParams,
            paymentMethodOptionsParams,
            paymentMethodExtraParams,
        )

        return NextStep.Confirm(
            confirmParams = confirmParams,
            isDeferred = false,
        )
    }

    private fun createFailStep(
        exception: Exception,
        message: String,
    ): NextStep.Fail {
        return NextStep.Fail(
            cause = exception,
            message = if (requestOptions.apiKeyIsLiveMode) {
                PaymentsCoreR.string.stripe_internal_error.resolvableString
            } else {
                message.resolvableString
            }
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

    private companion object {
        private const val INTENT_CALLBACK_FETCH_TIMEOUT = 2
        private const val INTENT_CALLBACK_FETCH_INTERVAL = 5L
        private val GENERIC_STRIPE_MESSAGE = R.string.stripe_something_went_wrong
    }
}
