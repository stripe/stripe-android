package com.stripe.android.checkout

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiConfigurationPreview
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationTypeKey
import com.stripe.android.paymentsheet.analytics.FakeAnalyticsRequestExecutor
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.utils.FakeDurationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ApiConfigurationPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutAnalyticsPerformerTest {
    @Test
    fun `payment element confirmation reports success and clears saved context`() = runScenario {
        val selection = PaymentSelection.GooglePay
        performer.onPaymentElementConfirmationStarted(selection)

        assertThat(savedConfirmationKeys()).hasSize(2)
        analyticsRequestExecutor.requestTurbine.expectNoEvents()

        startReporting()

        val success = successfulResult()
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(success)
        runCurrent()

        analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.succeeded",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = mapOf(
                    "selected_lpm" to "google_pay",
                    "duration" to 2f,
                    "intent_id" to success.intent.id,
                    "deferred_intent_confirmation_type" to "client",
                ),
            ),
        )
        assertThat(savedConfirmationKeys()).isEmpty()

        confirmationHandler.state.value = ConfirmationHandler.State.Idle
        runCurrent()
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(success)
        runCurrent()
        analyticsRequestExecutor.requestTurbine.expectNoEvents()
    }

    @Test
    fun `express checkout confirmation context survives performer recreation`() = runScenario {
        val selection = PaymentSelection.Link(brand = LinkBrand.Link)
        performer.onExpressCheckoutElementConfirmationStarted(selection)

        val restoredPerformer = recreatePerformer()
        startReporting(restoredPerformer)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())
        runCurrent()

        analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.succeeded",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = mapOf("selected_lpm" to "link"),
            ),
        )
        assertThat(savedConfirmationKeys()).isEmpty()
    }

    @Test
    fun `payment failure reports safe fields`() = runScenario {
        startReporting()
        performer.onExpressCheckoutElementConfirmationStarted(PaymentSelection.GooglePay)
        val privateMessage = "private payment details"
        val error = APIException(
            stripeError = StripeError(
                type = "card_error",
                code = "card_declined",
                message = privateMessage,
            ),
            requestId = "req_failed_confirmation",
            message = privateMessage,
        )
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = error,
                message = privateMessage.resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )
        runCurrent()

        val request = analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.failed",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = mapOf(
                    "selected_lpm" to "google_pay",
                    "duration" to 2f,
                    "error_code" to "card_declined",
                ),
            ),
        )
        assertThat(request.toString()).doesNotContain(privateMessage)
        assertThat(savedConfirmationKeys()).isEmpty()
    }

    @Test
    fun `Google Pay failure preserves its analytics error classification`() = runScenario {
        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        val privateMessage = "private Google Pay details"
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = IllegalStateException(privateMessage),
                message = privateMessage.resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.GooglePay(errorCode = 12),
            )
        )
        runCurrent()

        val request = analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.failed",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = mapOf(
                    "selected_lpm" to "google_pay",
                    "error_message" to "googlePay_12",
                    "error_code" to "12",
                ),
            ),
        )
        assertThat(request.toString()).doesNotContain(privateMessage)
    }

    @Test
    fun `canceled confirmation does not report a terminal event`() = runScenario {
        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                ConfirmationHandler.Result.Canceled.Action.InformCancellation
            )
        )
        runCurrent()

        analyticsRequestExecutor.requestTurbine.expectNoEvents()
        assertThat(savedConfirmationKeys()).isEmpty()
    }

    @Test
    fun `missing metadata uses API configuration fallback and checkout usage`() = runScenario(
        paymentMethodMetadata = null,
    ) {
        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            successfulResult(deferredIntentConfirmationType = null)
        )
        runCurrent()

        analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.succeeded",
                publishableKey = FALLBACK_PUBLISHABLE_KEY,
                params = emptyMap(),
            ),
        )
    }

    @Test
    fun `metadata is snapshotted before request dispatch`() = runScenario(
        workContext = { scheduler -> StandardTestDispatcher(scheduler) },
    ) {
        val metadataBefore = paymentMethodMetadata(marker = "before")
        val metadataAfter = paymentMethodMetadata(marker = "after")
        paymentMethodMetadata = metadataBefore

        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            successfulResult(deferredIntentConfirmationType = null)
        )
        analyticsRequestExecutor.requestTurbine.expectNoEvents()

        paymentMethodMetadata = metadataAfter
        runCurrent()

        analyticsRequestExecutor.awaitAndAssertConfirmationRequest(
            ExpectedEvent(
                event = "checkout_elements.confirm.succeeded",
                publishableKey = DEFAULT_METADATA_PUBLISHABLE_KEY,
                params = mapOf("metadata_snapshot" to "before"),
            ),
        )
    }

    private fun runScenario(
        duration: Duration = 2.seconds,
        paymentMethodMetadata: PaymentMethodMetadata? = defaultPaymentMethodMetadata(),
        workContext: (TestCoroutineScheduler) -> CoroutineContext = { scheduler ->
            UnconfinedTestDispatcher(scheduler)
        },
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val savedStateHandle = SavedStateHandle()
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val durationProvider = FakeDurationProvider(duration)
        val scenario = Scenario(
            confirmationHandler = confirmationHandler,
            savedStateHandle = savedStateHandle,
            context = ApplicationProvider.getApplicationContext<Context>(),
            analyticsRequestExecutor = analyticsRequestExecutor,
            durationProvider = durationProvider,
            paymentMethodMetadata = paymentMethodMetadata,
            fallbackApiConfiguration = ApiConfiguration(FALLBACK_PUBLISHABLE_KEY).build(),
            backgroundScope = backgroundScope,
            testScheduler = testScheduler,
            workContext = workContext(testScheduler),
        )

        scenario.block()

        confirmationHandler.validate()
        analyticsRequestExecutor.validate()
    }

    private class Scenario(
        val confirmationHandler: FakeConfirmationHandler,
        val savedStateHandle: SavedStateHandle,
        private val context: Context,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val durationProvider: FakeDurationProvider,
        paymentMethodMetadata: PaymentMethodMetadata?,
        private val fallbackApiConfiguration: ApiConfiguration.State,
        private val backgroundScope: CoroutineScope,
        private val testScheduler: TestCoroutineScheduler,
        private val workContext: CoroutineContext,
    ) {
        var paymentMethodMetadata: PaymentMethodMetadata? = paymentMethodMetadata

        private val paymentMethodMetadataProvider = Provider<PaymentMethodMetadata?> {
            paymentMethodMetadata
        }
        private val apiConfigurationProvider = Provider { fallbackApiConfiguration }

        val performer: CheckoutAnalyticsPerformer = createPerformer()

        fun recreatePerformer(): CheckoutAnalyticsPerformer = createPerformer()

        fun startReporting(performer: CheckoutAnalyticsPerformer = this.performer) {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                performer.reportConfirmationResults()
            }
        }

        fun runCurrent() {
            testScheduler.runCurrent()
        }

        fun savedConfirmationKeys(): Set<String> =
            savedStateHandle.keys().filter { it.startsWith("CheckoutAnalyticsPerformer.") }.toSet()

        private fun createPerformer(): CheckoutAnalyticsPerformer = CheckoutAnalyticsPerformer(
            confirmationHandler = confirmationHandler,
            savedStateHandle = savedStateHandle,
            context = context,
            analyticsRequestExecutor = analyticsRequestExecutor,
            durationProvider = durationProvider,
            paymentMethodMetadataProvider = paymentMethodMetadataProvider,
            apiConfigurationProvider = apiConfigurationProvider,
            workContext = workContext,
        )
    }

    private fun paymentMethodMetadata(
        marker: String,
    ): PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
        analyticsMetadata = AnalyticsMetadata(
            mapOf(
                "metadata_snapshot" to AnalyticsMetadata.Value.SimpleString(marker),
            )
        ),
        apiConfiguration = ApiConfiguration(DEFAULT_METADATA_PUBLISHABLE_KEY).build(),
    )

    private fun defaultPaymentMethodMetadata(): PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
        apiConfiguration = ApiConfiguration(DEFAULT_METADATA_PUBLISHABLE_KEY).build(),
    )

    private data class ExpectedEvent(
        val event: String,
        val publishableKey: String,
        val params: Map<String, Any?>,
    )

    private suspend fun FakeAnalyticsRequestExecutor.awaitAndAssertConfirmationRequest(
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

        fun successfulResult(
            deferredIntentConfirmationType: DeferredIntentConfirmationType? = DeferredIntentConfirmationType.Client,
        ) = ConfirmationHandler.Result.Succeeded(
            intent = PaymentIntentFixtures.PI_SUCCEEDED,
            metadata = MutableConfirmationMetadata().apply {
                deferredIntentConfirmationType?.let {
                    set(DeferredIntentConfirmationTypeKey, it)
                }
            },
        )
    }
}
