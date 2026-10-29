@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiConfigurationPreview
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentsheet.analytics.FakeAnalyticsRequestExecutor
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.utils.FakeDurationProvider
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

@OptIn(ApiConfigurationPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutInitAnalyticsPerformerTest {
    @Test
    fun `init uses the active API configuration instead of previously committed payment metadata`() = runScenario {
        performer.createInitAttempt("cs_test_newinit").started(response = null)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "checkout_elements.init.started")
        assertThat(request.params).containsEntry("product_usage", "checkout_elements")
        assertThat(request.params).containsEntry("publishable_key", FALLBACK_PUBLISHABLE_KEY)
        assertThat(request.params).containsEntry("checkout_session_id", "cs_test_newinit")
        assertThat(request.params).doesNotContainKey("checkout_config_id")
    }

    @Test
    fun `queued init attempt includes its wait in its own duration`() = runScenario {
        val first = performer.createInitAttempt("cs_test_first")
        first.started(response = null)
        val firstStarted = analyticsRequestExecutor.awaitAndAssertRequest(
            ExpectedEvent(
                event = "checkout_elements.init.started",
                publishableKey = FALLBACK_PUBLISHABLE_KEY,
                params = mapOf("checkout_session_id" to "cs_test_first"),
            )
        )
        assertThat(firstStarted).doesNotContainKey("duration")

        timeSource += 150.milliseconds
        val second = performer.createInitAttempt("cs_test_second")
        second.started(response = null)
        analyticsRequestExecutor.awaitAndAssertRequest(
            ExpectedEvent(
                event = "checkout_elements.init.started",
                publishableKey = FALLBACK_PUBLISHABLE_KEY,
                params = mapOf("checkout_session_id" to "cs_test_second"),
            )
        )

        timeSource += 350.milliseconds
        first.succeeded(
            response = CheckoutSessionResponseFactory.create(id = "cs_test_first"),
            orderedLpms = listOf("card", "klarna"),
        )
        analyticsRequestExecutor.awaitAndAssertRequest(
            ExpectedEvent(
                event = "checkout_elements.init.succeeded",
                publishableKey = FALLBACK_PUBLISHABLE_KEY,
                params = mapOf(
                    "checkout_session_id" to "cs_test_first",
                    "ordered_lpms" to "card,klarna",
                    "duration" to 0.5f,
                ),
            )
        )

        timeSource += 500.milliseconds
        second.failed(response = null, error = IllegalStateException("private failure details"))
        val failed = analyticsRequestExecutor.awaitAndAssertRequest(
            ExpectedEvent(
                event = "checkout_elements.init.failed",
                publishableKey = FALLBACK_PUBLISHABLE_KEY,
                params = mapOf(
                    "checkout_session_id" to "cs_test_second",
                    "duration" to 0.85f,
                    "error_type" to "java.lang.IllegalStateException",
                ),
            )
        )
        assertThat(failed).doesNotContainKey("checkout_config_id")
        assertThat(failed.toString()).doesNotContain("private failure details")
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            apiConfiguration = ApiConfiguration(DEFAULT_METADATA_PUBLISHABLE_KEY).build(),
        )
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle).apply {
            state = CheckoutControllerStateFactory.create(
                paymentSelection = PaymentSelection.GooglePay,
                paymentMethodMetadata = paymentMethodMetadata,
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                    configId = "cfg_checkout",
                    elementsSession = ElementsSession.createFromFallback(
                        stripeIntent = paymentMethodMetadata.stripeIntent,
                        sessionsError = null,
                    ).copy(elementsSessionConfigId = "cfg_elements"),
                ),
            )
        }
        val confirmationHandler = FakeConfirmationHandler()
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val durationProvider = FakeDurationProvider()
        val timeSource = TestTimeSource()
        val activeApiConfiguration = ApiConfiguration(FALLBACK_PUBLISHABLE_KEY).build()
        val performer = TestCheckoutAnalyticsPerformerFactory.create(
            confirmationHandler = confirmationHandler,
            savedStateHandle = savedStateHandle,
            stateHolder = stateHolder,
            analyticsRequestExecutor = analyticsRequestExecutor,
            durationProvider = durationProvider,
            apiConfiguration = activeApiConfiguration,
            timeSource = timeSource,
            workContext = UnconfinedTestDispatcher(testScheduler),
        )

        Scenario(
            performer = performer,
            analyticsRequestExecutor = analyticsRequestExecutor,
            timeSource = timeSource,
        ).apply { block() }

        confirmationHandler.validate()
        durationProvider.validate()
        analyticsRequestExecutor.validate()
    }

    private data class Scenario(
        val performer: CheckoutAnalyticsPerformer,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val timeSource: TestTimeSource,
    )

    private data class ExpectedEvent(
        val event: String,
        val publishableKey: String,
        val params: Map<String, Any?>,
    )

    private suspend fun FakeAnalyticsRequestExecutor.awaitAndAssertRequest(
        expected: ExpectedEvent,
    ): Map<String, *> {
        val request = requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", expected.event)
        assertThat(request.params).containsEntry("product_usage", "checkout_elements")
        assertThat(request.params).containsEntry("publishable_key", expected.publishableKey)
        expected.params.forEach { (key, value) ->
            assertThat(request.params).containsEntry(key, value)
        }
        return request.params
    }

    private companion object {
        const val FALLBACK_PUBLISHABLE_KEY = "pk_test_checkout_analytics"
        const val DEFAULT_METADATA_PUBLISHABLE_KEY = "pk_test_checkout_metadata"
    }
}
