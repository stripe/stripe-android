package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.ConfirmStripeIntentParamsFactory
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.RadarOptions
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import com.stripe.android.R as PaymentsCoreR

internal interface IntentConfirmationInterceptor {

    suspend fun intercept(
        initializationMode: PaymentElementLoader.InitializationMode,
        intent: StripeIntent,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
        paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        customerRequestedSave: Boolean,
    ): ConfirmationDefinition.Action<Args>

    suspend fun intercept(
        initializationMode: PaymentElementLoader.InitializationMode,
        intent: StripeIntent,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        hCaptchaToken: String?,
    ): ConfirmationDefinition.Action<Args>

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

@OptIn(SharedPaymentTokenSessionPreview::class)
internal class DefaultIntentConfirmationInterceptor @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val errorReporter: ErrorReporter,
    private val intentCreationCallbackProvider: Provider<CreateIntentCallback?>,
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
    ): ConfirmationDefinition.Action<Args> {
        return when (initializationMode) {
            is PaymentElementLoader.InitializationMode.DeferredIntent -> {
                /*
                 * We don't pass the created intent in `DeferredIntent` mode because we rely on the created intent
                 * from the merchant through `CreateIntentCallback`. The intent passed through here is created from
                 * `PaymentSheet.Configuration` in order to populate `Payment Element` data.
                 */
                if (initializationMode.intentConfiguration.intentBehavior is
                        PaymentSheet.IntentConfiguration.IntentBehavior.SharedPaymentToken
                ) {
                    SharedPaymentTokenConfirmationInterceptor(
                        stripeRepository = stripeRepository,
                        errorReporter = errorReporter,
                        preparePaymentMethodHandlerProvider = preparePaymentMethodHandlerProvider,
                        publishableKeyProvider = publishableKeyProvider,
                        stripeAccountIdProvider = stripeAccountIdProvider,
                    ).handlePrepareNewPaymentMethod(
                        intent = intent,
                        intentConfiguration = initializationMode.intentConfiguration,
                        shippingValues = shippingValues,
                        paymentMethodCreateParams = paymentMethodCreateParams,
                    )
                } else {
                    DeferredIntentConfirmationInterceptor(
                        stripeRepository = stripeRepository,
                        errorReporter = errorReporter,
                        intentCreationCallbackProvider = intentCreationCallbackProvider,
                        allowsManualConfirmation = allowsManualConfirmation,
                        publishableKeyProvider = publishableKeyProvider,
                        stripeAccountIdProvider = stripeAccountIdProvider,
                    ).handleNewPaymentMethod(
                        intentConfiguration = initializationMode.intentConfiguration,
                        intent = intent,
                        paymentMethodCreateParams = paymentMethodCreateParams,
                        paymentMethodOptionsParams = paymentMethodOptionsParams,
                        paymentMethodExtraParams = paymentMethodExtraParams,
                        shippingValues = shippingValues,
                        customerRequestedSave = customerRequestedSave,
                    )
                }
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
        hCaptchaToken: String?
    ): ConfirmationDefinition.Action<Args> {
        return when (initializationMode) {
            is PaymentElementLoader.InitializationMode.DeferredIntent -> {
                if (initializationMode.intentConfiguration.intentBehavior is
                        PaymentSheet.IntentConfiguration.IntentBehavior.SharedPaymentToken
                ) {
                    SharedPaymentTokenConfirmationInterceptor(
                        stripeRepository = stripeRepository,
                        errorReporter = errorReporter,
                        preparePaymentMethodHandlerProvider = preparePaymentMethodHandlerProvider,
                        publishableKeyProvider = publishableKeyProvider,
                        stripeAccountIdProvider = stripeAccountIdProvider,
                    ).handlePrepareNewPaymentMethod(
                        intent = intent,
                        paymentMethod = paymentMethod,
                        shippingValues = shippingValues,
                    )
                } else {
                    DeferredIntentConfirmationInterceptor(
                        stripeRepository = stripeRepository,
                        errorReporter = errorReporter,
                        intentCreationCallbackProvider = intentCreationCallbackProvider,
                        allowsManualConfirmation = allowsManualConfirmation,
                        publishableKeyProvider = publishableKeyProvider,
                        stripeAccountIdProvider = stripeAccountIdProvider,
                    ).handleSavedPaymentMethod(
                        intentConfiguration = initializationMode.intentConfiguration,
                        intent = intent,
                        paymentMethod = paymentMethod,
                        paymentMethodOptionsParams = paymentMethodOptionsParams,
                        paymentMethodExtraParams = paymentMethodExtraParams,
                        shippingValues = shippingValues,
                        hCaptchaToken = hCaptchaToken,
                    )
                }
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
    ): ConfirmationDefinition.Action<Args> {
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
        return ConfirmationDefinition.Action.Launch(
            launcherArguments = Args.Confirm(confirmParams),
            deferredIntentConfirmationType = DeferredIntentConfirmationType.Client.takeIf { isDeferred },
            receivesResultInProcess = false,
        )
    }
    private fun createConfirmStep(
        clientSecret: String,
        intent: StripeIntent,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
        paymentMethodExtraParams: PaymentMethodExtraParams? = null,
    ): ConfirmationDefinition.Action<Args> {
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

        return ConfirmationDefinition.Action.Launch(
            launcherArguments = Args.Confirm(confirmParams),
            deferredIntentConfirmationType = null,
            receivesResultInProcess = false,
        )
    }

    private fun createFailStep(
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
    internal companion object {
        const val PROVIDER_FETCH_TIMEOUT = 2
        const val PROVIDER_FETCH_INTERVAL = 5L
    }
}
