package com.stripe.android.interceptor

import com.stripe.android.ConfirmCallback
import com.stripe.android.ConfirmCallbackForClientSideConfirmation
import com.stripe.android.ConfirmCallbackForServerSideConfirmation
import com.stripe.android.ConfirmStripeIntentParamsFactory
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.utils.StripeIntentValidator
import javax.inject.Inject
import javax.inject.Named

interface IntentConfirmationInterceptor {
    sealed interface NextStep {
        data class Fail(val error: String) : NextStep
        data class Confirm(
            val confirmStripeIntentParams: ConfirmStripeIntentParams
        ) : NextStep
        object Complete : NextStep
    }

    suspend fun intercept(
        clientSecret: String?,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?
    ): NextStep

    suspend fun intercept(
        clientSecret: String?,
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?
    ): NextStep
}

class DefaultIntentConfirmationInterceptor @Inject constructor(
    private val stripeRepository: StripeRepository,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String,
    private val confirmCallback: ConfirmCallback?,
    private val stripeIntentValidator: StripeIntentValidator
) : IntentConfirmationInterceptor {
    override suspend fun intercept(
        clientSecret: String?,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?
    ): IntentConfirmationInterceptor.NextStep {
        return clientSecret?.let {
            val paramsFactory = ConfirmStripeIntentParamsFactory.createFactory(
                clientSecret = clientSecret,
                shipping = shippingValues
            )
            return IntentConfirmationInterceptor.NextStep.Confirm(
                paramsFactory.create(paymentMethodCreateParams, setupForFutureUsage)
            )
        } ?: run {
            val paymentMethod = stripeRepository.createPaymentMethod(
                paymentMethodCreateParams,
                ApiRequest.Options(
                    apiKey = publishableKeyProvider(),
                    stripeAccount = stripeAccountIdProvider()
                )
            )
                ?: return@run IntentConfirmationInterceptor.NextStep.Fail(
                    "Failed to create PaymentMethod"
                )
            intercept(
                clientSecret = null,
                paymentMethod = paymentMethod,
                shippingValues = shippingValues,
                setupForFutureUsage = setupForFutureUsage
            )
        }
    }

    private suspend fun retrieveStripeIntent(clientSecret: String): StripeIntent {
        return stripeRepository.retrieveStripeIntent(
            clientSecret = clientSecret,
            options = ApiRequest.Options(
                apiKey = publishableKeyProvider(),
                stripeAccount = stripeAccountIdProvider()
            )
        )
    }

    override suspend fun intercept(
        clientSecret: String?,
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?
    ): IntentConfirmationInterceptor.NextStep {
        return clientSecret?.let {
            val paramsFactory = ConfirmStripeIntentParamsFactory.createFactory(
                clientSecret = clientSecret,
                shipping = shippingValues
            )
            return IntentConfirmationInterceptor.NextStep.Confirm(
                paramsFactory.create(paymentMethod)
            )
        } ?: run {
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
                        // TODO(jameswoo) pass shouldSavePaymentMethod
                        shouldSavePaymentMethod = false,
                        paymentMethod = paymentMethod,
                        shippingValues = shippingValues
                    )
                }
                else -> {
                    error("ConfirmCallback must be implemented for DeferredIntent flow")
                }
            }
        }
    }

    private suspend fun handleClientSideConfirmation(
        confirmCallback: ConfirmCallbackForClientSideConfirmation,
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): IntentConfirmationInterceptor.NextStep {
        val result = confirmCallback.onConfirmResponse(
            paymentMethodId = paymentMethod.id!!
        )
        return when (result) {
            is ConfirmCallback.Result.Success -> {
                IntentConfirmationInterceptor.NextStep.Confirm(
                    ConfirmStripeIntentParamsFactory.createFactory(
                        clientSecret = result.clientSecret,
                        shipping = shippingValues
                    ).create(
                        paymentMethod
                    )
                )
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
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): IntentConfirmationInterceptor.NextStep {
        val result = confirmCallback.onConfirmResponse(
            paymentMethodId = paymentMethod.id!!,
            shouldSavePaymentMethod = shouldSavePaymentMethod
        )

        return when (result) {
            is ConfirmCallback.Result.Success -> {
                val intent = retrieveStripeIntent(result.clientSecret)
                // TODO(jameswoo) What do we do if the intent is in a failed state?
                if (stripeIntentValidator.isConfirmed(intent)) {
                    IntentConfirmationInterceptor.NextStep.Complete
                } else {
                    IntentConfirmationInterceptor.NextStep.Confirm(
                        ConfirmStripeIntentParamsFactory.createFactory(
                            clientSecret = result.clientSecret,
                            shipping = shippingValues
                        ).create(paymentMethod)
                    )
                }
            }
            is ConfirmCallback.Result.Failure -> {
                IntentConfirmationInterceptor.NextStep.Fail(result.error)
            }
        }
    }
}
