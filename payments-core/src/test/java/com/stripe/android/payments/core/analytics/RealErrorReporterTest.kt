package com.stripe.android.payments.core.analytics

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsRequestFactory
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
        publishableKeyProvider = { ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY },
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

        realErrorReporter.report(ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE, exception)

        val executedAnalyticsRequests = analyticsRequestExecutor.getExecutedRequests()
        assertThat(executedAnalyticsRequests.size).isEqualTo(1)
        val analyticsRequestParams = executedAnalyticsRequests.get(0).params
        assertThat(analyticsRequestParams.get("analytics_value")).isEqualTo(expectedAnalyticsValue)
        assertThat(analyticsRequestParams.get("request_id")).isNull()
    }

    @Test
    fun `RealErrorReporter logs requestId correctly`() {
        val expectedRequestId = "some_request_ID"
        val exception = CardException(StripeError(), requestId = expectedRequestId)
        val expectedAnalyticsValue = exception.analyticsValue()
        val expectedStatusCode = exception.statusCode.toString()

        realErrorReporter.report(ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE, exception)

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

        realErrorReporter.report(ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE, exception)

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
            additionalNonPiiParams = mapOf("foo" to "bar")
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
        )

        val executedAnalyticsRequests = analyticsRequestExecutor.getExecutedRequests()
        assertThat(executedAnalyticsRequests.size).isEqualTo(1)
        val analyticsRequestParams = executedAnalyticsRequests.get(0).params
        assertThat(analyticsRequestParams.get("analytics_value")).isNull()
        assertThat(analyticsRequestParams.get("status_code")).isNull()
        assertThat(analyticsRequestParams.get("request_id")).isNull()
    }
}
