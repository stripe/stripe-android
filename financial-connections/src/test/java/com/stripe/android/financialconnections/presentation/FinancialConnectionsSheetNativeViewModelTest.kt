package com.stripe.android.financialconnections.presentation

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.financialConnectionsSessionNoAccounts
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.domain.CompleteFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.CreateInstantDebitsResult
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete.EarlyTerminationCause
import com.stripe.android.financialconnections.exception.CustomManualEntryRequiredError
import com.stripe.android.financialconnections.exception.UnclassifiedError
import com.stripe.android.financialconnections.financialConnectionsSessionWithNoMoreAccounts
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Canceled
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.launcher.InstantDebitsResult
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSession.StatusDetails
import com.stripe.android.financialconnections.model.FinancialConnectionsSession.StatusDetails.Cancelled
import com.stripe.android.financialconnections.model.FinancialConnectionsSession.StatusDetails.Cancelled.Reason
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.Finish
import com.stripe.android.financialconnections.ui.theme.Theme
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
internal class FinancialConnectionsSheetNativeViewModelTest {

    @get:Rule
    val rule: TestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()
    private val completeFinancialConnectionsSession = mock<CompleteFinancialConnectionsSession>()
    private val applicationId = "com.sample.applicationid"
    private val configuration = FinancialConnectionsSheet.Configuration(
        financialConnectionsSessionClientSecret = ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )
    private val encodedPaymentMethod = "{\"id\": \"pm_123\"}"

    private val liveEvents = mutableListOf<FinancialConnectionsEvent>()

    @Before
    fun setup() {
        FinancialConnections.setEventListener { liveEvents += it }
    }

    @Test
    fun `nativeAuthFlowCoordinator - when manual entry termination, finish with CustomManualEntryRequiredError`() =
        runTest {
            val sessionWithCustomManualEntry = financialConnectionsSessionNoAccounts().copy(
                statusDetails = StatusDetails(
                    cancelled = Cancelled(
                        reason = Reason.CUSTOM_MANUAL_ENTRY
                    )
                )
            )

            whenever(completeFinancialConnectionsSession(anyOrNull(), anyOrNull())).thenReturn(
                CompleteFinancialConnectionsSession.Result(
                    session = sessionWithCustomManualEntry,
                    status = "canceled",
                )
            )

            val viewModel = createViewModel()

            nativeAuthFlowCoordinator().emit(Complete(EarlyTerminationCause.USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY))

            withState(viewModel) {
                require(it.viewEffect is Finish)
                require(it.viewEffect.result is Failed)
                assertThat(it.viewEffect.result.error).isInstanceOf(CustomManualEntryRequiredError::class.java)
            }
        }

    @Test
    fun `onCloseClick - when closing but linked accounts, finish with success`() = runTest {
        val sessionWithAccounts = financialConnectionsSessionWithNoMoreAccounts

        whenever(completeFinancialConnectionsSession(anyOrNull(), anyOrNull())).thenReturn(
            CompleteFinancialConnectionsSession.Result(
                session = sessionWithAccounts,
                status = "completed",
            )
        )

        val viewModel = createViewModel()

        nativeAuthFlowCoordinator().emit(Complete(null))

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
        whenever(completeFinancialConnectionsSession(anyOrNull(), anyOrNull())).thenReturn(
            CompleteFinancialConnectionsSession.Result(
                session = sessionWithNoAccounts,
                status = "canceled",
            )
        )

        val viewModel = createViewModel()

        nativeAuthFlowCoordinator().emit(Complete(null))

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
        whenever(completeFinancialConnectionsSession(anyOrNull(), anyOrNull())).thenReturn(
            CompleteFinancialConnectionsSession.Result(
                session = sessionWithNoAccounts,
                status = "custom_manual_entry",
            )
        )

        val viewModel = createViewModel()

        nativeAuthFlowCoordinator().emit(Complete(null))

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
        val viewModel = createViewModel()
        val intent = intent("stripe://auth-redirect/$applicationId?status=success")
        viewModel.handleOnNewIntent(intent)

        withState(viewModel) {
            assertThat(it.webAuthFlow).isEqualTo(WebAuthFlowState.Success(intent.data!!.toString()))
        }
    }

    @Test
    fun `handleOnNewIntent - when deeplink with error code received, webAuthFlow async fails`() {
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
        val viewModel = createViewModel()
        val intent = intent("stripe://auth-redirect/other-app-id?code=success")
        viewModel.handleOnNewIntent(intent)

        withState(viewModel) {
            val webAuthFlow = it.webAuthFlow
            assertIs<WebAuthFlowState.Canceled>(webAuthFlow)
        }
    }

    @Test
    fun `Returns Instant Debits Result when completing an Instant Debits flow`() = runTest {
        val session = financialConnectionsSessionWithNoMoreAccounts.copy(
            paymentAccount = FinancialConnectionsAccount(
                id = "account_001",
                supportedPaymentMethodTypes = emptyList(),
                created = 123,
                livemode = false,
                institutionName = "Stripe Bank",
            )
        )

        whenever(completeFinancialConnectionsSession(anyOrNull(), anyOrNull())).thenReturn(
            CompleteFinancialConnectionsSession.Result(
                session = session,
                status = "completed",
            )
        )

        val initialState = FinancialConnectionsSheetNativeState(
            webAuthFlow = WebAuthFlowState.Uninitialized,
            firstInit = true,
            configuration = configuration,
            reducedBranding = false,
            testMode = true,
            viewEffect = null,
            completed = false,
            initialPane = FinancialConnectionsSessionManifest.Pane.CONSENT,
            theme = Theme.LinkLight,
            isLinkWithStripe = true,
            manualEntryUsesMicrodeposits = false,
            elementsSessionContext = null,
        )

        val viewModel = createViewModel(
            initialState = initialState,
            createInstantDebitsResult = {
                InstantDebitsResult(
                    encodedPaymentMethod = encodedPaymentMethod,
                    last4 = "4242",
                    bankName = "Stripe Bank",
                    eligibleForIncentive = false,
                )
            },
        )

        nativeAuthFlowCoordinator().emit(Complete())

        val expectedViewEffect = Finish(
            result = Completed(
                instantDebits = InstantDebitsResult(
                    encodedPaymentMethod = encodedPaymentMethod,
                    last4 = "4242",
                    bankName = "Stripe Bank",
                    eligibleForIncentive = false,
                ),
            )
        )

        val state = viewModel.stateFlow.value
        assertThat(state.viewEffect).isEqualTo(expectedViewEffect)
    }

    @Test
    fun `Returns a failed result on invalid state when creating PaymentMethod in Instant Debits flow`() = runTest {
        val session = financialConnectionsSessionWithNoMoreAccounts.copy(
            paymentAccount = null,
        )

        whenever(completeFinancialConnectionsSession(anyOrNull(), anyOrNull())).thenReturn(
            CompleteFinancialConnectionsSession.Result(
                session = session,
                status = "completed",
            )
        )

        val initialState = FinancialConnectionsSheetNativeState(
            webAuthFlow = WebAuthFlowState.Uninitialized,
            firstInit = true,
            configuration = configuration,
            reducedBranding = false,
            testMode = true,
            viewEffect = null,
            completed = false,
            initialPane = FinancialConnectionsSessionManifest.Pane.CONSENT,
            theme = Theme.LinkLight,
            isLinkWithStripe = true,
            manualEntryUsesMicrodeposits = false,
            elementsSessionContext = null,
        )

        val viewModel = createViewModel(
            initialState = initialState,
            createInstantDebitsResult = {
                InstantDebitsResult(
                    encodedPaymentMethod = encodedPaymentMethod,
                    last4 = "4242",
                    bankName = "Stripe Bank",
                    eligibleForIncentive = false,
                )
            },
        )

        nativeAuthFlowCoordinator().emit(Complete())

        val state = viewModel.stateFlow.value
        val finishViewEffect = state.viewEffect as? Finish
        val failedResult = finishViewEffect?.result as? Failed
        assertThat(failedResult?.error).isInstanceOf(UnclassifiedError::class.java)
    }

    @Test
    fun `Returns a failed result when creating a PaymentMethod in Instant Debits flow fails`() = runTest {
        val session = financialConnectionsSessionWithNoMoreAccounts.copy(
            paymentAccount = FinancialConnectionsAccount(
                id = "account_001",
                supportedPaymentMethodTypes = emptyList(),
                created = 123,
                livemode = false,
                institutionName = "Stripe Bank",
            ),
        )

        whenever(completeFinancialConnectionsSession(anyOrNull(), anyOrNull())).thenReturn(
            CompleteFinancialConnectionsSession.Result(
                session = session,
                status = "completed",
            )
        )

        val initialState = FinancialConnectionsSheetNativeState(
            webAuthFlow = WebAuthFlowState.Uninitialized,
            firstInit = true,
            configuration = configuration,
            reducedBranding = false,
            testMode = true,
            viewEffect = null,
            completed = false,
            initialPane = FinancialConnectionsSessionManifest.Pane.CONSENT,
            theme = Theme.LinkLight,
            isLinkWithStripe = true,
            manualEntryUsesMicrodeposits = false,
            elementsSessionContext = null,
        )

        val viewModel = createViewModel(
            initialState = initialState,
            createInstantDebitsResult = {
                error("Something went wrong here")
            },
        )

        nativeAuthFlowCoordinator().emit(Complete())

        val state = viewModel.stateFlow.value
        val finishViewEffect = state.viewEffect as? Finish
        val failedResult = finishViewEffect?.result as? Failed
        assertThat(failedResult?.error?.message).isEqualTo("Something went wrong here")
    }

    @Test
    fun `Returns correct result when manual entry does not use microdeposits`() = runTest {
        val session = financialConnectionsSessionWithNoMoreAccounts.copy(
            paymentAccount = BankAccount(
                id = "id_1234",
                last4 = "4242",
            ),
        )

        whenever(completeFinancialConnectionsSession(anyOrNull(), anyOrNull())).thenReturn(
            CompleteFinancialConnectionsSession.Result(
                session = session,
                status = "completed",
            )
        )

        val initialState = FinancialConnectionsSheetNativeState(
            webAuthFlow = WebAuthFlowState.Uninitialized,
            firstInit = true,
            configuration = configuration,
            reducedBranding = false,
            testMode = true,
            viewEffect = null,
            completed = false,
            initialPane = FinancialConnectionsSessionManifest.Pane.CONSENT,
            theme = Theme.DefaultLight,
            isLinkWithStripe = false,
            manualEntryUsesMicrodeposits = false,
            elementsSessionContext = null,
        )

        val viewModel = createViewModel(initialState)

        viewModel.stateFlow.test {
            val state = awaitItem()
            assertThat(state.viewEffect).isNull()

            nativeAuthFlowCoordinator().emit(Complete())

            val result = (awaitItem().viewEffect as Finish).result as Completed
            val bankAccount = result.financialConnectionsSession?.paymentAccount as BankAccount
            assertThat(bankAccount.usesMicrodeposits).isFalse()
        }
    }

    @After
    fun tearDown() {
        liveEvents.clear()
    }

    private fun intent(url: String): Intent = Intent().apply { data = Uri.parse(url) }

    private fun createViewModel(
        initialState: FinancialConnectionsSheetNativeState = FinancialConnectionsSheetNativeState(
            args = FinancialConnectionsSheetNativeActivityArgs(
                configuration = configuration,
                initialSyncResponse = ApiKeyFixtures.syncResponse(),
            ),
            savedState = null
        ),
        createInstantDebitsResult: CreateInstantDebitsResult = CreateInstantDebitsResult {
            error("Unexpected call to create InstantDebitsResult")
        },
    ) = FinancialConnectionsSheetNativeViewModel(
        eventTracker = mock(),
        activityRetainedComponent = mock(),
        applicationId = applicationId,
        uriUtils = UriUtils(Logger.noop(), mock()),
        completeFinancialConnectionsSession = completeFinancialConnectionsSession,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        logger = mock(),
        navigationManager = TestNavigationManager(),
        savedStateHandle = SavedStateHandle(),
        initialState = initialState,
        createInstantDebitsResult = createInstantDebitsResult,
    )
}
