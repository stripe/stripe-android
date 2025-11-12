package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.ErrorReporter.ExpectedErrorEvent
import com.stripe.android.payments.core.analytics.ErrorReporter.SuccessEvent
import com.stripe.android.paymentsheet.CreateIntentCallback
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
    // Provider is required to defer ApiRequest.Options creation until after PaymentConfiguration is initialized.
    // Without it, Dagger would eagerly create ApiRequest.Options during graph construction, causing a crash
    // if PaymentConfiguration.init() hasn't been called yet.
    private val requestOptionsProvider: Provider<ApiRequest.Options>,
) {
    suspend fun waitForConfirmationTokenCallback(): CreateIntentWithConfirmationTokenCallback {
        return waitForDeferredIntentCallback(
            neededWaitEvent = SuccessEvent.FOUND_CREATE_INTENT_WITH_CONFIRMATION_TOKEN_CALLBACK_WHILE_POLLING,
            notFoundEvent = ExpectedErrorEvent.CREATE_INTENT_CALLBACK_NULL,
        ) {
            intentCreateIntentWithConfirmationTokenCallback.get()
        }
    }

    suspend fun waitForSharedPaymentTokenCallback(): PreparePaymentMethodHandler {
        return waitForDeferredIntentCallback(
            neededWaitEvent = SuccessEvent.FOUND_PREPARE_PAYMENT_METHOD_HANDLER_WHILE_POLLING,
            notFoundEvent = ExpectedErrorEvent.PREPARE_PAYMENT_METHOD_HANDLER_NULL,
        ) {
            preparePaymentMethodHandlerProvider.get()
        }
    }

    suspend fun waitForPaymentMethodCallback(): CreateIntentCallback {
        return waitForDeferredIntentCallback(
            neededWaitEvent = SuccessEvent.FOUND_CREATE_INTENT_CALLBACK_WHILE_POLLING,
            notFoundEvent = ExpectedErrorEvent.CREATE_INTENT_CALLBACK_NULL,
        ) {
            intentCreationCallbackProvider.get()
        }
    }

    private suspend inline fun <reified T : Any> waitForDeferredIntentCallback(
        neededWaitEvent: SuccessEvent,
        notFoundEvent: ExpectedErrorEvent,
        crossinline fetcher: () -> T?
    ): T {
        return fetcher() ?: withTimeoutOrNull(PROVIDER_FETCH_TIMEOUT.seconds) {
            while (true) {
                delay(PROVIDER_FETCH_INTERVAL)
                fetcher()?.let {
                    errorReporter.report(neededWaitEvent)
                    return@withTimeoutOrNull it
                }
            }
            null
        } ?: run {
            errorReporter.report(notFoundEvent)
            val errorMessage = "${T::class.java.simpleName} must be implemented when using IntentConfiguration!"
            throw DeferredIntentCallbackNotFoundException(
                message = errorMessage,
                resolvableError = if (requestOptionsProvider.get().apiKeyIsLiveMode) {
                    PaymentsCoreR.string.stripe_internal_error.resolvableString
                } else {
                    errorMessage.resolvableString
                }
            )
        }
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
