package com.stripe.android.financialconnections.presentation

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.airbnb.mvrx.test.MavericksTestRule
import com.airbnb.mvrx.withState
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.financialConnectionsSessionNoAccounts
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.domain.CompleteFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete.EarlyTerminationCause
import com.stripe.android.financialconnections.exception.CustomManualEntryRequiredError
import com.stripe.android.financialconnections.financialConnectionsSessionWithNoMoreAccounts
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Canceled
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.FinancialConnectionsSession.StatusDetails
import com.stripe.android.financialconnections.model.FinancialConnectionsSession.StatusDetails.Cancelled
import com.stripe.android.financialconnections.model.FinancialConnectionsSession.StatusDetails.Cancelled.Reason
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.Finish
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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

    private val liveEvents = mutableListOf<FinancialConnectionsEvent>()

    @Before
    fun setup() {
        FinancialConnections.setEventListener { liveEvents += it }
    }

    @Test
    fun `nativeAuthFlowCoordinator - when manual entry termination, finish with CustomManualEntryRequiredError`() =
        runTest {
            val messagesFlow = MutableSharedFlow<NativeAuthFlowCoordinator.Message>()
            val sessionWithCustomManualEntry = financialConnectionsSessionNoAccounts().copy(
                statusDetails = StatusDetails(
                    cancelled = Cancelled(
                        reason = Reason.CUSTOM_MANUAL_ENTRY
                    )
                )
            )
            whenever(nativeAuthFlowCoordinator())
                .thenReturn(messagesFlow)
            whenever(completeFinancialConnectionsSession(any()))
                .thenReturn(sessionWithCustomManualEntry)

            val viewModel = createViewModel()

            messagesFlow.emit(Complete(EarlyTerminationCause.USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY))

            withState(viewModel) {
                require(it.viewEffect is Finish)
                require(it.viewEffect.result is Failed)
                assertThat(it.viewEffect.result.error).isInstanceOf(CustomManualEntryRequiredError::class.java)
            }
        }

    @Test
    fun `onCloseClick - when closing but linked accounts, finish with success`() = runTest {
        val sessionWithAccounts = financialConnectionsSessionWithNoMoreAccounts
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        whenever(completeFinancialConnectionsSession(anyOrNull())).thenReturn(sessionWithAccounts)

        val viewModel = createViewModel()

        viewModel.onCloseConfirm()

        withState(viewModel) {
            require(it.viewEffect is Finish)
            require(it.viewEffect.result is Completed)
        }

        // emits live event
        assertThat(liveEvents).contains(
            FinancialConnectionsEvent(
                name = Name.SUCCESS,
                metadata = Metadata(manualEntry = false)
            )
        )
    }

    @Test
    fun `onCloseClick - when closing, no accounts, no errors, finish with cancel`() = runTest {
        val sessionWithNoAccounts = financialConnectionsSessionNoAccounts()
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        whenever(completeFinancialConnectionsSession(anyOrNull())).thenReturn(sessionWithNoAccounts)

        val viewModel = createViewModel()

        viewModel.onCloseConfirm()

        withState(viewModel) {
            require(it.viewEffect is Finish)
            require(it.viewEffect.result is Canceled)
        }

        // emits live event
        assertThat(liveEvents).contains(
            FinancialConnectionsEvent(
                name = Name.CANCEL,
                metadata = Metadata()
            )
        )
    }

    @Test
    fun `onCloseClick - when closing and custom manual entry error, finish with error`() = runTest {
        val sessionWithNoAccounts = financialConnectionsSessionNoAccounts().copy(
            statusDetails = StatusDetails(
                cancelled = Cancelled(
                    reason = Reason.CUSTOM_MANUAL_ENTRY
                )
            )
        )
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        whenever(completeFinancialConnectionsSession(anyOrNull())).thenReturn(sessionWithNoAccounts)

        val viewModel = createViewModel()

        viewModel.onCloseConfirm()

        withState(viewModel) {
            require(it.viewEffect is Finish)
            require(it.viewEffect.result is Failed)
        }

        // emits live event
        assertThat(liveEvents).contains(
            FinancialConnectionsEvent(
                name = Name.MANUAL_ENTRY_INITIATED,
                metadata = Metadata()
            )
        )
    }

    @Test
    fun `handleOnNewIntent - when deeplink with success code received, webAuthFlow async succeeds`() {
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        val viewModel = createViewModel()
        val intent = intent("stripe://auth-redirect/$applicationId?status=success")
        viewModel.handleOnNewIntent(intent)

        withState(viewModel) {
            assertThat(it.webAuthFlow).isEqualTo(WebAuthFlowState.Success(intent.data!!.toString()))
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
            assertIs<WebAuthFlowState.Failed>(webAuthFlow)
            assertThat(webAuthFlow.reason).isEqualTo(errorReason)
        }
    }

    @Test
    fun `handleOnNewIntent - when app2app deeplink with error code received, webAuthFlow async fails`() {
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        val viewModel = createViewModel()
        val intent = intent(
            "stripe-auth://link-accounts/$applicationId/authentication_return#authSessionId=12345&code=failure"
        )
        viewModel.handleOnNewIntent(intent)

        withState(viewModel) {
            val webAuthFlow = it.webAuthFlow
            assertIs<WebAuthFlowState.Failed>(webAuthFlow)
        }
    }

    @Test
    fun `handleOnNewIntent - when app2app deeplink with success, webAuthFlow async suceeds`() {
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        val viewModel = createViewModel()
        val intent = intent(
            "stripe-auth://link-accounts/$applicationId/authentication_return#authSessionId=12345&code=success"
        )
        viewModel.handleOnNewIntent(intent)

        withState(viewModel) {
            val webAuthFlow = it.webAuthFlow
            assertIs<WebAuthFlowState.Success>(webAuthFlow)
        }
    }

    @Test
    fun `handleOnNewIntent - when deeplink with unknown code received, webAuthFlow async cancelled`() {
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        val viewModel = createViewModel()
        val intent = intent("stripe://auth-redirect/$applicationId?status=unknown")
        viewModel.handleOnNewIntent(intent)

        withState(viewModel) {
            val webAuthFlow = it.webAuthFlow
            assertIs<WebAuthFlowState.Canceled>(webAuthFlow)
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
            assertIs<WebAuthFlowState.Canceled>(webAuthFlow)
        }
    }

    @Test
    fun `handleOnNewIntent - when deeplink with unknown applicationId received, webAuthFlow async cancels`() {
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        val viewModel = createViewModel()
        val intent = intent("stripe://auth-redirect/other-app-id?code=success")
        viewModel.handleOnNewIntent(intent)

        withState(viewModel) {
            val webAuthFlow = it.webAuthFlow
            assertIs<WebAuthFlowState.Canceled>(webAuthFlow)
        }
    }

    @After
    fun tearDown() {
        liveEvents.clear()
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
        uriUtils = UriUtils(Logger.noop(), mock()),
        completeFinancialConnectionsSession = completeFinancialConnectionsSession,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        logger = mock(),
        getManifest = mock(),
        navigationManager = TestNavigationManager(),
        initialState = initialState
    )
}
