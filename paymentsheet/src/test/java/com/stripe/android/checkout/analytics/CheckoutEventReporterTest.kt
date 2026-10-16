package com.stripe.android.checkout.analytics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiConfigurationPreview
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import com.stripe.android.paymentsheet.analytics.defaultAnalyticsParams
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.utils.FakeDurationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ApiConfigurationPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutEventReporterTest {
    @Test
    fun `success uses PaymentSheetEvent params and checkout duration`() = runTest {
        val duration = 1250.milliseconds
        val durationProvider = FakeDurationProvider(duration)
        val executor = FakeAnalyticsRequestExecutor()
        val reporter = CheckoutEventReporterTestFactory.create(
            executor,
            durationProvider = durationProvider,
        )
        val deferredType = DeferredIntentConfirmationType.Client
        val intentId = "pi_checkout_reporter"

        reporter.succeeded(
            paymentSelection = PaymentSelection.GooglePay,
            deferredIntentConfirmationType = deferredType,
            intentId = intentId,
        )

        val request = executor.getExecutedRequests().single()
        val expectedPaymentEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Embedded,
            result = PaymentSheetEvent.Payment.Result.Success,
            duration = duration,
            paymentSelection = PaymentSelection.GooglePay,
            deferredIntentConfirmationType = deferredType,
            intentId = intentId,
        )
        assertThat(request.params["event"]).isEqualTo("checkout_elements.confirm.succeeded")
        assertPaymentEventParams(request.params, expectedPaymentEvent)
        assertThat(request.params).containsEntry("product_usage", "checkout_elements")
        assertThat(request.params).containsEntry("publishable_key", "pk_test_checkout_analytics")
        assertThat(durationProvider.has(FakeDurationProvider.Call.End(DurationProvider.Key.Checkout)))
            .isTrue()
    }

    @Test
    fun `Google Pay failure uses PaymentSheetEvent error classification`() = runTest {
        val duration = 500.milliseconds
        val durationProvider = FakeDurationProvider(duration)
        val executor = FakeAnalyticsRequestExecutor()
        val reporter = CheckoutEventReporterTestFactory.create(
            executor,
            durationProvider = durationProvider,
        )
        val error = PaymentSheetConfirmationError.GooglePay(errorCodeInt = 12)

        reporter.failed(
            paymentSelection = PaymentSelection.GooglePay,
            error = error,
        )

        val request = executor.getExecutedRequests().single()
        val expectedPaymentEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Embedded,
            result = PaymentSheetEvent.Payment.Result.Failure(error),
            duration = duration,
            paymentSelection = PaymentSelection.GooglePay,
            deferredIntentConfirmationType = null,
            intentId = null,
        )
        assertThat(request.params["event"]).isEqualTo("checkout_elements.confirm.failed")
        assertPaymentEventParams(request.params, expectedPaymentEvent)
        assertThat(request.params).containsEntry("error_message", "googlePay_12")
        assertThat(request.params).containsEntry("error_code", "12")
        assertThat(durationProvider.has(FakeDurationProvider.Call.End(DurationProvider.Key.Checkout)))
            .isTrue()
    }

    @Test
    fun `metadata and publishable key are snapshotted before request dispatch`() = runTest {
        val metadataBefore = paymentMethodMetadata(
            publishableKey = "pk_test_metadata_before",
            marker = "before",
        )
        val metadataAfter = paymentMethodMetadata(
            publishableKey = "pk_test_metadata_after",
            marker = "after",
        )
        var currentMetadata = metadataBefore
        val metadataProviderCalls = Turbine<Unit>()
        val metadataProvider = Provider<PaymentMethodMetadata?> {
            metadataProviderCalls.add(Unit)
            currentMetadata
        }
        val executor = FakeAnalyticsRequestExecutor()
        val reporter = CheckoutEventReporterTestFactory.create(
            analyticsRequestExecutor = executor,
            paymentMethodMetadataProvider = metadataProvider,
            durationProvider = FakeDurationProvider(1.seconds),
            workContext = StandardTestDispatcher(testScheduler),
        )
        metadataProviderCalls.expectNoEvents()

        reporter.succeeded(
            paymentSelection = PaymentSelection.GooglePay,
            deferredIntentConfirmationType = null,
            intentId = null,
        )
        metadataProviderCalls.awaitItem()
        metadataProviderCalls.expectNoEvents()
        assertThat(executor.getExecutedRequests()).isEmpty()

        currentMetadata = metadataAfter
        testScheduler.runCurrent()

        val request = executor.getExecutedRequests().single()
        assertThat(request.params).containsEntry("publishable_key", "pk_test_metadata_before")
        assertThat(request.params).containsEntry("metadata_snapshot", "before")
        metadataBefore.defaultAnalyticsParams().forEach { (key, value) ->
            assertThat(request.params).containsEntry(key, value)
        }
        metadataProviderCalls.ensureAllEventsConsumed()
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

    private fun assertPaymentEventParams(
        actual: Map<String, *>,
        expected: PaymentSheetEvent.Payment,
    ) {
        expected.params.forEach { (key, value) ->
            assertThat(actual).containsEntry(key, value)
        }
    }
}

@OptIn(ApiConfigurationPreview::class)
internal object CheckoutEventReporterTestFactory {
    fun create(
        analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        paymentMethodMetadataProvider: Provider<PaymentMethodMetadata?> =
            Provider<PaymentMethodMetadata?> { null },
        durationProvider: DurationProvider = FakeDurationProvider(),
        workContext: CoroutineContext = Dispatchers.Unconfined,
    ): CheckoutEventReporter {
        val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext<Context>(),
            defaultProductUsageTokens = setOf("checkout_elements"),
            apiConfigurationProvider = Provider {
                ApiConfiguration("pk_test_checkout_analytics").build()
            },
        )
        return CheckoutEventReporter(
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = analyticsRequestFactory,
            durationProvider = durationProvider,
            paymentMethodMetadataProvider = paymentMethodMetadataProvider,
            workContext = workContext,
        )
    }
}
