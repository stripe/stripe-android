package com.stripe.android.financialconnections.features.success

import app.cash.turbine.test
import com.airbnb.mvrx.test.MavericksTestRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.domain.CompleteFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.GetAuthorizationSessionAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Finish
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.navigation.NavigationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
internal class SuccessViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mvrxRule = MavericksTestRule(testDispatcher = testDispatcher)

    private val getManifest = mock<GetManifest>()
    private val navigationManager = mock<NavigationManager>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = mock<NativeAuthFlowCoordinator>()
    private val getAuthorizationSessionAccounts = mock<GetAuthorizationSessionAccounts>()
    private val completeFinancialConnectionsSession = mock<CompleteFinancialConnectionsSession>()

    private fun buildViewModel(
        state: SuccessState
    ) = SuccessViewModel(
        getManifest = getManifest,
        navigationManager = navigationManager,
        logger = Logger.noop(),
        eventTracker = eventTracker,
        initialState = state,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        getAuthorizationSessionAccounts = getAuthorizationSessionAccounts,
        completeFinancialConnectionsSession = completeFinancialConnectionsSession
    )

    @Test
    fun `init - when skipSuccessPane is true, complete session and emit Finish`() = runTest {
        val session = ApiKeyFixtures.financialConnectionsSessionNoAccounts()
        val accounts = ApiKeyFixtures.partnerAccountList()
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            skipSuccessPane = true,
            activeAuthSession = ApiKeyFixtures.authorizationSession(),
            activeInstitution = ApiKeyFixtures.institution()
        )
        whenever(getAuthorizationSessionAccounts(any())).thenReturn(accounts)
        whenever(getManifest()).thenReturn(manifest)
        whenever(completeFinancialConnectionsSession()).thenReturn(session)

        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())

        nativeAuthFlowCoordinator().test {
            buildViewModel(SuccessState())
            // Just emits the complete event, no page loaded events.
            assertThat(eventTracker.sentEvents).containsExactly(
                FinancialConnectionsEvent.Complete(
                    connectedAccounts = session.accounts.count,
                    exception = null
                )
            )
            assertThat(awaitItem()).isEqualTo(Finish(Completed(financialConnectionsSession = session)))
        }
    }

    @Test
    fun `init - when skipSuccessPane is false, session is not auto completed`() = runTest {
        val session = ApiKeyFixtures.financialConnectionsSessionNoAccounts()
        val accounts = ApiKeyFixtures.partnerAccountList()
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            skipSuccessPane = false,
            activeAuthSession = ApiKeyFixtures.authorizationSession(),
            activeInstitution = ApiKeyFixtures.institution()
        )
        whenever(getAuthorizationSessionAccounts(any())).thenReturn(accounts)
        whenever(getManifest()).thenReturn(manifest)
        whenever(completeFinancialConnectionsSession()).thenReturn(session)

        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())

        nativeAuthFlowCoordinator().test {
            buildViewModel(SuccessState())
            assertThat(eventTracker.sentEvents).containsExactly(
                FinancialConnectionsEvent.PaneLoaded(
                    pane = FinancialConnectionsSessionManifest.Pane.SUCCESS,
                )
            )
            expectNoEvents()
            verifyNoInteractions(completeFinancialConnectionsSession)
        }
    }

    @Test
    fun `onDoneClick - when complete succeeds, AuthFlow finishes with success result`() = runTest {
        val session = ApiKeyFixtures.financialConnectionsSessionNoAccounts()
        whenever(completeFinancialConnectionsSession()).thenReturn(session)

        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        nativeAuthFlowCoordinator().test {
            buildViewModel(SuccessState()).onDoneClick()
            assertEquals(
                expected = Finish(Completed(financialConnectionsSession = session)),
                actual = awaitItem(),
            )
        }
    }

    @Test
    fun `onDoneClick - when complete fails, AuthFlow finishes with fail result`() = runTest {
        val error = IllegalArgumentException("Something went wrong!")
        whenever(completeFinancialConnectionsSession()).thenThrow(error)

        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        nativeAuthFlowCoordinator().test {
            buildViewModel(SuccessState()).onDoneClick()
            assertEquals(
                expected = Finish(Failed(error = error)),
                actual = awaitItem(),
            )
        }
    }
}
