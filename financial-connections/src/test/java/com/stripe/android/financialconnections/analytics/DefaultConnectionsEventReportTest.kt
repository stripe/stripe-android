package com.stripe.android.financialconnections.analytics

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.model.LinkAccountSession
import com.stripe.android.financialconnections.model.LinkedAccountList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class DefaultConnectionsEventReportTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val analyticsRequestExecutor = mock<AnalyticsRequestExecutor>()
    private val analyticsRequestFactory = AnalyticsRequestFactory(
        packageManager = application.packageManager,
        packageName = application.packageName.orEmpty(),
        packageInfo = application.packageManager.getPackageInfo(application.packageName, 0),
        publishableKeyProvider = { ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY }
    )

    private val eventReporter = DefaultFinancialFinancialConnectionsEventReporter(
        analyticsRequestExecutor,
        analyticsRequestFactory,
        testDispatcher
    )

    private val configuration = FinancialConnectionsSheet.Configuration(
        ApiKeyFixtures.DEFAULT_LINK_ACCOUNT_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )

    private val linkAccountSession = LinkAccountSession(
        "las_1234567890",
        ApiKeyFixtures.DEFAULT_LINK_ACCOUNT_SESSION_SECRET,
        LinkedAccountList(
            linkedAccounts = emptyList(),
            hasMore = false,
            url = "url",
            count = 0
        ),
        true
    )

    @Test
    fun `onPresented() should fire analytics request with expected event value`() {
        eventReporter.onPresented(configuration)
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "stripe_android.connections.sheet.presented" &&
                    req.params["las_client_secret"] == ApiKeyFixtures.DEFAULT_LINK_ACCOUNT_SESSION_SECRET
            }
        )
    }

    @Test
    fun `onResult() should fire analytics request with expected event value for success`() {
        eventReporter.onResult(
            configuration,
            FinancialConnectionsSheetResult.Completed(linkAccountSession)
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "stripe_android.connections.sheet.closed" &&
                    req.params["session_result"] == "completed" &&
                    req.params["las_client_secret"] == ApiKeyFixtures.DEFAULT_LINK_ACCOUNT_SESSION_SECRET
            }
        )
    }

    @Test
    fun `onResult() should fire analytics request with expected event value for cancelled`() {
        eventReporter.onResult(configuration, FinancialConnectionsSheetResult.Canceled)
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "stripe_android.connections.sheet.closed" &&
                    req.params["session_result"] == "cancelled" &&
                    req.params["las_client_secret"] == ApiKeyFixtures.DEFAULT_LINK_ACCOUNT_SESSION_SECRET
            }
        )
    }

    @Test
    fun `onResult() should fire analytics request with expected event value for failure`() {
        eventReporter.onResult(configuration, FinancialConnectionsSheetResult.Failed(Exception()))
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "stripe_android.connections.sheet.failed" &&
                    req.params["session_result"] == "failure" &&
                    req.params["las_client_secret"] == ApiKeyFixtures.DEFAULT_LINK_ACCOUNT_SESSION_SECRET
            }
        )
    }
}
