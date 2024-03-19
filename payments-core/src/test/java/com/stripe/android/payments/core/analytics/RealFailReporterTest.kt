package com.stripe.android.payments.core.analytics

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RealFailReporterTest {
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
        val expectedStatusCode = exception.statusCode.toString()

        realErrorReporter.report(ErrorReporter.ErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE, exception)

        val executedAnalyticsRequests = analyticsRequestExecutor.getExecutedRequests()
        assertThat(executedAnalyticsRequests.size).isEqualTo(1)
        val analyticsRequestParams = executedAnalyticsRequests.get(0).params
        assertThat(analyticsRequestParams.get("analyticsValue")).isEqualTo(expectedAnalyticsValue)
        assertThat(analyticsRequestParams.get("statusCode")).isEqualTo(expectedStatusCode)
    }
}
