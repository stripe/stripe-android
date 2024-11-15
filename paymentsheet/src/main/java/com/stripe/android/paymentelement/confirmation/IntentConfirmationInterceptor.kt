package com.stripe.android.paymentelement.confirmation

import com.stripe.android.ConfirmStripeIntentParamsFactory
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.setupFutureUsage
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.confirmation.IntentConfirmationInterceptor.NextStep
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.DeferredIntentValidator
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import javax.inject.Inject
import javax.inject.Named

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
        paymentMethodCreateParams: PaymentMethodCreateParams,
        paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        customerRequestedSave: Boolean,
    ): NextStep

    suspend fun intercept(
        initializationMode: PaymentElementLoader.InitializationMode,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
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
        It appears you are reusing an intent on every `createIntentCallback` call. You should either create a brand
        new intent in `createIntentCallback` or update the existing intent with the new payment method ID.
    """.trimIndent()
}

internal class DefaultIntentConfirmationInterceptor @Inject constructor(
    private val stripeRepository: StripeRepository,
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
        paymentMethodCreateParams: PaymentMethodCreateParams,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        customerRequestedSave: Boolean,
    ): NextStep {
        return when (initializationMode) {
            is PaymentElementLoader.InitializationMode.DeferredIntent -> {
                handleDeferredIntent(
                    intentConfiguration = initializationMode.intentConfiguration,
                    shippingValues = shippingValues,
                    paymentMethodCreateParams = paymentMethodCreateParams,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    customerRequestedSave = customerRequestedSave,
                )
            }

            is PaymentElementLoader.InitializationMode.PaymentIntent -> {
                createConfirmStep(
                    clientSecret = initializationMode.clientSecret,
                    shippingValues = shippingValues,
                    paymentMethodCreateParams = paymentMethodCreateParams,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                )
            }

            is PaymentElementLoader.InitializationMode.SetupIntent -> {
                createConfirmStep(
                    clientSecret = initializationMode.clientSecret,
                    shippingValues = shippingValues,
                    paymentMethodCreateParams = paymentMethodCreateParams,
                )
            }
        }
    }

    override suspend fun intercept(
        initializationMode: PaymentElementLoader.InitializationMode,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): NextStep {
        return when (initializationMode) {
            is PaymentElementLoader.InitializationMode.DeferredIntent -> {
                val offSession = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                handleDeferredIntent(
                    intentConfiguration = initializationMode.intentConfiguration,
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    shippingValues = shippingValues,
                    shouldSavePaymentMethod = paymentMethodOptionsParams?.setupFutureUsage() == offSession,
                )
            }

            is PaymentElementLoader.InitializationMode.PaymentIntent -> {
                createConfirmStep(
                    clientSecret = initializationMode.clientSecret,
                    shippingValues = shippingValues,
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    isDeferred = false,
                )
            }

            is PaymentElementLoader.InitializationMode.SetupIntent -> {
                createConfirmStep(
                    clientSecret = initializationMode.clientSecret,
                    shippingValues = shippingValues,
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    isDeferred = false,
                )
            }
        }
    }

    private suspend fun handleDeferredIntent(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
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
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        shouldSavePaymentMethod: Boolean,
    ): NextStep {
        return when (val callback = IntentConfirmationInterceptor.createIntentCallback) {
            is CreateIntentCallback -> {
                handleDeferredIntentCreationFromPaymentMethod(
                    createIntentCallback = callback,
                    intentConfiguration = intentConfiguration,
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    shouldSavePaymentMethod = shouldSavePaymentMethod,
                    shippingValues = shippingValues,
                )
            }

            else -> {
                error(
                    "${CreateIntentCallback::class.java.simpleName} must be implemented " +
                        "when using IntentConfiguration with PaymentSheet"
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

    private suspend fun handleDeferredIntentCreationFromPaymentMethod(
        createIntentCallback: CreateIntentCallback,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
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
                        shippingValues = shippingValues,
                    )
                }
            }

            is CreateIntentResult.Failure -> {
                NextStep.Fail(
                    cause = result.cause,
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
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): NextStep {
        return retrieveStripeIntent(clientSecret).mapCatching { intent ->
            if (intent.isConfirmed) {
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
                    shippingValues,
                    paymentMethod,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
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
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        isDeferred: Boolean,
    ): NextStep.Confirm {
        val factory = ConfirmStripeIntentParamsFactory.createFactory(
            clientSecret = clientSecret,
            shipping = shippingValues,
        )

        val confirmParams = factory.create(
            paymentMethod = paymentMethod,
            optionsParams = paymentMethodOptionsParams,
        )
        return NextStep.Confirm(
            confirmParams = confirmParams,
            isDeferred = isDeferred,
        )
    }

    private fun createConfirmStep(
        clientSecret: String,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
    ): NextStep.Confirm {
        val paramsFactory = ConfirmStripeIntentParamsFactory.createFactory(
            clientSecret = clientSecret,
            shipping = shippingValues,
        )

        val confirmParams = paramsFactory.create(
            paymentMethodCreateParams,
            paymentMethodOptionsParams,
        )

        return NextStep.Confirm(
            confirmParams = confirmParams,
            isDeferred = false,
        )
    }

    private companion object {
        private val GENERIC_STRIPE_MESSAGE = R.string.stripe_something_went_wrong
    }
}
