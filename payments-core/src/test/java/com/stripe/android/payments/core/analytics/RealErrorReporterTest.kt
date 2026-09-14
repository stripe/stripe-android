package com.stripe.android.payments.core.analytics

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.exception.CardException
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RealErrorReporterTest {
    val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val analyticsRequestFactory = AnalyticsRequestFactory(
        packageManager = application.packageManager,
        packageName = application.packageName.orEmpty(),
        packageInfo = application.packageInfo,
        networkTypeProvider = { "5G" },
    )
    val realErrorReporter: RealErrorReporter = RealErrorReporter(analyticsRequestExecutor, analyticsRequestFactory)

    @Before
    fun clearAnalyticsRequestExecutor() {
        analyticsRequestExecutor.clear()
    }

    @Test
    fun `RealErrorReporter logs correct info via analyticsRequestExecutor`() {
        val exception = StripeException.create(IllegalArgumentException("this arg isn't legal"))
        val expectedAnalyticsValue = exception.analyticsValue()

        realErrorReporter.report(
            ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE,
            exception,
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        )

        val executedAnalyticsRequests = analyticsRequestExecutor.getExecutedRequests()
        assertThat(executedAnalyticsRequests.size).isEqualTo(1)
        val analyticsRequestParams = executedAnalyticsRequests.get(0).params
        assertThat(analyticsRequestParams.get("analytics_value")).isEqualTo(expectedAnalyticsValue)
        assertThat(analyticsRequestParams.get("request_id")).isNull()
    }

    @Test
    fun `one reporter uses the key supplied for each report`() = runScenario {
        reporter.report(
            ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE,
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        )
        reporter.report(
            ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE,
            publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
        )

        assertThat(executor.requests.takeItem().params["publishable_key"])
            .isEqualTo(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        assertThat(executor.requests.takeItem().params["publishable_key"])
            .isEqualTo(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    }

    @Test
    fun `reports without credentials use undefined key`() = runScenario {
        reporter.report(
            ErrorReporter.ExpectedErrorEvent.PAYMENT_LAUNCHER_CONFIRMATION_NULL_ARGS,
            publishableKey = null,
        )

        assertThat(executor.requests.takeItem().params["publishable_key"])
            .isEqualTo(ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY)
    }

    @Test
    fun `RealErrorReporter logs requestId correctly`() {
        val expectedRequestId = "some_request_ID"
        val exception = CardException(StripeError(), requestId = expectedRequestId)
        val expectedAnalyticsValue = exception.analyticsValue()
        val expectedStatusCode = exception.statusCode.toString()

        realErrorReporter.report(
            ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE,
            exception,
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        )

        val executedAnalyticsRequests = analyticsRequestExecutor.getExecutedRequests()
        assertThat(executedAnalyticsRequests.size).isEqualTo(1)
        val analyticsRequestParams = executedAnalyticsRequests.get(0).params
        assertThat(analyticsRequestParams.get("analytics_value")).isEqualTo(expectedAnalyticsValue)
        assertThat(analyticsRequestParams.get("status_code")).isNotEqualTo(StripeException.DEFAULT_STATUS_CODE)
        assertThat(analyticsRequestParams.get("status_code")).isEqualTo(expectedStatusCode)
        assertThat(analyticsRequestParams.get("request_id")).isEqualTo(expectedRequestId)
    }

    @Test
    fun `RealErrorReporter logs StripeError information correctly`() {
        val expectedRequestId = "some_request_ID"
        val expectedErrorType = "some_error_type"
        val expectedErrorCode = "some_error_code"
        val exception = CardException(
            StripeError(type = expectedErrorType, code = expectedErrorCode),
            requestId = expectedRequestId
        )

        realErrorReporter.report(
            ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE,
            exception,
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        )

        val executedAnalyticsRequests = analyticsRequestExecutor.getExecutedRequests()
        assertThat(executedAnalyticsRequests.size).isEqualTo(1)
        val analyticsRequestParams = executedAnalyticsRequests.get(0).params
        assertThat(analyticsRequestParams.get("error_code")).isEqualTo(expectedErrorCode)
        assertThat(analyticsRequestParams.get("error_type")).isEqualTo(expectedErrorType)
        assertThat(analyticsRequestParams.get("request_id")).isEqualTo(expectedRequestId)
    }

    @Test
    fun `RealErrorReporter logs additionalNonPiiParams via analyticsRequestExecutor`() {
        val exception = StripeException.create(IllegalArgumentException("this arg isn't legal"))
        val expectedAnalyticsValue = exception.analyticsValue()

        realErrorReporter.report(
            errorEvent = ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE,
            stripeException = exception,
            additionalNonPiiParams = mapOf("foo" to "bar"),
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        )

        val executedAnalyticsRequests = analyticsRequestExecutor.getExecutedRequests()
        assertThat(executedAnalyticsRequests.size).isEqualTo(1)
        val analyticsRequestParams = executedAnalyticsRequests.get(0).params
        assertThat(analyticsRequestParams.get("analytics_value")).isEqualTo(expectedAnalyticsValue)
        assertThat(analyticsRequestParams.get("request_id")).isNull()
        assertThat(analyticsRequestParams.get("foo")).isEqualTo("bar")
    }

    @Test
    fun `RealErrorReporter logs skips exception params when exception is null via analyticsRequestExecutor`() {
        realErrorReporter.report(
            errorEvent = ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE,
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        )

        val executedAnalyticsRequests = analyticsRequestExecutor.getExecutedRequests()
        assertThat(executedAnalyticsRequests.size).isEqualTo(1)
        val analyticsRequestParams = executedAnalyticsRequests.get(0).params
        assertThat(analyticsRequestParams.get("analytics_value")).isNull()
        assertThat(analyticsRequestParams.get("status_code")).isNull()
        assertThat(analyticsRequestParams.get("request_id")).isNull()
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        val executor = FakeRequestExecutor()
        val reporter = RealErrorReporter(executor, analyticsRequestFactory)

        Scenario(reporter, executor).block()

        executor.requests.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val reporter: RealErrorReporter,
        val executor: FakeRequestExecutor,
    )

    private class FakeRequestExecutor : AnalyticsRequestExecutor {
        val requests = Turbine<AnalyticsRequest>()

        override fun executeAsync(request: AnalyticsRequest) {
            requests.add(request)
        }
    }
}
