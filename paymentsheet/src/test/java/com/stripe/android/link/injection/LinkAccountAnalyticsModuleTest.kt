package com.stripe.android.link.injection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.link.TestFactory
import com.stripe.android.link.analytics.LinkEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkAccountAnalyticsModuleTest {
    @Test
    fun `account reporter supplies Link configuration key to shared factory`() = runTest {
        val executor = FakeAnalyticsRequestExecutor()
        val errorReporter = FakeErrorReporter()
        val factory = PaymentAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext<Context>(),
            defaultProductUsageTokens = setOf("PaymentSheet"),
        )
        val reporter = LinkAccountAnalyticsModule.provideLinkEventsReporter(
            analyticsRequestExecutor = executor,
            paymentAnalyticsRequestFactory = factory,
            errorReporter = errorReporter,
            workContext = coroutineContext,
            logger = Logger.noop(),
            durationProvider = DefaultDurationProvider.instance,
            configuration = TestFactory.LINK_CONFIGURATION,
        )

        reporter.onAccountLookupComplete()

        val request = executor.requests.awaitItem()
        assertThat(request.params).containsEntry("event", LinkEvent.AccountLookupComplete.eventName)
        assertThat(request.params).containsEntry(
            "publishable_key",
            TestFactory.LINK_CONFIGURATION.apiConfiguration.publishableKey,
        )
        assertThat(request.params).containsEntry("product_usage", "PaymentSheet")
        executor.requests.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
    }

    internal class FakeAnalyticsRequestExecutor : AnalyticsRequestExecutor {
        val requests = Turbine<AnalyticsRequest>()

        override fun executeAsync(request: AnalyticsRequest) {
            requests.add(request)
        }
    }
}
