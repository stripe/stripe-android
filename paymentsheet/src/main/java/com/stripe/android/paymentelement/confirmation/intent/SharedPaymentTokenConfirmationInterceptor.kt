package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.exception.GenericStripeException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.utils.errorMessage
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.DefaultIntentConfirmationInterceptor.Companion.PROVIDER_FETCH_INTERVAL
import com.stripe.android.paymentelement.confirmation.intent.DefaultIntentConfirmationInterceptor.Companion.PROVIDER_FETCH_TIMEOUT
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import kotlin.time.Duration.Companion.seconds
import com.stripe.android.R as PaymentsCoreR

@OptIn(SharedPaymentTokenSessionPreview::class)
internal class SharedPaymentTokenConfirmationInterceptor @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val errorReporter: ErrorReporter,
    private val preparePaymentMethodHandlerProvider: Provider<PreparePaymentMethodHandler?>,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
) {

    private val requestOptions: ApiRequest.Options
        get() = ApiRequest.Options(
            apiKey = publishableKeyProvider(),
            stripeAccount = stripeAccountIdProvider(),
        )

    internal suspend fun handlePrepareNewPaymentMethod(
        intent: StripeIntent,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
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

        return stripeRepository.createPaymentMethod(params, requestOptions).fold(
            onSuccess = { paymentMethod ->
                handlePrepareNewPaymentMethod(
                    intent = intent,
                    paymentMethod = paymentMethod,
                    shippingValues = shippingValues,
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

    internal suspend fun handlePrepareNewPaymentMethod(
        intent: StripeIntent,
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmationDefinition.Action<Args> {
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

                    ConfirmationDefinition.Action.Complete(
                        intent = intent,
                        deferredIntentConfirmationType = DeferredIntentConfirmationType.None,
                        completedFullPaymentFlow = false,
                    )
                } catch (exception: Exception) {
                    ConfirmationDefinition.Action.Fail(
                        cause = exception,
                        message = exception.errorMessage,
                        errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                    )
                }
            }

            else -> {
                val error = "${PreparePaymentMethodHandler::class.java.simpleName} must be implemented " +
                    "when using IntentConfiguration with shared payment tokens!"

                errorReporter.report(ErrorReporter.ExpectedErrorEvent.PREPARE_PAYMENT_METHOD_HANDLER_NULL)

                ConfirmationDefinition.Action.Fail(
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
    }

    private fun retrievePreparePaymentMethodHandler(): PreparePaymentMethodHandler? {
        return preparePaymentMethodHandlerProvider.get()
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

}