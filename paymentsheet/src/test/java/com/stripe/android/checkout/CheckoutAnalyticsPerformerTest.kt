package com.stripe.android.checkout

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiConfigurationPreview
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.defaultAnalyticsParams
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationTypeKey
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
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
    fun `payment element success reports once and clears saved context`() = runScenario {
        val selection = PaymentSelection.GooglePay
        performer.onPaymentElementConfirmationStarted(selection)

        assertThat(savedConfirmationKeys()).hasSize(2)
        assertThat(events()).isEmpty()

        startReporting()

        val success = successfulResult()
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(success)
        confirmationHandler.state.value = ConfirmationHandler.State.Idle
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(success)
        runCurrent()
        awaitAnalyticsProviderCalls(metadataCalls = 1, apiConfigurationCalls = 0)

        val terminalEvents = events()
        assertThat(terminalEvents.map { it["event"] })
            .containsExactly("checkout_elements.confirm.succeeded")
        assertPaymentEventParams(
            actual = terminalEvents.single(),
            expected = PaymentSheetEvent.Payment(
                mode = EventReporter.Mode.Embedded,
                result = PaymentSheetEvent.Payment.Result.Success,
                duration = 2.seconds,
                paymentSelection = selection,
                deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
                intentId = PaymentIntentFixtures.PI_SUCCEEDED.id,
            ),
        )
        assertThat(terminalEvents.single()).containsEntry("product_usage", "checkout_elements")
        assertThat(terminalEvents.single()).containsEntry(
            "publishable_key",
            DEFAULT_METADATA_PUBLISHABLE_KEY,
        )
        assertThat(terminalEvents.single()).containsEntry("duration", 2f)
        assertThat(savedConfirmationKeys()).isEmpty()
        assertThat(durationProvider.has(FakeDurationProvider.Call.End(DurationProvider.Key.Checkout)))
            .isTrue()
    }

    @Test
    fun `express checkout Link selection is restored after performer recreation`() = runScenario {
        val selection = PaymentSelection.Link(brand = LinkBrand.Link)
        performer.onExpressCheckoutElementConfirmationStarted(selection)

        val restoredPerformer = recreatePerformer()
        startReporting(restoredPerformer)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())
        runCurrent()
        awaitAnalyticsProviderCalls(metadataCalls = 1, apiConfigurationCalls = 0)

        val terminal = events().single()
        assertThat(terminal["event"]).isEqualTo("checkout_elements.confirm.succeeded")
        assertThat(terminal).containsEntry("selected_lpm", "link")
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
        awaitAnalyticsProviderCalls(metadataCalls = 1, apiConfigurationCalls = 0)

        val terminal = events().single()
        assertThat(terminal["event"]).isEqualTo("checkout_elements.confirm.failed")
        assertPaymentEventParams(
            actual = terminal,
            expected = PaymentSheetEvent.Payment(
                mode = EventReporter.Mode.Embedded,
                result = PaymentSheetEvent.Payment.Result.Failure(
                    PaymentSheetConfirmationError.Stripe(error)
                ),
                duration = 2.seconds,
                paymentSelection = PaymentSelection.GooglePay,
                deferredIntentConfirmationType = null,
                intentId = null,
            ),
        )
        assertThat(terminal).containsEntry("error_code", "card_declined")
        assertThat(terminal).containsEntry("duration", 2f)
        assertThat(terminal.toString()).doesNotContain(privateMessage)
        assertThat(savedConfirmationKeys()).isEmpty()
        assertThat(durationProvider.has(FakeDurationProvider.Call.End(DurationProvider.Key.Checkout)))
            .isTrue()
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
        awaitAnalyticsProviderCalls(metadataCalls = 1, apiConfigurationCalls = 0)

        val terminal = events().single()
        assertThat(terminal["event"]).isEqualTo("checkout_elements.confirm.failed")
        assertPaymentEventParams(
            actual = terminal,
            expected = PaymentSheetEvent.Payment(
                mode = EventReporter.Mode.Embedded,
                result = PaymentSheetEvent.Payment.Result.Failure(
                    PaymentSheetConfirmationError.GooglePay(errorCodeInt = 12)
                ),
                duration = 2.seconds,
                paymentSelection = PaymentSelection.GooglePay,
                deferredIntentConfirmationType = null,
                intentId = null,
            ),
        )
        assertThat(terminal).containsEntry("error_message", "googlePay_12")
        assertThat(terminal).containsEntry("error_code", "12")
        assertThat(terminal.toString()).doesNotContain(privateMessage)
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

        assertThat(events()).isEmpty()
        assertThat(savedConfirmationKeys()).isEmpty()
        assertThat(durationProvider.has(FakeDurationProvider.Call.End(DurationProvider.Key.Checkout)))
            .isFalse()
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
        awaitAnalyticsProviderCalls(metadataCalls = 1, apiConfigurationCalls = 1)

        val request = events().single()
        assertThat(request).containsEntry("event", "checkout_elements.confirm.succeeded")
        assertThat(request).containsEntry("publishable_key", FALLBACK_PUBLISHABLE_KEY)
        assertThat(request).containsEntry("product_usage", "checkout_elements")
    }

    @Test
    fun `internal failure is filtered`() = assertFilteredFailure(
        ConfirmationHandler.Result.Failed.ErrorType.Internal
    )

    @Test
    fun `merchant integration failure is filtered`() = assertFilteredFailure(
        ConfirmationHandler.Result.Failed.ErrorType.MerchantIntegration
    )

    @Test
    fun `fatal failure is filtered`() = assertFilteredFailure(
        ConfirmationHandler.Result.Failed.ErrorType.Fatal
    )

    @Test
    fun `metadata and publishable key are snapshotted before request dispatch`() = runScenario(
        workContext = { scheduler -> StandardTestDispatcher(scheduler) },
    ) {
        val metadataBefore = paymentMethodMetadata(
            publishableKey = "pk_test_metadata_before",
            marker = "before",
        )
        val metadataAfter = paymentMethodMetadata(
            publishableKey = "pk_test_metadata_after",
            marker = "after",
        )
        paymentMethodMetadataProvider.metadata = metadataBefore
        assertAnalyticsProvidersNotCalled()

        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            successfulResult(deferredIntentConfirmationType = null)
        )
        paymentMethodMetadataProvider.getCalls.awaitItem()
        apiConfigurationProvider.getCalls.expectNoEvents()
        assertThat(events()).isEmpty()

        paymentMethodMetadataProvider.metadata = metadataAfter
        runCurrent()

        val request = events().single()
        assertThat(request).containsEntry("publishable_key", "pk_test_metadata_before")
        assertThat(request).containsEntry("metadata_snapshot", "before")
        assertThat(request).containsEntry("product_usage", "checkout_elements")
        metadataBefore.defaultAnalyticsParams().forEach { (key, value) ->
            assertThat(request).containsEntry(key, value)
        }
    }

    private fun assertFilteredFailure(
        type: ConfirmationHandler.Result.Failed.ErrorType,
    ) = runScenario {
        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = IllegalStateException("filtered failure"),
                message = "Filtered failure".resolvableString,
                type = type,
            )
        )
        runCurrent()

        assertThat(events()).isEmpty()
        assertThat(savedConfirmationKeys()).isEmpty()
        assertThat(durationProvider.has(FakeDurationProvider.Call.End(DurationProvider.Key.Checkout)))
            .isFalse()
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
        val paymentMethodMetadataProvider = FakePaymentMethodMetadataProvider(paymentMethodMetadata)
        val apiConfigurationProvider = FakeApiConfigurationProvider(
            ApiConfiguration(FALLBACK_PUBLISHABLE_KEY).build()
        )
        val scenario = Scenario(
            confirmationHandler = confirmationHandler,
            savedStateHandle = savedStateHandle,
            context = ApplicationProvider.getApplicationContext<Context>(),
            analyticsRequestExecutor = analyticsRequestExecutor,
            durationProvider = durationProvider,
            paymentMethodMetadataProvider = paymentMethodMetadataProvider,
            apiConfigurationProvider = apiConfigurationProvider,
            backgroundScope = backgroundScope,
            testScheduler = testScheduler,
            workContext = workContext(testScheduler),
        )

        scenario.block()

        confirmationHandler.validate()
        paymentMethodMetadataProvider.ensureAllEventsConsumed()
        apiConfigurationProvider.ensureAllEventsConsumed()
    }

    private class Scenario(
        val confirmationHandler: FakeConfirmationHandler,
        val savedStateHandle: SavedStateHandle,
        private val context: Context,
        private val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val durationProvider: FakeDurationProvider,
        val paymentMethodMetadataProvider: FakePaymentMethodMetadataProvider,
        val apiConfigurationProvider: FakeApiConfigurationProvider,
        private val backgroundScope: CoroutineScope,
        private val testScheduler: TestCoroutineScheduler,
        private val workContext: CoroutineContext,
    ) {
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

        fun events(): List<Map<String, *>> =
            analyticsRequestExecutor.getExecutedRequests().map { it.params }

        fun savedConfirmationKeys(): Set<String> =
            savedStateHandle.keys().filter { it.startsWith("CheckoutAnalyticsPerformer.") }.toSet()

        suspend fun awaitAnalyticsProviderCalls(
            metadataCalls: Int,
            apiConfigurationCalls: Int,
        ) {
            repeat(metadataCalls) { paymentMethodMetadataProvider.getCalls.awaitItem() }
            repeat(apiConfigurationCalls) { apiConfigurationProvider.getCalls.awaitItem() }
        }

        fun assertAnalyticsProvidersNotCalled() {
            paymentMethodMetadataProvider.getCalls.expectNoEvents()
            apiConfigurationProvider.getCalls.expectNoEvents()
        }

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

    private class FakePaymentMethodMetadataProvider(
        var metadata: PaymentMethodMetadata? = null,
    ) : Provider<PaymentMethodMetadata?> {
        val getCalls = Turbine<Unit>()

        override fun get(): PaymentMethodMetadata? {
            getCalls.add(Unit)
            return metadata
        }

        fun ensureAllEventsConsumed() = getCalls.ensureAllEventsConsumed()
    }

    private class FakeApiConfigurationProvider(
        private val apiConfiguration: ApiConfiguration.State,
    ) : Provider<ApiConfiguration.State> {
        val getCalls = Turbine<Unit>()

        override fun get(): ApiConfiguration.State {
            getCalls.add(Unit)
            return apiConfiguration
        }

        fun ensureAllEventsConsumed() = getCalls.ensureAllEventsConsumed()
    }

    private fun assertPaymentEventParams(
        actual: Map<String, *>,
        expected: PaymentSheetEvent.Payment,
    ) {
        expected.params.forEach { (key, value) ->
            assertThat(actual).containsEntry(key, value)
        }
    }

    private fun paymentMethodMetadata(
        publishableKey: String,
        marker: String,
    ): PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
        analyticsMetadata = AnalyticsMetadata(
            mapOf(
                "metadata_snapshot" to AnalyticsMetadata.Value.SimpleString(marker),
            )
        ),
        apiConfiguration = ApiConfiguration(publishableKey).build(),
    )

    private fun defaultPaymentMethodMetadata(): PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
        apiConfiguration = ApiConfiguration(DEFAULT_METADATA_PUBLISHABLE_KEY).build(),
    )

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
