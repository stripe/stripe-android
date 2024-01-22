package com.stripe.android.financialconnections.features.success

import app.cash.turbine.test
import com.airbnb.mvrx.test.MavericksTestRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("MaxLineLength")
internal class SuccessViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mavericksRule = MavericksTestRule(testDispatcher = testDispatcher)

    private val getManifest = mock<GetManifest>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = mock<NativeAuthFlowCoordinator>()
    private val getCachedAccounts = mock<GetCachedAccounts>()

    private fun buildViewModel(
        state: SuccessState
    ) = SuccessViewModel(
        getManifest = getManifest,
        logger = Logger.noop(),
        eventTracker = eventTracker,
        initialState = state,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        saveToLinkWithStripeSucceeded = mock(),
        getCachedAccounts = getCachedAccounts,
    )

    @Test
    fun `init - when skipSuccessPane is true, complete session and emit Finish`() = runTest {
        val accounts = ApiKeyFixtures.partnerAccountList().data
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            skipSuccessPane = true,
            activeAuthSession = ApiKeyFixtures.authorizationSession(),
            activeInstitution = ApiKeyFixtures.institution()
        )
        whenever(getCachedAccounts()).thenReturn(accounts)
        whenever(getManifest()).thenReturn(manifest)

        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())

        nativeAuthFlowCoordinator().test {
            buildViewModel(SuccessState())
            // Triggers flow termination.
            assertThat(eventTracker.sentEvents).isEmpty()
            assertThat(awaitItem()).isEqualTo(Complete())
        }
    }

    @Test
    fun `init - when skipSuccessPane is false, session is not auto completed`() = runTest {
        val accounts = ApiKeyFixtures.partnerAccountList()
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            skipSuccessPane = false,
            activeAuthSession = ApiKeyFixtures.authorizationSession(),
            activeInstitution = ApiKeyFixtures.institution()
        )
        whenever(getCachedAccounts()).thenReturn(accounts.data)
        whenever(getManifest()).thenReturn(manifest)

        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())

        nativeAuthFlowCoordinator().test {
            buildViewModel(SuccessState())
            assertThat(eventTracker.sentEvents).containsExactly(
                PaneLoaded(
                    pane = Pane.SUCCESS,
                )
            )
            expectNoEvents()
        }
    }

    @Test
    fun `onDoneClick - complete session is triggered`() = runTest {
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        nativeAuthFlowCoordinator().test {
            buildViewModel(SuccessState()).onDoneClick()
            assertEquals(
                expected = Complete(),
                actual = awaitItem(),
            )
        }
    }
}
