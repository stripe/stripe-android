package com.stripe.android.financialconnections.presentation

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.test.MvRxTestRule
import com.airbnb.mvrx.withState
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.exception.WebAuthFlowCancelledException
import com.stripe.android.financialconnections.exception.WebAuthFlowFailedException
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
internal class FinancialConnectionsSheetNativeViewModelTest {

    @get:Rule
    val mvrxRule = MvRxTestRule(testDispatcher = UnconfinedTestDispatcher())
    private val applicationId = "com.sample.applicationid"
    private val nativeAuthFlowCoordinator = mock<NativeAuthFlowCoordinator>()


    @Test
    fun `handleOnNewIntent - when deeplink with success code received, webAuthFlow async succeeds`() {
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        val viewModel = createViewModel()
        val intent = intent("stripe://auth-redirect/$applicationId?code=success")
        viewModel.handleOnNewIntent(intent)

        withState(viewModel) {
            assertThat(it.webAuthFlow).isEqualTo(Success(intent.data!!.toString()))
        }
    }

    @Test
    fun `handleOnNewIntent - when deeplink with unknown code received, webAuthFlow async fails`() {
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        val viewModel = createViewModel()
        val intent = intent("stripe://auth-redirect/$applicationId?code=unknown")
        viewModel.handleOnNewIntent(intent)

        withState(viewModel) {
            val webAuthFlow = it.webAuthFlow
            assertIs<Fail<*>>(webAuthFlow)
            assertThat(webAuthFlow.error).isInstanceOf(WebAuthFlowFailedException::class.java)
        }
    }

    @Test
    fun `handleOnNewIntent - when deeplink with cancel code received, webAuthFlow async fails`() {
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        val viewModel = createViewModel()
        val intent = intent("stripe://auth-redirect/$applicationId?code=cancel")
        viewModel.handleOnNewIntent(intent)

        withState(viewModel) {
            val webAuthFlow = it.webAuthFlow
            assertIs<Fail<*>>(webAuthFlow)
            assertThat(webAuthFlow.error).isInstanceOf(WebAuthFlowCancelledException::class.java)
        }
    }

    @Test
    fun `handleOnNewIntent - when deeplink with unknown applicationId received, webAuthFlow async fails`() {
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        val viewModel = createViewModel()
        val intent = intent("stripe://auth-redirect/other-app-id?code=success")
        viewModel.handleOnNewIntent(intent)

        withState(viewModel) {
            val webAuthFlow = it.webAuthFlow
            assertIs<Fail<*>>(webAuthFlow)
            assertThat(webAuthFlow.error).isInstanceOf(WebAuthFlowFailedException::class.java)
        }
    }

    private fun intent(url: String): Intent = Intent().apply { data = Uri.parse(url) }

    private fun createViewModel(
        initialState: FinancialConnectionsSheetNativeState = FinancialConnectionsSheetNativeState(
            FinancialConnectionsSheetNativeActivityArgs(
                configuration = FinancialConnectionsSheet.Configuration(
                    financialConnectionsSessionClientSecret = ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
                    publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
                ),
                initialSyncResponse = ApiKeyFixtures.syncResponse(),
            )
        )
    ) = FinancialConnectionsSheetNativeViewModel(
        eventTracker = mock(),
        getManifest = mock(),
        activityRetainedComponent = mock(),
        applicationId = applicationId,
        uriUtils = UriUtils(Logger.noop()),
        completeFinancialConnectionsSession = mock(),
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        logger = mock(),
        initialState = initialState
    )

}