package com.stripe.android.analytics

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class DefaultPaymentSessionEventReporterTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val durationProvider = FakeDurationProvider((2).seconds)
    private val analyticsRequestExecutor = mock<AnalyticsRequestExecutor>()
    private val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
        ApplicationProvider.getApplicationContext(),
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )

    @Test
    fun `onLoadStarted() should fire analytics request with expected event value`() {
        val eventReporter = createEventReporter()

        eventReporter.onLoadStarted()

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "bi_load_started"
            }
        )
    }

    @Test
    fun `onLoadSucceeded() should fire analytics request with expected event value`() {
        val eventReporter = createEventReporter()

        eventReporter.onLoadStarted()
        eventReporter.onLoadSucceeded("card")

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "bi_load_succeeded" &&
                    req.params["selected_lpm"] == "card" &&
                    req.params["duration"] == 2f
            }
        )
    }

    @Test
    fun `onLoadFailed() should fire analytics request with expected event value`() {
        val eventReporter = createEventReporter()

        eventReporter.onLoadStarted()
        eventReporter.onLoadFailed(Exception())

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "bi_load_failed" &&
                    req.params["error_message"] == "unknown" &&
                    req.params["duration"] == 2f
            }
        )
    }

    @Test
    fun `onOptionsShown() should fire analytics request with expected event value`() {
        val eventReporter = createEventReporter()

        eventReporter.onOptionsShown()

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "bi_options_shown"
            }
        )
    }

    @Test
    fun `onFormShown() should fire analytics request with expected event value`() {
        val eventReporter = createEventReporter()

        eventReporter.onFormShown("card")

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "bi_form_shown" &&
                    req.params["selected_lpm"] == "card"
            }
        )
    }

    @Test
    fun `onFormShown() should restart duration on call`() {
        val durationProvider = FakeDurationProvider()

        val eventReporter = createEventReporter(durationProvider)

        eventReporter.onFormShown("card")

        assertThat(
            durationProvider.has(
                FakeDurationProvider.Call.Start(
                    key = DurationProvider.Key.ConfirmButtonClicked,
                    reset = true
                )
            )
        ).isTrue()
    }

    @Test
    fun `onFormInteracted() should fire analytics request with expected event value`() {
        val eventReporter = createEventReporter()

        eventReporter.onFormInteracted("card")

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "bi_form_interacted" &&
                    req.params["selected_lpm"] == "card"
            }
        )
    }

    @Test
    fun `onCardNumberCompleted() should fire analytics request with expected event value`() {
        val eventReporter = createEventReporter()

        eventReporter.onCardNumberCompleted()

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "bi_card_number_completed"
            }
        )
    }

    @Test
    fun `onDoneButtonTapped() should fire analytics request with expected event value`() {
        val eventReporter = createEventReporter()

        eventReporter.onDoneButtonTapped("card")

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "bi_done_button_tapped" &&
                    req.params["selected_lpm"] == "card" &&
                    req.params["duration"] == 2f
            }
        )
    }

    private fun createEventReporter(
        durationProvider: DurationProvider = this.durationProvider,
    ): PaymentSessionEventReporter {
        return DefaultPaymentSessionEventReporter(
            analyticsRequestExecutor = analyticsRequestExecutor,
            paymentAnalyticsRequestFactory = analyticsRequestFactory,
            durationProvider = durationProvider,
            workContext = testDispatcher
        )
    }
}
