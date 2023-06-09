package com.stripe.android.paymentsheet

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.ConfirmStripeIntentParamsFactory
import com.stripe.android.R
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.IntentConfirmationInterceptor.NextStep
import com.stripe.android.paymentsheet.injection.IS_FLOW_CONTROLLER
import javax.inject.Inject
import javax.inject.Named

internal interface IntentConfirmationInterceptor {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface NextStep {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Fail(
            val cause: Throwable,
            val message: String,
        ) : NextStep

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Confirm(val confirmParams: ConfirmStripeIntentParams) : NextStep

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class HandleNextAction(val clientSecret: String) : NextStep

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Complete(val isForceSuccess: Boolean) : NextStep
    }

    suspend fun intercept(
        initializationMode: PaymentSheet.InitializationMode,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
    ): NextStep

    suspend fun intercept(
        initializationMode: PaymentSheet.InitializationMode,
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
    ): NextStep

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        var createIntentCallback: AbsCreateIntentCallback? = null

        const val COMPLETE_WITHOUT_CONFIRMING_INTENT = "COMPLETE_WITHOUT_CONFIRMING_INTENT"
    }
}

@OptIn(ExperimentalPaymentSheetDecouplingApi::class)
internal class DefaultIntentConfirmationInterceptor @Inject constructor(
    private val context: Context,
    private val stripeRepository: StripeRepository,
    @Named(IS_FLOW_CONTROLLER) private val isFlowController: Boolean,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
) : IntentConfirmationInterceptor {

    private val genericErrorMessage: String
        get() = context.getString(R.string.stripe_unable_to_complete_operation)

    private val requestOptions: ApiRequest.Options
        get() = ApiRequest.Options(
            apiKey = publishableKeyProvider(),
            stripeAccount = stripeAccountIdProvider(),
        )

    override suspend fun intercept(
        initializationMode: PaymentSheet.InitializationMode,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
    ): NextStep {
        return when (initializationMode) {
            is PaymentSheet.InitializationMode.DeferredIntent -> {
                handleDeferredIntent(
                    intentConfiguration = initializationMode.intentConfiguration,
                    shippingValues = shippingValues,
                    paymentMethodCreateParams = paymentMethodCreateParams,
                    setupForFutureUsage = setupForFutureUsage,
                )
            }
            is PaymentSheet.InitializationMode.PaymentIntent -> {
                createConfirmStep(
                    clientSecret = initializationMode.clientSecret,
                    shippingValues = shippingValues,
                    paymentMethodCreateParams = paymentMethodCreateParams,
                    setupForFutureUsage = setupForFutureUsage,
                )
            }

            is PaymentSheet.InitializationMode.SetupIntent -> {
                createConfirmStep(
                    clientSecret = initializationMode.clientSecret,
                    shippingValues = shippingValues,
                    paymentMethodCreateParams = paymentMethodCreateParams,
                    setupForFutureUsage = setupForFutureUsage,
                )
            }
        }
    }

    override suspend fun intercept(
        initializationMode: PaymentSheet.InitializationMode,
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
    ): NextStep {
        return when (initializationMode) {
            is PaymentSheet.InitializationMode.DeferredIntent -> {
                handleDeferredIntent(
                    intentConfiguration = initializationMode.intentConfiguration,
                    paymentMethod = paymentMethod,
                    shippingValues = shippingValues,
                    setupForFutureUsage = setupForFutureUsage,
                )
            }
            is PaymentSheet.InitializationMode.PaymentIntent -> {
                createConfirmStep(
                    clientSecret = initializationMode.clientSecret,
                    shippingValues = shippingValues,
                    paymentMethod = paymentMethod,
                )
            }
            is PaymentSheet.InitializationMode.SetupIntent -> {
                createConfirmStep(
                    clientSecret = initializationMode.clientSecret,
                    shippingValues = shippingValues,
                    paymentMethod = paymentMethod,
                )
            }
        }
    }

    private suspend fun handleDeferredIntent(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
    ): NextStep {
        val params = paymentMethodCreateParams.copy(
            productUsage = paymentMethodCreateParams.attribution + "deferred-intent",
        )

        return createPaymentMethod(params).fold(
            onSuccess = { paymentMethod ->
                handleDeferredIntent(
                    intentConfiguration = intentConfiguration,
                    paymentMethod = paymentMethod,
                    shippingValues = shippingValues,
                    setupForFutureUsage = setupForFutureUsage,
                )
            },
            onFailure = { error ->
                NextStep.Fail(
                    cause = error,
                    message = genericErrorMessage,
                )
            }
        )
    }

    private suspend fun handleDeferredIntent(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
    ): NextStep {
        return when (val callback = IntentConfirmationInterceptor.createIntentCallback) {
            is CreateIntentCallback -> {
                handleIntentCreationFromPaymentMethod(
                    createIntentCallback = callback,
                    intentConfiguration = intentConfiguration,
                    paymentMethod = paymentMethod,
                    shouldSavePaymentMethod = setupForFutureUsage == OffSession,
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
        return runCatching {
            stripeRepository.createPaymentMethod(
                paymentMethodCreateParams = params,
                options = requestOptions,
            ) ?: throw APIException(message = "Couldn't parse response when creating payment method")
        }
    }

    private suspend fun handleIntentCreationFromPaymentMethod(
        createIntentCallback: CreateIntentCallback,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethod: PaymentMethod,
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
                    handleIntentCreationSuccess(
                        clientSecret = result.clientSecret,
                        intentConfiguration = intentConfiguration,
                        paymentMethod = paymentMethod,
                        shippingValues = shippingValues,
                    )
                }
            }
            is CreateIntentResult.Failure -> {
                NextStep.Fail(
                    cause = result.cause,
                    message = result.displayMessage ?: genericErrorMessage,
                )
            }
        }
    }

    private suspend fun handleIntentCreationSuccess(
        clientSecret: String,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): NextStep {
        return retrieveStripeIntent(clientSecret).mapCatching { intent ->
            if (intent.isConfirmed) {
                NextStep.Complete(isForceSuccess = false)
            } else if (intent.requiresAction()) {
                NextStep.HandleNextAction(clientSecret)
            } else {
                DeferredIntentValidator.validate(intent, intentConfiguration, isFlowController)
                createConfirmStep(clientSecret, shippingValues, paymentMethod)
            }
        }.getOrElse { error ->
            NextStep.Fail(
                cause = error,
                message = genericErrorMessage,
            )
        }
    }

    private suspend fun retrieveStripeIntent(clientSecret: String): Result<StripeIntent> {
        return runCatching {
            stripeRepository.retrieveStripeIntent(
                clientSecret = clientSecret,
                options = requestOptions,
            )
        }
    }

    private fun createConfirmStep(
        clientSecret: String,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        paymentMethod: PaymentMethod,
    ): NextStep.Confirm {
        val factory = ConfirmStripeIntentParamsFactory.createFactory(
            clientSecret = clientSecret,
            shipping = shippingValues,
        )

        val confirmParams = factory.create(paymentMethod)
        return NextStep.Confirm(confirmParams)
    }

    private fun createConfirmStep(
        clientSecret: String,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
    ): NextStep.Confirm {
        val paramsFactory = ConfirmStripeIntentParamsFactory.createFactory(
            clientSecret = clientSecret,
            shipping = shippingValues,
        )
        val confirmParams = paramsFactory.create(paymentMethodCreateParams, setupForFutureUsage)
        return NextStep.Confirm(confirmParams)
    }
}
