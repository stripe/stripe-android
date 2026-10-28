package com.stripe.android.payments.core.analytics

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.ContextUtils.packageInfo
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CheckoutConfigurationErrorReporterTest {
    @Test
    fun `reports missing common configuration as an unexpected error`() = runScenario {
        errorReporter.report(
            errorEvent = ErrorReporter.UnexpectedErrorEvent.CHECKOUT_COMMON_CONFIGURATION_IS_NULL,
        )

        assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
            .isEqualTo("unexpected_error.checkout.common_configuration.is_null")
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val errorReporter = RealErrorReporter(
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = AnalyticsRequestFactory(
                packageManager = application.packageManager,
                packageInfo = application.packageInfo,
                packageName = application.packageName.orEmpty(),
                publishableKeyProvider = { ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY },
                networkTypeProvider = { null },
            ),
        )

        Scenario(
            errorReporter = errorReporter,
            analyticsRequestExecutor = analyticsRequestExecutor,
        ).block()

        analyticsRequestExecutor.requests.ensureAllEventsConsumed()
    }

    private class Scenario(
        val errorReporter: RealErrorReporter,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
    )

    private class FakeAnalyticsRequestExecutor : AnalyticsRequestExecutor {
        val requests = Turbine<AnalyticsRequest>()

        override fun executeAsync(request: AnalyticsRequest) {
            requests.add(request)
        }
    }
}
