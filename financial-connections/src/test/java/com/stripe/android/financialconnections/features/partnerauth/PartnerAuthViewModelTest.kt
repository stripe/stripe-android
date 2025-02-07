package com.stripe.android.financialconnections.features.partnerauth

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.PopUpToBehavior
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.repository.CoreAuthorizationPendingNetworkingRepairRepository
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class PartnerAuthViewModelTest {

    @get:Rule
    val testRule = CoroutineTestRule()

    @Test
    fun `Navigates back when users cancels in modal`() {
        val navigationManager = TestNavigationManager()
        val tracker = TestFinancialConnectionsAnalyticsTracker()

        val viewModel = makeViewModel(
            initialState = SharedPartnerAuthState(
                pane = Pane.PARTNER_AUTH_DRAWER,
                payload = Async.Uninitialized,
                inModal = true,
            ),
            navigationManager = navigationManager,
            tracker = tracker,
        )

        viewModel.onCancelClick()

        navigationManager.assertNavigatedBack()
        tracker.assertContainsEvent("linked_accounts.click.prepane.cancel")
    }

    @Test
    fun `Navigates to institution picker when users cancels in full-screen pane`() {
        val navigationManager = TestNavigationManager()
        val tracker = TestFinancialConnectionsAnalyticsTracker()

        val viewModel = makeViewModel(
            initialState = SharedPartnerAuthState(
                pane = Pane.PARTNER_AUTH,
                payload = Async.Uninitialized,
                inModal = false,
            ),
            navigationManager = navigationManager,
            tracker = tracker,
        )

        viewModel.onCancelClick()

        navigationManager.assertNavigatedTo(
            destination = Destination.InstitutionPicker,
            pane = Pane.PARTNER_AUTH,
            popUpTo = PopUpToBehavior.Current(
                inclusive = true,
            ),
        )
        tracker.assertContainsEvent("linked_accounts.click.prepane.choose_another_bank")
    }

    private fun makeViewModel(
        initialState: SharedPartnerAuthState,
        navigationManager: NavigationManager = TestNavigationManager(),
        tracker: FinancialConnectionsAnalyticsTracker = TestFinancialConnectionsAnalyticsTracker(),
    ): PartnerAuthViewModel {
        return PartnerAuthViewModel(
            completeAuthorizationSession = mock(),
            createAuthorizationSession = mock(),
            cancelAuthorizationSession = mock(),
            retrieveAuthorizationSession = mock(),
            eventTracker = tracker,
            applicationId = "com.app.id",
            uriUtils = UriUtils(
                logger = Logger.noop(),
                tracker = tracker,
            ),
            postAuthSessionEvent = mock(),
            getOrFetchSync = mock(),
            browserManager = mock(),
            handleError = mock(),
            navigationManager = navigationManager,
            pollAuthorizationSessionOAuthResults = mock(),
            logger = Logger.noop(),
            presentSheet = mock(),
            initialState = initialState,
            nativeAuthFlowCoordinator = NativeAuthFlowCoordinator(),
            pendingRepairRepository = CoreAuthorizationPendingNetworkingRepairRepository(
                savedStateHandle = SavedStateHandle(),
                logger = Logger.noop(),
            ),
            repairAuthSession = mock(),
        )
    }
}
