package com.stripe.android.paymentelement.confirmation.interceptor

import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentCallback
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentCallbackNotFoundException
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentCallbackRetriever
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import javax.inject.Provider
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.stripe.android.R as PaymentsCoreR

@OptIn(SharedPaymentTokenSessionPreview::class)
class DeferredIntentCallbackRetrieverTest {

    @Test
    fun `retrieve with sharedPaymentToken fails if PreparePaymentMethodHandler is not set`() = testNoProvider(
        event = ErrorReporter.ExpectedErrorEvent.PREPARE_PAYMENT_METHOD_HANDLER_NULL,
        failureMessage = PREPARE_PAYMENT_METHOD_HANDLER_MESSAGE,
        userMessage = PREPARE_PAYMENT_METHOD_HANDLER_MESSAGE.resolvableString,
    ) { errorReporter ->
        createDefaultRetriever(errorReporter = errorReporter).waitForDeferredIntentCallback(
            PaymentSheet.IntentConfiguration.IntentBehavior.SharedPaymentToken(null)
        )
    }

    @Test
    fun `retrieve with DeferredIntent fails if CreateIntentCallback is not set`() = testNoProvider(
        event = ErrorReporter.ExpectedErrorEvent.CREATE_INTENT_CALLBACK_NULL,
        failureMessage = CREATE_INTENT_CALLBACK_MESSAGE,
        userMessage = CREATE_INTENT_CALLBACK_MESSAGE.resolvableString,
    ) { errorReporter ->
        createDefaultRetriever(errorReporter = errorReporter).waitForDeferredIntentCallback(
            PaymentSheet.IntentConfiguration.IntentBehavior.Default
        )
    }

    @Test
    fun `Message for live key when error without confirm callback is user friendly`() = testNoProvider(
        event = ErrorReporter.ExpectedErrorEvent.CREATE_INTENT_CALLBACK_NULL,
        failureMessage = CREATE_INTENT_CALLBACK_MESSAGE,
        userMessage = PaymentsCoreR.string.stripe_internal_error.resolvableString,
    ) { errorReporter ->
        createDefaultRetriever(
            errorReporter = errorReporter,
            publishableKeyProvider = { "pk_live_12345" },
        ).waitForDeferredIntentCallback(
            PaymentSheet.IntentConfiguration.IntentBehavior.Default
        )
    }

    @Test
    fun `Succeeds if callback is found before timeout time`() {
        val dispatcher = StandardTestDispatcher()
        var callback: CreateIntentCallback? = null

        runTest(dispatcher) {
            val errorReporter = FakeErrorReporter()
            val retrieveJob = async {
                createDefaultRetriever(
                    errorReporter = errorReporter,
                    intentCreationCallbackProvider = Provider { callback },
                ).waitForDeferredIntentCallback(
                    PaymentSheet.IntentConfiguration.IntentBehavior.Default
                )
            }
            dispatcher.scheduler.advanceTimeBy(1000)
            assertTrue(retrieveJob.isActive)

            callback = CreateIntentCallback { _, _ ->
                CreateIntentResult.Success(clientSecret = "pi_123")
            }

            dispatcher.scheduler.advanceTimeBy(1001)
            assertFalse(retrieveJob.isActive)
            assertTrue(retrieveJob.isCompleted)
            assertThat(errorReporter.getLoggedErrors()).containsExactly(
                ErrorReporter.SuccessEvent.FOUND_CREATE_INTENT_CALLBACK_WHILE_POLLING.eventName,
            )
        }
    }

    private fun testNoProvider(
        event: ErrorReporter.ErrorEvent,
        failureMessage: String,
        userMessage: ResolvableString,
        retrieveCall: suspend (errorReporter: FakeErrorReporter) -> DeferredIntentCallback
    ) {
        val errorReporter = FakeErrorReporter()
        val dispatcher = StandardTestDispatcher()

        runTest(dispatcher) {
            lateinit var exception: DeferredIntentCallbackNotFoundException
            val retrieveJob = async {
                exception = assertFailsWith<DeferredIntentCallbackNotFoundException> {
                    retrieveCall(errorReporter)
                }
            }

            assertTrue(retrieveJob.isActive)

            dispatcher.scheduler.advanceTimeBy(1000)

            assertTrue(retrieveJob.isActive)

            dispatcher.scheduler.advanceTimeBy(1000)

            assertTrue(retrieveJob.isActive)

            dispatcher.scheduler.advanceTimeBy(1)

            assertFalse(retrieveJob.isActive)

            assertThat(exception.message).isEqualTo(failureMessage)
            assertThat(exception.resolvableError).isEqualTo(userMessage)

            assertThat(errorReporter.getLoggedErrors()).contains(event.eventName)
        }
    }

    private fun createDefaultRetriever(
        publishableKeyProvider: Provider<String> = Provider { "pk_test_1234" },
        errorReporter: FakeErrorReporter = FakeErrorReporter(),
        intentCreationCallbackProvider: Provider<CreateIntentCallback?> = Provider { null },
        intentCreationWithConfirmationTokenCallback:
        Provider<CreateIntentWithConfirmationTokenCallback?> = Provider { null },
        preparePaymentMethodHandlerProvider: Provider<PreparePaymentMethodHandler?> = Provider { null },
    ): DeferredIntentCallbackRetriever {
        return DeferredIntentCallbackRetriever(
            intentCreationCallbackProvider = intentCreationCallbackProvider,
            intentCreateIntentWithConfirmationTokenCallback = intentCreationWithConfirmationTokenCallback,
            preparePaymentMethodHandlerProvider = preparePaymentMethodHandlerProvider,
            errorReporter = errorReporter,
            requestOptionsProvider = {
                ApiRequest.Options(
                    apiKey = publishableKeyProvider.get(),
                    stripeAccount = null,
                )
            },
        )
    }

    companion object {
        private const val CREATE_INTENT_CALLBACK_MESSAGE =
            "One of CreateIntentCallback or CreateIntentWithConfirmationTokenCallback " +
                "must be implemented when using IntentConfiguration with PaymentSheet"

        const val PREPARE_PAYMENT_METHOD_HANDLER_MESSAGE =
            "PreparePaymentMethodHandler must be implemented when using IntentConfiguration with " +
                "shared payment tokens!"
    }
}
