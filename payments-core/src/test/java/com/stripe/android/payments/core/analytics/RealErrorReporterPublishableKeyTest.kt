package com.stripe.android.payments.core.analytics

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.ApiRequest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
internal class RealErrorReporterPublishableKeyTest {
    @Test
    fun `uses current configuration when no override is supplied`() = runScenario {
        reporter.report(ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE)
        configurationCalls.awaitItem()

        assertThat(executor.requests.awaitItem().params)
            .containsEntry("publishable_key", "pk_test_configured")
    }

    @Test
    fun `reports without a key when configuration is unavailable`() = runScenario(
        configurationProvider = { error("Configuration is not available yet") },
    ) {
        reporter.report(ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE)
        configurationCalls.awaitItem()

        assertThat(executor.requests.awaitItem().params)
            .containsEntry("publishable_key", ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY)
    }

    @Test
    fun `explicit key does not resolve unavailable configuration`() = runScenario(
        configurationProvider = { error("Must not resolve configuration") },
    ) {
        reporter.report(
            errorEvent = ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE,
            publishableKeyOverride = "pk_test_operation",
        )

        assertThat(executor.requests.awaitItem().params)
            .containsEntry("publishable_key", "pk_test_operation")
        configurationCalls.expectNoEvents()
    }

    @Test
    fun `redacts user keys supplied by configuration`() = runScenario(
        configurationProvider = { ApiConfiguration.State("uk_secret", null) },
    ) {
        reporter.report(ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE)
        configurationCalls.awaitItem()

        assertThat(executor.requests.awaitItem().params)
            .containsEntry("publishable_key", "[REDACTED_LIVE_KEY]")
    }

    private fun runScenario(
        configurationProvider: Provider<ApiConfiguration.State> = Provider {
            ApiConfiguration.State("pk_test_configured", null)
        },
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val executor = FakeAnalyticsRequestExecutor()
        val factory = AnalyticsRequestFactory(
            packageManager = null,
            packageInfo = null,
            packageName = "test",
            networkTypeProvider = { null },
            pluginTypeProvider = { null },
        )
        val configurationCalls = Turbine<Unit>()
        val reporter = RealErrorReporter(executor, factory) {
            configurationCalls.add(Unit)
            configurationProvider.get()
        }

        Scenario(reporter, executor, configurationCalls).block()

        executor.requests.ensureAllEventsConsumed()
        configurationCalls.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val reporter: RealErrorReporter,
        val executor: FakeAnalyticsRequestExecutor,
        val configurationCalls: Turbine<Unit>,
    )

    internal class FakeAnalyticsRequestExecutor : AnalyticsRequestExecutor {
        val requests = Turbine<AnalyticsRequest>()

        override fun executeAsync(request: AnalyticsRequest) {
            requests.add(request)
        }
    }
}
