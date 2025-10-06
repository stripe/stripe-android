package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Provider
import kotlin.time.Duration.Companion.seconds
import com.stripe.android.R as PaymentsCoreR

@OptIn(SharedPaymentTokenSessionPreview::class)
internal class DeferredIntentCallbackRetriever @Inject constructor(
    private val intentCreationCallbackProvider: Provider<CreateIntentCallback?>,
    private val intentCreateIntentWithConfirmationTokenCallback: Provider<CreateIntentWithConfirmationTokenCallback?>,
    private val preparePaymentMethodHandlerProvider: Provider<PreparePaymentMethodHandler?>,
    private val errorReporter: ErrorReporter,
    private val requestOptions: ApiRequest.Options,
) {

    suspend fun waitForDeferredIntentCallback(
        behavior: PaymentSheet.IntentConfiguration.IntentBehavior
    ): DeferredIntentCallback {
        return retrieveDeferredIntentCallback(behavior) ?: run {
            withTimeoutOrNull(PROVIDER_FETCH_TIMEOUT.seconds) {
                var deferredIntentCallback: DeferredIntentCallback? = null

                while (deferredIntentCallback == null) {
                    delay(PROVIDER_FETCH_INTERVAL)
                    deferredIntentCallback = retrieveDeferredIntentCallback(behavior)
                }

                deferredIntentCallback.also {
                    when (it) {
                        is DeferredIntentCallback.PaymentMethod -> {
                            errorReporter.report(
                                ErrorReporter.SuccessEvent.FOUND_CREATE_INTENT_CALLBACK_WHILE_POLLING
                            )
                        }
                        is DeferredIntentCallback.ConfirmationToken -> {
                            errorReporter.report(
                                ErrorReporter.SuccessEvent
                                    .FOUND_CREATE_INTENT_WITH_CONFIRMATION_TOKEN_CALLBACK_WHILE_POLLING
                            )
                        }
                        is DeferredIntentCallback.SharedPaymentToken -> {
                            errorReporter.report(
                                ErrorReporter.SuccessEvent
                                    .FOUND_PREPARE_PAYMENT_METHOD_HANDLER_WHILE_POLLING
                            )
                        }
                    }
                }
            }
        } ?: run {
            val errorMessage: String
            when (behavior) {
                is PaymentSheet.IntentConfiguration.IntentBehavior.SharedPaymentToken -> {
                    errorMessage = "${PreparePaymentMethodHandler::class.java.simpleName} must be implemented " +
                        "when using IntentConfiguration with shared payment tokens!"

                    errorReporter.report(
                        ErrorReporter.ExpectedErrorEvent.PREPARE_PAYMENT_METHOD_HANDLER_NULL
                    )
                }
                is PaymentSheet.IntentConfiguration.IntentBehavior.Default -> {
                    errorMessage = "One of ${CreateIntentCallback::class.java.simpleName} or " +
                        "${CreateIntentWithConfirmationTokenCallback::class.java.simpleName} must be implemented " +
                        "when using IntentConfiguration with PaymentSheet"

                    errorReporter.report(ErrorReporter.ExpectedErrorEvent.CREATE_INTENT_CALLBACK_NULL)
                }
            }
            throw DeferredIntentCallbackNotFoundException(
                message = errorMessage,
                resolvableError = if (requestOptions.apiKeyIsLiveMode) {
                    PaymentsCoreR.string.stripe_internal_error.resolvableString
                } else {
                    errorMessage.resolvableString
                }
            )
        }
    }

    private fun retrieveDeferredIntentCallback(
        behavior: PaymentSheet.IntentConfiguration.IntentBehavior
    ): DeferredIntentCallback? {
        when (behavior) {
            is PaymentSheet.IntentConfiguration.IntentBehavior.SharedPaymentToken -> {
                preparePaymentMethodHandlerProvider.get()?.let {
                    return DeferredIntentCallback.SharedPaymentToken(it)
                }
            }
            is PaymentSheet.IntentConfiguration.IntentBehavior.Default -> {
                intentCreationCallbackProvider.get()?.let {
                    return DeferredIntentCallback.PaymentMethod(it)
                }
                intentCreateIntentWithConfirmationTokenCallback.get()?.let {
                    return DeferredIntentCallback.ConfirmationToken(it)
                }
            }
        }
        return null
    }

    private companion object {
        const val PROVIDER_FETCH_TIMEOUT = 2
        const val PROVIDER_FETCH_INTERVAL = 5L
    }
}

internal class DeferredIntentCallbackNotFoundException(
    message: String,
    val resolvableError: ResolvableString
) : StripeException(message = message) {
    override fun analyticsValue(): String = "deferredIntentCallbackNotFound"
}

@OptIn(SharedPaymentTokenSessionPreview::class)
internal sealed class DeferredIntentCallback {
    class SharedPaymentToken(val handler: PreparePaymentMethodHandler) : DeferredIntentCallback()
    class PaymentMethod(val callback: CreateIntentCallback) : DeferredIntentCallback()
    class ConfirmationToken(val callback: CreateIntentWithConfirmationTokenCallback) : DeferredIntentCallback()
}
