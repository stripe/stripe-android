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
import javax.inject.Inject
import javax.inject.Named

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface IntentConfirmationInterceptor {

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
        clientSecret: String?,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
    ): NextStep

    suspend fun intercept(
        clientSecret: String?,
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultIntentConfirmationInterceptor @Inject constructor(
    private val context: Context,
    private val stripeRepository: StripeRepository,
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
        clientSecret: String?,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
    ): NextStep {
        return if (clientSecret != null) {
            createConfirmStep(
                clientSecret = clientSecret,
                shippingValues = shippingValues,
                paymentMethodCreateParams = paymentMethodCreateParams,
                setupForFutureUsage = setupForFutureUsage,
            )
        } else {
            handleDeferredIntent(
                shippingValues = shippingValues,
                paymentMethodCreateParams = paymentMethodCreateParams,
                setupForFutureUsage = setupForFutureUsage,
            )
        }
    }

    private suspend fun handleDeferredIntent(
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

    override suspend fun intercept(
        clientSecret: String?,
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
    ): NextStep {
        return if (clientSecret != null) {
            createConfirmStep(clientSecret, shippingValues, paymentMethod)
        } else {
            handleDeferredIntent(paymentMethod, shippingValues, setupForFutureUsage)
        }
    }

    private suspend fun handleDeferredIntent(
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
    ): NextStep {
        return when (val callback = IntentConfirmationInterceptor.createIntentCallback) {
            is CreateIntentCallback -> {
                handleIntentCreationFromPaymentMethod(
                    createIntentCallback = callback,
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
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): NextStep {
        return retrieveStripeIntent(clientSecret).fold(
            onSuccess = { intent ->
                if (intent.isConfirmed) {
                    NextStep.Complete(isForceSuccess = false)
                } else if (intent.requiresAction()) {
                    NextStep.HandleNextAction(clientSecret)
                } else {
                    createConfirmStep(clientSecret, shippingValues, paymentMethod)
                }
            },
            onFailure = { error ->
                NextStep.Fail(
                    cause = error,
                    message = genericErrorMessage,
                )
            }
        )
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
