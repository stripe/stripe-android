package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

import com.airbnb.mvrx.test.MavericksTestRule
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.DisableNetworking
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NetworkingLinkLoginWarmupViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule()

    private val getManifest = mock<GetManifest>()
    private val goNext = mock<GoNext>()
    private val disableNetworking = mock<DisableNetworking>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()

    private fun buildViewModel(
        state: NetworkingLinkLoginWarmupState
    ) = NetworkingLinkLoginWarmupViewModel(
        goNext = goNext,
        getManifest = getManifest,
        logger = Logger.noop(),
        disableNetworking = disableNetworking,
        eventTracker = eventTracker,
        initialState = state
    )

    @Test
    fun `onContinueClick - navigates to verification pane`() {
        val viewModel = buildViewModel(NetworkingLinkLoginWarmupState())

        viewModel.onContinueClick()

        verify(goNext).invoke(Pane.NETWORKING_LINK_VERIFICATION)
    }

    @Test
    fun `onClickableTextClick - skip_login disables networking and navigates`() = runTest {
        val viewModel = buildViewModel(NetworkingLinkLoginWarmupState())
        val expectedNextPane = Pane.INSTITUTION_PICKER

        whenever(disableNetworking()).thenReturn(
            ApiKeyFixtures.sessionManifest().copy(nextPane = expectedNextPane)
        )

        viewModel.onClickableTextClick("skip_login")

        verify(disableNetworking).invoke()
        verify(goNext).invoke(expectedNextPane)
    }
}
