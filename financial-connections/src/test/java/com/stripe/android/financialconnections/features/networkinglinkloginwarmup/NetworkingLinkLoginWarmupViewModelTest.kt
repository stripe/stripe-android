package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSession
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.DisableNetworking
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.PopUpToBehavior
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.utils.TestHandleError
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.model.ConsumerSessionLookup
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class NetworkingLinkLoginWarmupViewModelTest {

    @get:Rule
    val testRule = CoroutineTestRule()

    private val getOrFetchSync = mock<GetOrFetchSync>()
    private val navigationManager = TestNavigationManager()
    private val handleError = TestHandleError()
    private val disableNetworking = mock<DisableNetworking>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = mock<NativeAuthFlowCoordinator>()
    private val lookupAccount = mock<LookupAccount>()

    private fun buildViewModel(
        state: NetworkingLinkLoginWarmupState
    ) = NetworkingLinkLoginWarmupViewModel(
        navigationManager = navigationManager,
        getOrFetchSync = getOrFetchSync,
        handleError = handleError,
        disableNetworking = disableNetworking,
        eventTracker = eventTracker,
        initialState = state,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        lookupAccount = lookupAccount,
        prefillDetails = null,
    )

    @Test
    fun `init - payload error navigates to error screen`() = runTest {
        val error = RuntimeException("Failed to fetch manifest")
        whenever(getOrFetchSync(any(), anyOrNull())).thenAnswer { throw error }

        buildViewModel(NetworkingLinkLoginWarmupState())

        handleError.assertError(
            extraMessage = "Error fetching payload",
            pane = Pane.NETWORKING_LINK_LOGIN_WARMUP,
            error = error,
            displayErrorScreen = true
        )
    }

    @Test
    fun `onContinueClick - calls lookup to cache session and navigates to verification pane`() = runTest {
        whenever(getOrFetchSync(any(), anyOrNull())).thenReturn(
            syncResponse().copy(
                manifest = sessionManifest().copy(
                    accountholderCustomerEmailAddress = "test@stripe.com"
                )
            )
        )
        whenever(
            lookupAccount(
                email = anyOrNull(),
                phone = anyOrNull(),
                phoneCountryCode = anyOrNull(),
                emailSource = anyOrNull(),
                verifiedFlow = anyOrNull(),
                sessionId = anyOrNull(),
                pane = anyOrNull()
            )
        ).thenReturn(
            ConsumerSessionLookup(
                exists = true,
                errorMessage = null,
                consumerSession = consumerSession()
            )
        )

        val viewModel = buildViewModel(NetworkingLinkLoginWarmupState())
        viewModel.onContinueClick()

        verify(lookupAccount).invoke(
            email = anyOrNull(),
            phone = anyOrNull(),
            phoneCountryCode = anyOrNull(),
            emailSource = anyOrNull(),
            verifiedFlow = anyOrNull(),
            sessionId = anyOrNull(),
            pane = anyOrNull()
        )
        navigationManager.assertNavigatedTo(
            destination = Destination.NetworkingLinkVerification,
            pane = Pane.NETWORKING_LINK_LOGIN_WARMUP
        )
    }

    @Test
    fun `onSecondaryButtonClicked - navigates to institution picker and clears back stack`() = runTest {
        val referrer = Pane.CONSENT
        val viewModel = buildViewModel(NetworkingLinkLoginWarmupState(referrer))

        whenever(disableNetworking(clientSuggestedNextPaneOnDisableNetworking = null)).thenReturn(
            sessionManifest().copy(nextPane = Pane.INSTITUTION_PICKER)
        )

        viewModel.onSecondaryButtonClicked()
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
    fun `onSecondaryButtonClicked with Instant Debits - closes bottom sheet`() = runTest {
        val referrer = Pane.CONSENT
        val viewModel = buildViewModel(
            NetworkingLinkLoginWarmupState(
                referrer = referrer,
                isInstantDebits = true,
            )
        )

        whenever(disableNetworking(clientSuggestedNextPaneOnDisableNetworking = null)).thenReturn(
            sessionManifest().copy(nextPane = Pane.INSTITUTION_PICKER)
        )

        viewModel.onSecondaryButtonClicked()
        navigationManager.assertNavigatedBack()
    }

    @Test
    fun `onClickableTextClick - skip_login disables networking and navigates`() = runTest {
        val viewModel = buildViewModel(NetworkingLinkLoginWarmupState())
        val expectedNextPane = Pane.INSTITUTION_PICKER

        whenever(disableNetworking(clientSuggestedNextPaneOnDisableNetworking = null)).thenReturn(
            sessionManifest().copy(nextPane = expectedNextPane)
        )

        viewModel.onSecondaryButtonClicked()

        verify(disableNetworking).invoke(clientSuggestedNextPaneOnDisableNetworking = null)
        navigationManager.assertNavigatedTo(
            destination = expectedNextPane.destination,
            popUpTo = PopUpToBehavior.Current(inclusive = true),
            pane = Pane.NETWORKING_LINK_LOGIN_WARMUP
        )
    }
}
