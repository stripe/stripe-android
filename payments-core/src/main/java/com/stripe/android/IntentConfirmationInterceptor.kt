package com.stripe.android

import androidx.annotation.RestrictTo
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
import javax.inject.Inject
import javax.inject.Named

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface IntentConfirmationInterceptor {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface NextStep {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Fail(val error: Throwable) : NextStep

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Confirm(val confirmParams: ConfirmStripeIntentParams) : NextStep

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class HandleNextAction(val clientSecret: String) : NextStep

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        object Complete : NextStep
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
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultIntentConfirmationInterceptor @Inject constructor(
    private val stripeRepository: StripeRepository,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
    private val confirmCallback: ConfirmCallback?,
) : IntentConfirmationInterceptor {

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
    ): IntentConfirmationInterceptor.NextStep {
        return if (clientSecret != null) {
            createConfirmStep(
                clientSecret = clientSecret,
                shippingValues = shippingValues,
                paymentMethodCreateParams = paymentMethodCreateParams,
                setupForFutureUsage = setupForFutureUsage,
            )
        } else {
            val paymentMethod = createPaymentMethod(paymentMethodCreateParams)

            if (paymentMethod != null) {
                intercept(
                    clientSecret = null,
                    paymentMethod = paymentMethod,
                    shippingValues = shippingValues,
                    setupForFutureUsage = setupForFutureUsage,
                )
            } else {
                IntentConfirmationInterceptor.NextStep.Fail(
                    error = IllegalStateException(
                        "Failed to create payment method during confirmation"
                    ),
                )
            }
        }
    }

    override suspend fun intercept(
        clientSecret: String?,
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
    ): IntentConfirmationInterceptor.NextStep {
        return if (clientSecret != null) {
            createConfirmStep(clientSecret, shippingValues, paymentMethod)
        } else {
            when (confirmCallback) {
                is ConfirmCallbackForClientSideConfirmation -> {
                    handleClientSideConfirmation(
                        confirmCallback = confirmCallback,
                        paymentMethod = paymentMethod,
                        shippingValues = shippingValues
                    )
                }
                is ConfirmCallbackForServerSideConfirmation -> {
                    handleServerSideConfirmation(
                        confirmCallback = confirmCallback,
                        shouldSavePaymentMethod = setupForFutureUsage == OffSession,
                        paymentMethod = paymentMethod,
                        shippingValues = shippingValues
                    )
                }
                else -> {
                    IntentConfirmationInterceptor.NextStep.Fail(
                        error = IllegalStateException(
                            "ConfirmCallback must be implemented when using " +
                                "IntentConfiguration to configure PaymentSheet",
                        ),
                    )
                }
            }
        }
    }

    private suspend fun createPaymentMethod(params: PaymentMethodCreateParams): PaymentMethod? {
        return runCatching {
            stripeRepository.createPaymentMethod(
                paymentMethodCreateParams = params,
                options = requestOptions,
            )
        }.getOrNull()
    }

    private suspend fun handleClientSideConfirmation(
        confirmCallback: ConfirmCallbackForClientSideConfirmation,
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): IntentConfirmationInterceptor.NextStep {
        val result = confirmCallback.onConfirmResponse(paymentMethodId = paymentMethod.id!!)

        return when (result) {
            is ConfirmCallback.Result.Success -> {
                createConfirmStep(result.clientSecret, shippingValues, paymentMethod)
            }
            is ConfirmCallback.Result.Failure -> {
                IntentConfirmationInterceptor.NextStep.Fail(result.error)
            }
        }
    }

    private suspend fun handleServerSideConfirmation(
        confirmCallback: ConfirmCallbackForServerSideConfirmation,
        paymentMethod: PaymentMethod,
        shouldSavePaymentMethod: Boolean,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): IntentConfirmationInterceptor.NextStep {
        val result = confirmCallback.onConfirmResponse(
            paymentMethodId = paymentMethod.id!!,
            shouldSavePaymentMethod = shouldSavePaymentMethod
        )

        return when (result) {
            is ConfirmCallback.Result.Success -> {
                val intent = retrieveStripeIntent(result.clientSecret)

                if (intent?.isConfirmed == true) {
                    IntentConfirmationInterceptor.NextStep.Complete
                } else if (intent?.status == StripeIntent.Status.RequiresAction) {
                    IntentConfirmationInterceptor.NextStep.HandleNextAction(result.clientSecret)
                } else {
                    createConfirmStep(result.clientSecret, shippingValues, paymentMethod)
                }
            }
            is ConfirmCallback.Result.Failure -> {
                IntentConfirmationInterceptor.NextStep.Fail(result.error)
            }
        }
    }

    private suspend fun retrieveStripeIntent(clientSecret: String): StripeIntent? {
        return runCatching {
            stripeRepository.retrieveStripeIntent(
                clientSecret = clientSecret,
                options = requestOptions,
            )
        }.getOrNull()
    }

    private fun createConfirmStep(
        clientSecret: String,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        paymentMethod: PaymentMethod,
    ): IntentConfirmationInterceptor.NextStep.Confirm {
        val factory = ConfirmStripeIntentParamsFactory.createFactory(
            clientSecret = clientSecret,
            shipping = shippingValues,
        )

        val confirmParams = factory.create(paymentMethod)
        return IntentConfirmationInterceptor.NextStep.Confirm(confirmParams)
    }

    private fun createConfirmStep(
        clientSecret: String,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
    ): IntentConfirmationInterceptor.NextStep.Confirm {
        val paramsFactory = ConfirmStripeIntentParamsFactory.createFactory(
            clientSecret = clientSecret,
            shipping = shippingValues,
        )
        val confirmParams = paramsFactory.create(paymentMethodCreateParams, setupForFutureUsage)
        return IntentConfirmationInterceptor.NextStep.Confirm(confirmParams)
    }
}
