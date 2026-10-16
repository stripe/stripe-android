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
import com.stripe.android.core.utils.DurationProvider
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
import com.stripe.android.utils.FakeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
        confirmationHandler.state.value = ConfirmationHandler.State.Idle
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(success)
        runCurrent()
        val terminal = analyticsRequestExecutor.requestTurbine.awaitItem().params
        paymentMethodMetadataProvider.getCalls.expectMostRecentItem()
        apiConfigurationProvider.getCalls.expectNoEvents()
        analyticsRequestExecutor.requestTurbine.expectNoEvents()

        assertThat(terminal["event"]).isEqualTo("checkout_elements.confirm.succeeded")
        assertThat(terminal).containsEntry("selected_lpm", "google_pay")
        assertThat(terminal).containsEntry("deferred_intent_confirmation_type", "client")
        assertThat(terminal).containsEntry("intent_id", success.intent.id)
        assertThat(terminal).containsEntry("product_usage", "checkout_elements")
        assertThat(terminal).containsEntry(
            "publishable_key",
            DEFAULT_METADATA_PUBLISHABLE_KEY,
        )
        assertThat(terminal).containsEntry("duration", 2f)
        assertThat(savedConfirmationKeys()).isEmpty()
        assertThat(durationProvider.has(FakeDurationProvider.Call.End(DurationProvider.Key.Checkout)))
            .isTrue()
    }

    @Test
    fun `express checkout confirmation context survives performer recreation`() = runScenario {
        val selection = PaymentSelection.Link(brand = LinkBrand.Link)
        performer.onExpressCheckoutElementConfirmationStarted(selection)

        val restoredPerformer = recreatePerformer()
        startReporting(restoredPerformer)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successfulResult())
        runCurrent()

        val terminal = analyticsRequestExecutor.requestTurbine.awaitItem().params
        paymentMethodMetadataProvider.getCalls.expectMostRecentItem()
        apiConfigurationProvider.getCalls.expectNoEvents()
        analyticsRequestExecutor.requestTurbine.expectNoEvents()
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

        val terminal = analyticsRequestExecutor.requestTurbine.awaitItem().params
        paymentMethodMetadataProvider.getCalls.expectMostRecentItem()
        apiConfigurationProvider.getCalls.expectNoEvents()
        analyticsRequestExecutor.requestTurbine.expectNoEvents()
        assertThat(terminal["event"]).isEqualTo("checkout_elements.confirm.failed")
        assertThat(terminal).containsEntry("selected_lpm", "google_pay")
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

        val terminal = analyticsRequestExecutor.requestTurbine.awaitItem().params
        paymentMethodMetadataProvider.getCalls.expectMostRecentItem()
        apiConfigurationProvider.getCalls.expectNoEvents()
        analyticsRequestExecutor.requestTurbine.expectNoEvents()
        assertThat(terminal["event"]).isEqualTo("checkout_elements.confirm.failed")
        assertThat(terminal).containsEntry("selected_lpm", "google_pay")
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

        analyticsRequestExecutor.requestTurbine.expectNoEvents()
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

        val request = analyticsRequestExecutor.requestTurbine.awaitItem().params
        paymentMethodMetadataProvider.getCalls.expectMostRecentItem()
        apiConfigurationProvider.getCalls.expectMostRecentItem()
        analyticsRequestExecutor.requestTurbine.expectNoEvents()
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
        paymentMethodMetadataProvider.value = metadataBefore
        assertAnalyticsProvidersNotCalled()

        startReporting()
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            successfulResult(deferredIntentConfirmationType = null)
        )
        paymentMethodMetadataProvider.getCalls.awaitItem()
        apiConfigurationProvider.getCalls.expectNoEvents()
        analyticsRequestExecutor.requestTurbine.expectNoEvents()

        paymentMethodMetadataProvider.value = metadataAfter
        runCurrent()

        val request = analyticsRequestExecutor.requestTurbine.awaitItem().params
        paymentMethodMetadataProvider.getCalls.expectMostRecentItem()
        apiConfigurationProvider.getCalls.expectNoEvents()
        analyticsRequestExecutor.requestTurbine.expectNoEvents()
        assertThat(request).containsEntry("publishable_key", "pk_test_metadata_before")
        assertThat(request).containsEntry("metadata_snapshot", "before")
        assertThat(request).containsEntry("product_usage", "checkout_elements")
    }

    @Test
    fun `successive confirmations use the current metadata publishable key`() = runScenario {
        val metadataA = paymentMethodMetadata(
            publishableKey = "pk_test_metadata_a",
            marker = "metadata_a",
        )
        val metadataB = paymentMethodMetadata(
            publishableKey = "pk_test_metadata_b",
            marker = "metadata_b",
        )
        paymentMethodMetadataProvider.value = metadataA
        startReporting()

        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            successfulResult(deferredIntentConfirmationType = null)
        )
        runCurrent()
        val firstRequest = analyticsRequestExecutor.requestTurbine.awaitItem().params
        paymentMethodMetadataProvider.getCalls.expectMostRecentItem()
        apiConfigurationProvider.getCalls.expectNoEvents()
        assertThat(firstRequest).containsEntry("publishable_key", "pk_test_metadata_a")
        assertThat(firstRequest).containsEntry("metadata_snapshot", "metadata_a")
        analyticsRequestExecutor.requestTurbine.expectNoEvents()

        confirmationHandler.state.value = ConfirmationHandler.State.Idle
        runCurrent()
        paymentMethodMetadataProvider.value = metadataB
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            successfulResult(deferredIntentConfirmationType = null)
        )
        runCurrent()

        val secondRequest = analyticsRequestExecutor.requestTurbine.awaitItem().params
        paymentMethodMetadataProvider.getCalls.expectMostRecentItem()
        apiConfigurationProvider.getCalls.expectNoEvents()
        analyticsRequestExecutor.requestTurbine.expectNoEvents()
        assertThat(secondRequest).containsEntry("publishable_key", "pk_test_metadata_b")
        assertThat(secondRequest).containsEntry("metadata_snapshot", "metadata_b")
        assertThat(secondRequest).containsEntry("product_usage", "checkout_elements")
        assertThat(savedConfirmationKeys()).isEmpty()
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

        analyticsRequestExecutor.requestTurbine.expectNoEvents()
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
        val paymentMethodMetadataProvider = FakeProvider(paymentMethodMetadata)
        val apiConfigurationProvider = FakeProvider(
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
        analyticsRequestExecutor.validate()
        paymentMethodMetadataProvider.ensureAllEventsConsumed()
        apiConfigurationProvider.ensureAllEventsConsumed()
    }

    private class Scenario(
        val confirmationHandler: FakeConfirmationHandler,
        val savedStateHandle: SavedStateHandle,
        private val context: Context,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val durationProvider: FakeDurationProvider,
        val paymentMethodMetadataProvider: FakeProvider<PaymentMethodMetadata?>,
        val apiConfigurationProvider: FakeProvider<ApiConfiguration.State>,
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

        fun savedConfirmationKeys(): Set<String> =
            savedStateHandle.keys().filter { it.startsWith("CheckoutAnalyticsPerformer.") }.toSet()

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
