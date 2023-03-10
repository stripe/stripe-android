package com.stripe.android.financialconnections.presentation

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.test.MavericksTestRule
import com.airbnb.mvrx.withState
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.financialConnectionsSessionNoAccounts
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.domain.CompleteFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Terminate
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Terminate.EarlyTerminationCause
import com.stripe.android.financialconnections.exception.CustomManualEntryRequiredError
import com.stripe.android.financialconnections.exception.WebAuthFlowCancelledException
import com.stripe.android.financialconnections.exception.WebAuthFlowFailedException
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.FinancialConnectionsSession.StatusDetails
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
internal class FinancialConnectionsSheetNativeViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = UnconfinedTestDispatcher())

    private val nativeAuthFlowCoordinator = mock<NativeAuthFlowCoordinator>()
    private val completeFinancialConnectionsSession = mock<CompleteFinancialConnectionsSession>()
    private val applicationId = "com.sample.applicationid"
    private val configuration = FinancialConnectionsSheet.Configuration(
        financialConnectionsSessionClientSecret = ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )

    @Test
    fun `nativeAuthFlowCoordinator - when manual entry termination, finish with CustomManualEntryRequiredError`() =
        runTest {
            val messagesFlow = MutableSharedFlow<NativeAuthFlowCoordinator.Message>()
            val sessionWithCustomManualEntry = financialConnectionsSessionNoAccounts().copy(
                statusDetails = StatusDetails(
                    cancelled = StatusDetails.Cancelled(
                        reason = StatusDetails.Cancelled.Reason.CUSTOM_MANUAL_ENTRY
                    )
                )
            )
            whenever(nativeAuthFlowCoordinator())
                .thenReturn(messagesFlow)
            whenever(completeFinancialConnectionsSession(any()))
                .thenReturn(sessionWithCustomManualEntry)

            val viewModel = createViewModel()

            messagesFlow.emit(Terminate(EarlyTerminationCause.USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY))

            withState(viewModel) {
                require(it.viewEffect is FinancialConnectionsSheetNativeViewEffect.Finish)
                require(it.viewEffect.result is FinancialConnectionsSheetActivityResult.Failed)
                assertThat(it.viewEffect.result.error).isInstanceOf(CustomManualEntryRequiredError::class.java)
            }
        }

    @Test
    fun `handleOnNewIntent - when deeplink with success code received, webAuthFlow async succeeds`() {
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        val viewModel = createViewModel()
        val intent = intent("stripe://auth-redirect/$applicationId?status=success")
        viewModel.handleOnNewIntent(intent)

        withState(viewModel) {
            assertThat(it.webAuthFlow).isEqualTo(Success(intent.data!!.toString()))
        }
    }

    @Test
    fun `handleOnNewIntent - when deeplink with error code received, webAuthFlow async fails`() {
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        val viewModel = createViewModel()
        val errorReason = "random_reason"
        val intent = intent(
            "stripe://auth-redirect/$applicationId?status=failure&error_reason=$errorReason"
        )
        viewModel.handleOnNewIntent(intent)

        withState(viewModel) {
            val webAuthFlow = it.webAuthFlow
            assertIs<Fail<*>>(webAuthFlow)
            val error = webAuthFlow.error as WebAuthFlowFailedException
            assertThat(error.reason).isEqualTo(errorReason)
        }
    }

    @Test
    fun `handleOnNewIntent - when deeplink with unknown code received, webAuthFlow async fails`() {
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        val viewModel = createViewModel()
        val intent = intent("stripe://auth-redirect/$applicationId?status=unknown")
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
        val intent = intent("stripe://auth-redirect/$applicationId?status=cancel")
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
                configuration = configuration,
                initialSyncResponse = ApiKeyFixtures.syncResponse(),
            )
        )
    ) = FinancialConnectionsSheetNativeViewModel(
        eventTracker = mock(),
        activityRetainedComponent = mock(),
        applicationId = applicationId,
        uriUtils = UriUtils(Logger.noop()),
        completeFinancialConnectionsSession = completeFinancialConnectionsSession,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        logger = mock(),
        getManifest = mock(),
        initialState = initialState
    )
}
