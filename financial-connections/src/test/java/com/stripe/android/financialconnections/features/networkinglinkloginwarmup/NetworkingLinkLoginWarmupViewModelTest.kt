package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

import com.airbnb.mvrx.test.MavericksTestRule
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.DisableNetworking
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.utils.TestNavigationManager
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
    private val navigationManager = TestNavigationManager()
    private val disableNetworking = mock<DisableNetworking>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()

    private fun buildViewModel(
        state: NetworkingLinkLoginWarmupState
    ) = NetworkingLinkLoginWarmupViewModel(
        navigationManager = navigationManager,
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
        navigationManager.assertNavigatedTo(
            destination = Destination.NetworkingLinkVerification,
            pane = Pane.NETWORKING_LINK_LOGIN_WARMUP
        )
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
        navigationManager.assertNavigatedTo(
            destination = expectedNextPane.destination,
            pane = Pane.NETWORKING_LINK_LOGIN_WARMUP
        )
    }
}
