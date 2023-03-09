package com.stripe.android

import android.content.Context
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
        data class Fail(val errorMessage: String) : NextStep

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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        var createIntentCallback: AbsCreateIntentCallback? = null
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultIntentConfirmationInterceptor @Inject constructor(
    private val context: Context,
    private val stripeRepository: StripeRepository,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
) : IntentConfirmationInterceptor {

    private val userError: String
        get() = context.getString(R.string.unable_to_complete_operation)

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
            createPaymentMethod(paymentMethodCreateParams).fold(
                onSuccess = { paymentMethod ->
                    intercept(
                        clientSecret = null,
                        paymentMethod = paymentMethod,
                        shippingValues = shippingValues,
                        setupForFutureUsage = setupForFutureUsage,
                    )
                },
                onFailure = {
                    IntentConfirmationInterceptor.NextStep.Fail(userError)
                }
            )
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
            when (val callback = IntentConfirmationInterceptor.createIntentCallback) {
                is CreateIntentCallbackForServerSideConfirmation -> {
                    handleServerSideConfirmation(
                        createIntentCallback = callback,
                        shouldSavePaymentMethod = setupForFutureUsage == OffSession,
                        paymentMethod = paymentMethod,
                        shippingValues = shippingValues
                    )
                }
                is CreateIntentCallback -> {
                    handleClientSideConfirmation(
                        createIntentCallback = callback,
                        paymentMethod = paymentMethod,
                        shippingValues = shippingValues
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
    }

    private suspend fun createPaymentMethod(
        params: PaymentMethodCreateParams,
    ): Result<PaymentMethod> {
        return runCatching {
            requireNotNull(
                stripeRepository.createPaymentMethod(
                    paymentMethodCreateParams = params,
                    options = requestOptions,
                )
            )
        }
    }

    private suspend fun handleClientSideConfirmation(
        createIntentCallback: CreateIntentCallback,
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): IntentConfirmationInterceptor.NextStep {
        val result = createIntentCallback.onCreateIntent(paymentMethodId = paymentMethod.id!!)

        return when (result) {
            is CreateIntentCallback.Result.Success -> {
                createConfirmStep(result.clientSecret, shippingValues, paymentMethod)
            }
            is CreateIntentCallback.Result.Failure -> {
                IntentConfirmationInterceptor.NextStep.Fail(result.errorMessage)
            }
        }
    }

    private suspend fun handleServerSideConfirmation(
        createIntentCallback: CreateIntentCallbackForServerSideConfirmation,
        paymentMethod: PaymentMethod,
        shouldSavePaymentMethod: Boolean,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): IntentConfirmationInterceptor.NextStep {
        val result = createIntentCallback.onCreateIntent(
            paymentMethodId = paymentMethod.id!!,
            shouldSavePaymentMethod = shouldSavePaymentMethod
        )

        return when (result) {
            is CreateIntentCallback.Result.Success -> {
                val intentResult = retrieveStripeIntent(result.clientSecret)
                val intent = intentResult.getOrNull()

                if (intent == null) {
                    IntentConfirmationInterceptor.NextStep.Fail(userError)
                } else if (intent.isConfirmed) {
                    IntentConfirmationInterceptor.NextStep.Complete
                } else if (intent.status == StripeIntent.Status.RequiresAction) {
                    IntentConfirmationInterceptor.NextStep.HandleNextAction(result.clientSecret)
                } else {
                    createConfirmStep(result.clientSecret, shippingValues, paymentMethod)
                }
            }
            is CreateIntentCallback.Result.Failure -> {
                IntentConfirmationInterceptor.NextStep.Fail(result.errorMessage)
            }
        }
    }

    private suspend fun retrieveStripeIntent(clientSecret: String): Result<StripeIntent> {
        return runCatching {
            requireNotNull(
                stripeRepository.retrieveStripeIntent(
                    clientSecret = clientSecret,
                    options = requestOptions,
                )
            )
        }
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
