package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.exception.StripeException
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

internal abstract class CallbackRetriever(
    private val errorReporter: ErrorReporter,
) {
    protected suspend fun <T> waitForCallback(
        neededWaitEvent: SuccessEvent,
        notFoundEvent: ExpectedErrorEvent,
        notFoundMessage: String,
        analyticsValue: String,
        isLiveMode: Boolean,
        fetcher: () -> T?
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
            throw CallbackNotFoundException(
                message = notFoundMessage,
                analyticsValue = analyticsValue,
                resolvableError = if (isLiveMode) {
                    PaymentsCoreR.string.stripe_internal_error.resolvableString
                } else {
                    notFoundMessage.resolvableString
                }
            )
        }
    }

    private companion object {
        const val PROVIDER_FETCH_TIMEOUT = 2
        const val PROVIDER_FETCH_INTERVAL = 5L
    }
}

@OptIn(SharedPaymentTokenSessionPreview::class)
internal class DeferredIntentCallbackRetriever @Inject constructor(
    private val intentCreationCallbackProvider: Provider<CreateIntentCallback?>,
    private val intentCreateIntentWithConfirmationTokenCallback: Provider<CreateIntentWithConfirmationTokenCallback?>,
    private val preparePaymentMethodHandlerProvider: Provider<PreparePaymentMethodHandler?>,
    errorReporter: ErrorReporter,
) : CallbackRetriever(errorReporter) {
    suspend fun waitForConfirmationTokenCallback(isLiveMode: Boolean): CreateIntentWithConfirmationTokenCallback {
        return waitForCallback(
            neededWaitEvent = SuccessEvent.FOUND_CREATE_INTENT_WITH_CONFIRMATION_TOKEN_CALLBACK_WHILE_POLLING,
            notFoundEvent = ExpectedErrorEvent.CREATE_INTENT_CALLBACK_NULL,
            analyticsValue = ANALYTICS_VALUE,
            notFoundMessage = notFoundMessage<CreateIntentWithConfirmationTokenCallback>(),
            isLiveMode = isLiveMode,
        ) {
            intentCreateIntentWithConfirmationTokenCallback.get()
        }
    }

    suspend fun waitForSharedPaymentTokenCallback(isLiveMode: Boolean): PreparePaymentMethodHandler {
        return waitForCallback(
            neededWaitEvent = SuccessEvent.FOUND_PREPARE_PAYMENT_METHOD_HANDLER_WHILE_POLLING,
            notFoundEvent = ExpectedErrorEvent.PREPARE_PAYMENT_METHOD_HANDLER_NULL,
            analyticsValue = ANALYTICS_VALUE,
            notFoundMessage = notFoundMessage<PreparePaymentMethodHandler>(),
            isLiveMode = isLiveMode,
        ) {
            preparePaymentMethodHandlerProvider.get()
        }
    }

    suspend fun waitForPaymentMethodCallback(isLiveMode: Boolean): CreateIntentCallback {
        return waitForCallback(
            neededWaitEvent = SuccessEvent.FOUND_CREATE_INTENT_CALLBACK_WHILE_POLLING,
            notFoundEvent = ExpectedErrorEvent.CREATE_INTENT_CALLBACK_NULL,
            analyticsValue = ANALYTICS_VALUE,
            notFoundMessage = notFoundMessage<CreateIntentCallback>(),
            isLiveMode = isLiveMode,
        ) {
            intentCreationCallbackProvider.get()
        }
    }

    private inline fun <reified T : Any> notFoundMessage() =
        "${T::class.java.simpleName} must be implemented when using IntentConfiguration!"

    private companion object {
        const val ANALYTICS_VALUE = "deferredIntentCallbackNotFound"
    }
}

internal class CallbackNotFoundException(
    message: String,
    val resolvableError: ResolvableString,
    private val analyticsValue: String,
) : StripeException(message = message) {
    override fun analyticsValue(): String = analyticsValue
}
