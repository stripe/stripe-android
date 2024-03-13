package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

import com.airbnb.mvrx.test.MavericksTestRule
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.DisableNetworking
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.PopUpToBehavior
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.utils.TestHandleError
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
    private val handleError = TestHandleError()
    private val disableNetworking = mock<DisableNetworking>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()

    private fun buildViewModel(
        state: NetworkingLinkLoginWarmupState
    ) = NetworkingLinkLoginWarmupViewModel(
        navigationManager = navigationManager,
        getManifest = getManifest,
        handleError = handleError,
        disableNetworking = disableNetworking,
        eventTracker = eventTracker,
        initialState = state
    )

    @Test
    fun `init - payload error navigates to error screen`() = runTest {
        val error = RuntimeException("Failed to fetch manifest")
        whenever(getManifest()).thenAnswer { throw error }

        buildViewModel(NetworkingLinkLoginWarmupState())

        handleError.assertError(
            extraMessage = "Error fetching payload",
            pane = Pane.NETWORKING_LINK_LOGIN_WARMUP,
            error = error,
            displayErrorScreen = true
        )
    }

    @Test
    fun `onContinueClick - navigates to verification pane`() = runTest {
        val viewModel = buildViewModel(NetworkingLinkLoginWarmupState())

        viewModel.onContinueClick()
        navigationManager.assertNavigatedTo(
            destination = Destination.NetworkingLinkVerification,
            pane = Pane.NETWORKING_LINK_LOGIN_WARMUP
        )
    }

    @Test
    fun `onSkipClicked - navigates to institution picker and clears back stack`() = runTest {
        val referrer = Pane.CONSENT
        val viewModel = buildViewModel(NetworkingLinkLoginWarmupState(referrer))

        whenever(disableNetworking()).thenReturn(
            ApiKeyFixtures.sessionManifest().copy(nextPane = Pane.INSTITUTION_PICKER)
        )

        viewModel.onSkipClicked()
        navigationManager.assertNavigatedTo(
            destination = Destination.InstitutionPicker,
            popUpTo = PopUpToBehavior.Route(
                route = referrer.destination.fullRoute,
                inclusive = true,
            ),
            pane = Pane.NETWORKING_LINK_LOGIN_WARMUP,
        )
    }

    @Test
    fun `onClickableTextClick - skip_login disables networking and navigates`() = runTest {
        val viewModel = buildViewModel(NetworkingLinkLoginWarmupState())
        val expectedNextPane = Pane.INSTITUTION_PICKER

        whenever(disableNetworking()).thenReturn(
            ApiKeyFixtures.sessionManifest().copy(nextPane = expectedNextPane)
        )

        viewModel.onSkipClicked()

        verify(disableNetworking).invoke()
        navigationManager.assertNavigatedTo(
            destination = expectedNextPane.destination,
            popUpTo = PopUpToBehavior.Current(inclusive = true),
            pane = Pane.NETWORKING_LINK_LOGIN_WARMUP
        )
    }
}
