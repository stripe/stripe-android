package com.stripe.android.common.taptoadd

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.stripe.android.R as PaymentsCoreR

@OptIn(TapToAddPreview::class)
@RunWith(RobolectricTestRunner::class)
class DefaultCreateCardPresentSetupIntentCallbackRetrieverTest {
    @Test
    fun `Succeeds if CreateCardPresentSetupIntentCallback is found immediately`() = runTest {
        val callback = CreateCardPresentSetupIntentCallback {
            CreateIntentResult.Success("si_123")
        }
        val errorReporter = FakeErrorReporter()
        val retriever = createRetriever(
            callbackProvider = { callback },
            errorReporter = errorReporter,
        )

        assertThat(retriever.waitForCallback()).isEqualTo(callback)
        errorReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `Succeeds if CreateCardPresentSetupIntentCallback is found before timeout time`() {
        val dispatcher = StandardTestDispatcher()
        val errorReporter = FakeErrorReporter()
        var callback: CreateCardPresentSetupIntentCallback? = null
        val retriever = createRetriever(
            callbackProvider = { callback },
            errorReporter = errorReporter,
        )

        runTest(dispatcher) {
            val retrieveJob = async {
                retriever.waitForCallback()
            }
            dispatcher.scheduler.advanceTimeBy(1000)
            assertTrue(retrieveJob.isActive)

            callback = CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success(clientSecret = "pi_123")
            }

            dispatcher.scheduler.advanceTimeBy(1001)
            assertFalse(retrieveJob.isActive)
            assertTrue(retrieveJob.isCompleted)
            assertThat(errorReporter.awaitCall().errorEvent)
                .isEqualTo(ErrorReporter.SuccessEvent.TAP_TO_ADD_FOUND_CREATE_INTENT_CALLBACK_WHILE_POLLING)
            errorReporter.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `waitForCallback throws exception when callback not found after timeout`() {
        val errorReporter = FakeErrorReporter()
        val dispatcher = StandardTestDispatcher()
        val retriever = createRetriever(
            callbackProvider = { null },
            errorReporter = errorReporter
        )

        runTest(dispatcher) {
            lateinit var exception: CreateCardPresentSetupIntentCallbackNotFoundException
            val retrieveJob = async {
                exception = assertFailsWith<CreateCardPresentSetupIntentCallbackNotFoundException> {
                    retriever.waitForCallback()
                }
            }

            assertTrue(retrieveJob.isActive)

            dispatcher.scheduler.advanceTimeBy(1000)

            assertTrue(retrieveJob.isActive)

            dispatcher.scheduler.advanceTimeBy(1000)

            assertTrue(retrieveJob.isActive)

            dispatcher.scheduler.advanceTimeBy(1)

            assertFalse(retrieveJob.isActive)

            assertThat(exception.message)
                .isEqualTo(
                    "CreateCardPresentSetupIntentCallback must be implemented when using Tap to Add!"
                )
            assertThat(exception.resolvableError)
                .isEqualTo(
                    "CreateCardPresentSetupIntentCallback must be implemented when using Tap to Add!"
                        .resolvableString
                )
            assertThat(errorReporter.awaitCall().errorEvent)
                .isEqualTo(ErrorReporter.ExpectedErrorEvent.CREATE_CARD_PRESENT_SETUP_INTENT_NULL)
            errorReporter.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `waitForCallback returns test error message when not in live mode`() = runTest {
        val retriever = createRetriever(
            callbackProvider = { null },
            isLiveMode = false
        )

        try {
            retriever.waitForCallback()
            error("Expected exception to be thrown")
        } catch (e: CreateCardPresentSetupIntentCallbackNotFoundException) {
            assertThat(e.resolvableError)
                .isEqualTo(
                    "CreateCardPresentSetupIntentCallback must be implemented when using Tap to Add!"
                        .resolvableString
                )
        }
    }

    @Test
    fun `waitForCallback returns generic error message when in live mode`() = runTest {
        val retriever = createRetriever(
            callbackProvider = { null },
            isLiveMode = true,
        )

        try {
            retriever.waitForCallback()
            error("Expected exception to be thrown")
        } catch (e: CreateCardPresentSetupIntentCallbackNotFoundException) {
            assertThat(e.resolvableError)
                .isEqualTo(PaymentsCoreR.string.stripe_internal_error.resolvableString)
        }
    }

    private fun createRetriever(
        callbackProvider: Provider<CreateCardPresentSetupIntentCallback?>,
        errorReporter: ErrorReporter = FakeErrorReporter(),
        isLiveMode: Boolean = false,
    ): DefaultCreateCardPresentSetupIntentCallbackRetriever {
        val apiKey = if (isLiveMode) LIVE_PUBLISHABLE_KEY else ApiKeyFixtures.FAKE_PUBLISHABLE_KEY

        return DefaultCreateCardPresentSetupIntentCallbackRetriever(
            errorReporter = errorReporter,
            requestOptionsProvider = { ApiRequest.Options(apiKey = apiKey) },
            createCardPresentSetupIntentCallbackProvider = callbackProvider,
        )
    }

    private companion object {
        const val LIVE_PUBLISHABLE_KEY = "pk_live_123"
    }
}
