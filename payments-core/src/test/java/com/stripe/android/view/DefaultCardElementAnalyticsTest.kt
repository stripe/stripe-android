package com.stripe.android.view

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.networking.PaymentAnalyticsEvent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultCardElementAnalyticsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `reportShown sends event once per session`() = test(
        widgetType = CardElementWidgetType.CardInputWidget,
    ) {
        analytics.reportShown(context)
        analytics.reportShown(context)

        assertThat(executor.awaitReportCall()).isEqualTo(
            FakeCardElementAnalyticsRequestExecutor.ReportCall(
                event = PaymentAnalyticsEvent.MobileCardElementShown,
                params = mapOf(
                    DefaultCardElementAnalytics.PARAM_WIDGET_TYPE to
                        CardElementWidgetType.CardInputWidget.analyticsValue,
                ),
            ),
        )
        executor.ensureAllEventsConsumed()
    }

    @Test
    fun `reportInteraction sends event once per session`() = test {
        analytics.reportInteraction(context)
        analytics.reportInteraction(context)

        assertThat(executor.awaitReportCall()).isEqualTo(
            FakeCardElementAnalyticsRequestExecutor.ReportCall(
                event = PaymentAnalyticsEvent.MobileCardElementInteraction,
                params = mapOf(
                    DefaultCardElementAnalytics.PARAM_WIDGET_TYPE to
                        CardElementWidgetType.CardFormView.analyticsValue,
                ),
            ),
        )
        executor.ensureAllEventsConsumed()
    }

    @Test
    fun `reportFormCompleted sends event once per session`() = test {
        analytics.reportFormCompleted(context)
        analytics.reportFormCompleted(context)

        assertThat(executor.awaitReportCall()).isEqualTo(
            FakeCardElementAnalyticsRequestExecutor.ReportCall(
                event = PaymentAnalyticsEvent.MobileCardElementFormCompleted,
                params = mapOf(
                    DefaultCardElementAnalytics.PARAM_WIDGET_TYPE to
                        CardElementWidgetType.CardFormView.analyticsValue,
                ),
            ),
        )
        executor.ensureAllEventsConsumed()
    }

    @Test
    fun `reportShown uses multiline widget type`() = test(
        widgetType = CardElementWidgetType.CardMultilineWidget,
    ) {
        analytics.reportShown(context)

        assertThat(executor.awaitReportCall()).isEqualTo(
            FakeCardElementAnalyticsRequestExecutor.ReportCall(
                event = PaymentAnalyticsEvent.MobileCardElementShown,
                params = mapOf(
                    DefaultCardElementAnalytics.PARAM_WIDGET_TYPE to
                        CardElementWidgetType.CardMultilineWidget.analyticsValue,
                ),
            ),
        )
    }

    @Test
    fun `saveState and restoreState preserve dedupe across instances`() = runTest {
        val executor = FakeCardElementAnalyticsRequestExecutor()
        val factory = FakeCardElementAnalyticsRequestExecutorFactory(executor = executor)

        val first = DefaultCardElementAnalytics(
            widgetType = CardElementWidgetType.CardFormView,
            cardElementAnalyticsRequestExecutorFactory = factory,
        )

        first.reportShown(context)
        first.reportInteraction(context)
        first.reportFormCompleted(context)

        assertThat(executor.awaitReportCall().event)
            .isEqualTo(PaymentAnalyticsEvent.MobileCardElementShown)
        assertThat(executor.awaitReportCall().event)
            .isEqualTo(PaymentAnalyticsEvent.MobileCardElementInteraction)
        assertThat(executor.awaitReportCall().event)
            .isEqualTo(PaymentAnalyticsEvent.MobileCardElementFormCompleted)

        val bundle = Bundle()
        first.saveState(bundle)

        val second = DefaultCardElementAnalytics(
            widgetType = CardElementWidgetType.CardFormView,
            cardElementAnalyticsRequestExecutorFactory = factory,
        )

        second.restoreState(bundle)

        second.reportShown(context)
        second.reportInteraction(context)
        second.reportFormCompleted(context)

        executor.expectNoEvents()
    }

    private fun test(
        widgetType: CardElementWidgetType = CardElementWidgetType.CardFormView,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val executor = FakeCardElementAnalyticsRequestExecutor()
        val analytics = DefaultCardElementAnalytics(
            widgetType = widgetType,
            cardElementAnalyticsRequestExecutorFactory = FakeCardElementAnalyticsRequestExecutorFactory(
                executor = executor,
            ),
        )

        Scenario(
            context = context,
            executor = executor,
            analytics = analytics,
        ).block()

        executor.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val context: Context,
        val executor: FakeCardElementAnalyticsRequestExecutor,
        val analytics: CardElementAnalytics,
    )

    private class FakeCardElementAnalyticsRequestExecutor : CardElementAnalyticsRequestExecutor {
        private val reportCalls = Turbine<ReportCall>()

        suspend fun awaitReportCall(): ReportCall = reportCalls.awaitItem()

        fun ensureAllEventsConsumed() {
            reportCalls.ensureAllEventsConsumed()
        }

        fun expectNoEvents() {
            reportCalls.expectNoEvents()
        }

        override fun report(
            event: PaymentAnalyticsEvent,
            params: Map<String, String>,
        ) {
            reportCalls.add(ReportCall(event = event, params = params))
        }

        data class ReportCall(
            val event: PaymentAnalyticsEvent,
            val params: Map<String, String>,
        )
    }

    private class FakeCardElementAnalyticsRequestExecutorFactory(
        private val executor: FakeCardElementAnalyticsRequestExecutor,
    ) : CardElementAnalyticsRequestExecutor.Factory {
        override fun create(context: Context): CardElementAnalyticsRequestExecutor {
            return executor
        }
    }
}
