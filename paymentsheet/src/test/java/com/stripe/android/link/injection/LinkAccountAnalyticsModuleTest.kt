package com.stripe.android.link.injection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.link.analytics.LinkEvent
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.utils.FakeDurationProvider
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkAccountAnalyticsModuleTest {
    @Test
    fun `request factory uses Link configuration publishable key`() {
        val factory = LinkAccountAnalyticsModule.providePaymentAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext<Context>(),
            configuration = TestFactory.LINK_CONFIGURATION,
            productUsageTokens = emptySet(),
        )

        val request = factory.createRequest(
            event = LinkEvent.AccountLookupComplete,
            additionalParams = emptyMap(),
        )

        assertThat(request.params["publishable_key"])
            .isEqualTo(TestFactory.LINK_CONFIGURATION.apiConfiguration.publishableKey)
    }

    @Test
    fun `reporter for a Checkout configuration includes checkout session ID`() = runReporterScenario(
        configuration = TestFactory.LINK_CONFIGURATION.copy(
            clientAttributionMetadata = PaymentMethodMetadataFixtures.CHECKOUT_SESSION_CLIENT_ATTRIBUTION_METADATA,
        ),
    ) {
        reporter.onInlineSignupCheckboxChecked()
        testScope.testScheduler.advanceUntilIdle()

        val params = analyticsRequestExecutor.awaitEvent("link.signup.checkbox_checked")
        assertThat(params["checkout_session_id"]).isEqualTo("cs_test_123")
    }

    @Test
    fun `reporter for a non-Checkout configuration omits checkout session ID`() = runReporterScenario(
        configuration = TestFactory.LINK_CONFIGURATION,
    ) {
        reporter.onInlineSignupCheckboxChecked()
        testScope.testScheduler.advanceUntilIdle()

        val params = analyticsRequestExecutor.awaitEvent("link.signup.checkbox_checked")
        assertThat(params).doesNotContainKey("checkout_session_id")
    }

    private fun runReporterScenario(
        configuration: LinkConfiguration,
        block: suspend ReporterScenario.() -> Unit,
    ) {
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val errorReporter = FakeErrorReporter()
        val durationProvider = FakeDurationProvider()
        val testScope = TestScope()
        val reporter = LinkAccountAnalyticsModule.provideLinkEventsReporter(
            configuration = configuration,
            analyticsRequestExecutor = analyticsRequestExecutor,
            paymentAnalyticsRequestFactory = LinkAccountAnalyticsModule.providePaymentAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext<Context>(),
                configuration = configuration,
                productUsageTokens = emptySet(),
            ),
            errorReporter = errorReporter,
            workContext = testScope.coroutineContext,
            logger = Logger.noop(),
            durationProvider = durationProvider,
        )

        testScope.runTest {
            ReporterScenario(
                reporter = reporter,
                analyticsRequestExecutor = analyticsRequestExecutor,
                testScope = testScope,
            ).block()
        }

        analyticsRequestExecutor.validate()
        errorReporter.ensureAllEventsConsumed()
        durationProvider.validate()
    }

    private class ReporterScenario(
        val reporter: LinkEventsReporter,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val testScope: TestScope,
    )
}
