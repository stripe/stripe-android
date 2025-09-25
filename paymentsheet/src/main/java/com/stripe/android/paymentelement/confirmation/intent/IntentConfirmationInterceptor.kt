package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.ConfirmStripeIntentParamsFactory
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.exception.GenericStripeException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.utils.errorMessage
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.ConfirmationToken
import com.stripe.android.model.ConfirmationTokenParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.RadarOptions
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.model.setupFutureUsage
import com.stripe.android.model.updateSetupFutureUsageWithPmoSfu
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor.NextStep
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.DeferredIntentValidator
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.utils.hasIntentToSetup
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
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

        data class Complete(
            val isForceSuccess: Boolean,
            val completedFullPaymentFlow: Boolean = true,
        ) : NextStep {

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
        hCaptchaToken: String?,
        ephemeralKeySecret: String?,
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

internal sealed class DeferredIntentCallback {
    data class CreateIntentWithPaymentMethod(
        val delegate: CreateIntentCallback
    ) : DeferredIntentCallback()

    data class CreateIntentWithConfirmationToken(
        val delegate: CreateIntentWithConfirmationTokenCallback
    ) : DeferredIntentCallback()

    object None : DeferredIntentCallback()
}

@OptIn(SharedPaymentTokenSessionPreview::class)
@Suppress("LargeClass")
internal class DefaultIntentConfirmationInterceptor @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val errorReporter: ErrorReporter,
    private val intentCreationCallbackProvider: Provider<CreateIntentCallback?>,
    private val intentCreationWithConfirmationTokenCallbackProvider:
    Provider<CreateIntentWithConfirmationTokenCallback?>,
    private val preparePaymentMethodHandlerProvider: Provider<PreparePaymentMethodHandler?>,
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
                handleDeferred(
                    intentConfiguration = initializationMode.intentConfiguration,
                    shippingValues = shippingValues,
                    paymentMethodCreateParams = paymentMethodCreateParams,
                    paymentMethodOptionsParams = updatePaymentMethodOptionsParams(
                        code = paymentMethodCreateParams.typeCode,
                        intentConfiguration = initializationMode.intentConfiguration,
                        paymentMethodOptionsParams = paymentMethodOptionsParams
                    ),
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
        hCaptchaToken: String?,
        ephemeralKeySecret: String?,
    ): NextStep {
        return when (initializationMode) {
            is PaymentElementLoader.InitializationMode.DeferredIntent -> {
                val updatedPaymentMethodOptionsParams = updatePaymentMethodOptionsParams(
                    code = paymentMethod.type?.code,
                    intentConfiguration = initializationMode.intentConfiguration,
                    paymentMethodOptionsParams = paymentMethodOptionsParams
                )
                handleDeferred(
                    intentConfiguration = initializationMode.intentConfiguration,
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = updatedPaymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                    shippingValues = shippingValues,
                    shouldSavePaymentMethod = shouldSavePaymentMethod(
                        paymentMethodOptionsParams = updatedPaymentMethodOptionsParams,
                        intentConfiguration = initializationMode.intentConfiguration
                    ),
                    hCaptchaToken = hCaptchaToken,
                    ephemeralKeySecret = ephemeralKeySecret,
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
                    intentConfigSetupFutureUsage = null,
                    hCaptchaToken = hCaptchaToken
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
                    intentConfigSetupFutureUsage = null,
                    hCaptchaToken = hCaptchaToken
                )
            }
        }
    }

    private suspend fun handleDeferred(
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

        val callback = waitForIntentCallback()
        return if (callback is DeferredIntentCallback.CreateIntentWithConfirmationToken &&
            intentConfiguration.intentBehavior is PaymentSheet.IntentConfiguration.IntentBehavior.Default
        ) {
            handleDeferredIntentCreationFromConfirmationToken(
                createIntentWithConfirmationTokenCallback = callback.delegate,
                intentConfiguration = intentConfiguration,
                paymentMethodCreateParams = paymentMethodCreateParams,
                paymentMethodOptionsParams = paymentMethodOptionsParams,
                paymentMethodExtraParams = paymentMethodExtraParams,
                shippingValues = shippingValues,
            )
        } else {
            createPaymentMethod(params).fold(
                onSuccess = { paymentMethod ->
                    handleDeferred(
                        intentConfiguration = intentConfiguration,
                        paymentMethod = paymentMethod,
                        paymentMethodOptionsParams = paymentMethodOptionsParams,
                        paymentMethodExtraParams = paymentMethodExtraParams,
                        shippingValues = shippingValues,
                        shouldSavePaymentMethod = customerRequestedSave || shouldSavePaymentMethod(
                            paymentMethodOptionsParams = paymentMethodOptionsParams,
                            intentConfiguration = intentConfiguration
                        ),
                        hCaptchaToken = null,
                        ephemeralKeySecret = null
                    )
                },
                onFailure = { error ->
                    NextStep.Fail(
                        cause = error,
                        message = error.stripeErrorMessage(),
                    )
                }
            )
        }
    }

    private suspend fun handleDeferred(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        shouldSavePaymentMethod: Boolean,
        hCaptchaToken: String?,
        ephemeralKeySecret: String?
    ): NextStep {
        return when (intentConfiguration.intentBehavior) {
            is PaymentSheet.IntentConfiguration.IntentBehavior.Default -> handleDeferredIntent(
                intentConfiguration = intentConfiguration,
                paymentMethod = paymentMethod,
                paymentMethodOptionsParams = paymentMethodOptionsParams,
                paymentMethodExtraParams = paymentMethodExtraParams,
                shippingValues = shippingValues,
                shouldSavePaymentMethod = shouldSavePaymentMethod,
                hCaptchaToken = hCaptchaToken,
                ephemeralKeySecret = ephemeralKeySecret,
            )
            is PaymentSheet.IntentConfiguration.IntentBehavior.SharedPaymentToken -> handlePreparePaymentMethod(
                paymentMethod = paymentMethod,
                shippingValues = shippingValues,
            )
        }
    }

    private suspend fun handleDeferredIntent(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        shouldSavePaymentMethod: Boolean,
        hCaptchaToken: String?,
        ephemeralKeySecret: String?,
    ): NextStep {
        return when (val callback = waitForIntentCallback()) {
            is DeferredIntentCallback.CreateIntentWithPaymentMethod -> {
                handleDeferredIntentCreationFromPaymentMethod(
                    createIntentCallback = callback.delegate,
                    intentConfiguration = intentConfiguration,
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                    shouldSavePaymentMethod = shouldSavePaymentMethod,
                    shippingValues = shippingValues,
                    hCaptchaToken = hCaptchaToken
                )
            }

            is DeferredIntentCallback.CreateIntentWithConfirmationToken -> {
                handleDeferredIntentCreationFromConfirmationToken(
                    createIntentWithConfirmationTokenCallback = callback.delegate,
                    intentConfiguration = intentConfiguration,
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                    shippingValues = shippingValues,
                    hCaptchaToken = hCaptchaToken,
                    ephemeralKeySecret = ephemeralKeySecret
                )
            }

            is DeferredIntentCallback.None -> {
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

    private suspend fun handlePreparePaymentMethod(
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): NextStep {
        runCatching {
            stripeRepository.createSavedPaymentMethodRadarSession(
                paymentMethodId = paymentMethod.id
                    ?: throw GenericStripeException(
                        cause = IllegalStateException(
                            "No payment method ID was found for provided 'PaymentMethod' object!"
                        ),
                        analyticsValue = "noPaymentMethodId"
                    ),
                requestOptions = requestOptions,
            ).getOrThrow()
        }.onFailure {
            errorReporter.report(
                ErrorReporter.ExpectedErrorEvent.SAVED_PAYMENT_METHOD_RADAR_SESSION_FAILURE,
                stripeException = StripeException.create(it),
            )
        }

        return when (val handler = waitForPreparePaymentMethodHandler()) {
            is PreparePaymentMethodHandler -> {
                try {
                    handler.onPreparePaymentMethod(
                        paymentMethod = paymentMethod,
                        shippingAddress = shippingValues?.toAddressDetails(),
                    )

                    NextStep.Complete(isForceSuccess = true, completedFullPaymentFlow = false)
                } catch (exception: Exception) {
                    NextStep.Fail(
                        cause = exception,
                        message = exception.errorMessage,
                    )
                }
            }

            else -> {
                val error = "${PreparePaymentMethodHandler::class.java.simpleName} must be implemented " +
                    "when using IntentConfiguration with shared payment tokens!"

                errorReporter.report(ErrorReporter.ExpectedErrorEvent.PREPARE_PAYMENT_METHOD_HANDLER_NULL)

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

    private suspend fun waitForIntentCallback(): DeferredIntentCallback {
        return coroutineScope {
            val callbackWithPaymentMethod = async { waitForCreateIntentWithPaymentMethodCallback() }
            val callbackWithConfirmationToken = async { waitForCreateIntentWithConfirmationTokenCallback() }

            when (
                val result = select {
                    callbackWithPaymentMethod.onAwait { it }
                    callbackWithConfirmationToken.onAwait { it }
                }
            ) {
                is CreateIntentCallback -> {
                    DeferredIntentCallback.CreateIntentWithPaymentMethod(result)
                }
                is CreateIntentWithConfirmationTokenCallback -> {
                    DeferredIntentCallback.CreateIntentWithConfirmationToken(result)
                }
                else -> {
                    DeferredIntentCallback.None
                }
            }
        }
    }

    private suspend fun waitForCreateIntentWithPaymentMethodCallback(): CreateIntentCallback? {
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

    private suspend fun waitForCreateIntentWithConfirmationTokenCallback(): CreateIntentWithConfirmationTokenCallback? {
        return retrieveCreateIntentWithConfirmationTokenCallback() ?: run {
            val callback = withTimeoutOrNull(PROVIDER_FETCH_TIMEOUT.seconds) {
                var callback: CreateIntentWithConfirmationTokenCallback? = null

                while (callback == null) {
                    delay(PROVIDER_FETCH_INTERVAL)
                    callback = retrieveCreateIntentWithConfirmationTokenCallback()
                }

                callback
            }

            if (callback != null) {
                errorReporter.report(
                    ErrorReporter.SuccessEvent
                        .FOUND_CREATE_INTENT_WITH_CONFIRMATION_TOKEN_CALLBACK_WHILE_POLLING
                )
            }

            callback
        }
    }

    private suspend fun waitForPreparePaymentMethodHandler(): PreparePaymentMethodHandler? {
        return retrievePreparePaymentMethodHandler() ?: run {
            val handler = withTimeoutOrNull(PROVIDER_FETCH_TIMEOUT.seconds) {
                var handler: PreparePaymentMethodHandler? = null

                while (handler == null) {
                    delay(PROVIDER_FETCH_INTERVAL)
                    handler = retrievePreparePaymentMethodHandler()
                }

                handler
            }

            if (handler != null) {
                errorReporter.report(ErrorReporter.SuccessEvent.FOUND_PREPARE_PAYMENT_METHOD_HANDLER_WHILE_POLLING)
            }

            handler
        }
    }

    private fun retrieveCallback(): CreateIntentCallback? {
        return intentCreationCallbackProvider.get()
    }

    private fun retrieveCreateIntentWithConfirmationTokenCallback(): CreateIntentWithConfirmationTokenCallback? {
        return intentCreationWithConfirmationTokenCallbackProvider.get()
    }

    private fun retrievePreparePaymentMethodHandler(): PreparePaymentMethodHandler? {
        return preparePaymentMethodHandlerProvider.get()
    }

    private suspend fun handleDeferredIntentCreationFromPaymentMethod(
        createIntentCallback: CreateIntentCallback,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shouldSavePaymentMethod: Boolean,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        hCaptchaToken: String?
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
                        hCaptchaToken = hCaptchaToken
                    )
                }
            }

            is CreateIntentResult.Failure -> {
                val exception = CreateIntentCallbackFailureException(result.cause)
                NextStep.Fail(
                    cause = exception,
                    message = result.displayMessage?.resolvableString
                        ?: exception.stripeErrorMessage(),
                )
            }
        }
    }

    private suspend fun handleDeferredIntentCreationFromConfirmationToken(
        createIntentWithConfirmationTokenCallback: CreateIntentWithConfirmationTokenCallback,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): NextStep {
        return stripeRepository.createConfirmationToken(
            confirmationTokenParams = ConfirmationTokenParams(
                paymentMethodData = paymentMethodCreateParams,
                setUpFutureUsage = paymentMethodOptionsParams?.setupFutureUsage(),
            ),
            options = requestOptions,
        ).fold(
            onSuccess = { confirmationToken ->
                val paymentMethodPreview = confirmationToken.paymentMethodPreview
                    ?: return NextStep.Fail(
                        cause = IllegalStateException("Failed to fetch PaymentMethod"),
                        message = "Failed to fetch PaymentMethod".resolvableString,
                    )
                val paymentMethod = PaymentMethodJsonParser().parse(
                    JSONObject(paymentMethodPreview.allResponseFields)
                )
                handleDeferredOnConfirmationTokenCreated(
                    callback = createIntentWithConfirmationTokenCallback,
                    confirmationToken = confirmationToken,
                    intentConfiguration = intentConfiguration,
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                    shippingValues = shippingValues,
                    hCaptchaToken = null,
                )
            },
            onFailure = { error ->
                NextStep.Fail(
                    cause = error,
                    message = error.stripeErrorMessage(),
                )
            }
        )
    }

    private suspend fun handleDeferredIntentCreationFromConfirmationToken(
        createIntentWithConfirmationTokenCallback: CreateIntentWithConfirmationTokenCallback,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        hCaptchaToken: String?,
        ephemeralKeySecret: String?
    ): NextStep {
        return stripeRepository.createConfirmationToken(
            confirmationTokenParams = ConfirmationTokenParams(
                paymentMethodId = paymentMethod.id
                    ?: return NextStep.Fail(
                        cause = IllegalStateException("PaymentMethod must have an ID"),
                        message = "PaymentMethod must have an ID".resolvableString,
                    ),
                cvc = (paymentMethodOptionsParams as? PaymentMethodOptionsParams.Card)?.cvc,
            ),
            options = ApiRequest.Options(
                apiKey = ephemeralKeySecret ?: publishableKeyProvider(),
                stripeAccount = stripeAccountIdProvider(),
            )
        ).fold(
            onSuccess = { confirmationToken ->
                handleDeferredOnConfirmationTokenCreated(
                    callback = createIntentWithConfirmationTokenCallback,
                    confirmationToken = confirmationToken,
                    intentConfiguration = intentConfiguration,
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                    shippingValues = shippingValues,
                    hCaptchaToken = hCaptchaToken,
                )
            },
            onFailure = { error ->
                NextStep.Fail(
                    cause = error,
                    message = error.stripeErrorMessage(),
                )
            }
        )
    }

    private suspend fun handleDeferredOnConfirmationTokenCreated(
        callback: CreateIntentWithConfirmationTokenCallback,
        confirmationToken: ConfirmationToken,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        hCaptchaToken: String?
    ): NextStep {
        val result = callback.onCreateIntent(confirmationToken)

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
                        hCaptchaToken = hCaptchaToken
                    )
                }
            }

            is CreateIntentResult.Failure -> {
                val exception = CreateIntentCallbackFailureException(result.cause)
                NextStep.Fail(
                    cause = exception,
                    message = result.displayMessage?.resolvableString
                        ?: exception.stripeErrorMessage(),
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
    ): NextStep {
        return retrieveStripeIntent(clientSecret).mapCatching { intent ->
            if (intent.isConfirmed) {
                failIfSetAsDefaultFeatureIsEnabled(paymentMethodExtraParams)
                NextStep.Complete(isForceSuccess = false)
            } else if (intent.requiresAction()) {
                createHandleNextActionStep(clientSecret, intent, paymentMethod)
            } else {
                DeferredIntentValidator.validate(intent, intentConfiguration, allowsManualConfirmation, paymentMethod)
                createConfirmStep(
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
            NextStep.Fail(
                cause = error,
                message = error.stripeErrorMessage(),
            )
        }
    }

    private fun createHandleNextActionStep(
        clientSecret: String,
        intent: StripeIntent,
        paymentMethod: PaymentMethod
    ): NextStep {
        return runCatching {
            DeferredIntentValidator.validatePaymentMethod(intent, paymentMethod)
            NextStep.HandleNextAction(clientSecret)
        }.getOrElse {
            NextStep.Fail(
                cause = InvalidDeferredIntentUsageException(),
                message = resolvableString(R.string.stripe_paymentsheet_invalid_deferred_intent_usage),
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
        intentConfigSetupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
        hCaptchaToken: String?
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
            intentConfigSetupFutureUsage = intentConfigSetupFutureUsage,
            radarOptions = hCaptchaToken?.let { RadarOptions(it) }
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

    private fun ConfirmPaymentIntentParams.Shipping.toAddressDetails(): AddressDetails {
        return AddressDetails(
            name = getName(),
            phoneNumber = getPhone(),
            address = getAddress().run {
                PaymentSheet.Address(
                    line1 = line1,
                    line2 = line2,
                    city = city,
                    country = country,
                    postalCode = postalCode,
                    state = state,
                )
            }
        )
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

    private companion object {
        private const val PROVIDER_FETCH_TIMEOUT = 2
        private const val PROVIDER_FETCH_INTERVAL = 5L
    }
}
