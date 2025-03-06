package com.stripe.android.financialconnections.features.partnerauth

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.institution
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.domain.RepairAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.repository.CoreAuthorizationPendingNetworkingRepairRepository
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.financialconnections.utils.UriUtils
import com.stripe.android.uicore.navigation.NavigationManager
import com.stripe.android.uicore.navigation.PopUpToBehavior
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class PartnerAuthViewModelTest {

    @get:Rule
    val testRule = CoroutineTestRule()

    @Test
    fun `Creates auth session when in partner auth flow`() = runTest {
        val syncResponse = syncResponse(
            manifest = sessionManifest().copy(
                activeInstitution = institution(),
            )
        )

        val getOrFetchSync = mock<GetOrFetchSync> {
            onBlocking { invoke(any(), any()) } doReturn syncResponse
        }

        val createAuthorizationSession = mock<PostAuthorizationSession>()
        val repairAuthSession = mock<RepairAuthorizationSession>()

        makeViewModel(
            initialState = SharedPartnerAuthState(
                pane = Pane.PARTNER_AUTH,
                inModal = true,
            ),
            getOrFetchSync = getOrFetchSync,
            createAuthorizationSession = createAuthorizationSession,
            repairAuthSession = repairAuthSession,
        )

        verify(createAuthorizationSession).invoke(
            institution = syncResponse.manifest.activeInstitution!!,
            sync = syncResponse,
        )

        verify(repairAuthSession, never()).invoke(any())
    }

    @Test
    fun `Creates repair session when in networking relink flow`() = runTest {
        val syncResponse = syncResponse(
            manifest = sessionManifest().copy(
                activeInstitution = institution(),
            )
        )

        val createAuthorizationSession = mock<PostAuthorizationSession>()
        val repairAuthSession = mock<RepairAuthorizationSession>()

        val pendingRepairRepository = CoreAuthorizationPendingNetworkingRepairRepository(
            savedStateHandle = SavedStateHandle(),
            logger = Logger.noop(),
        ).apply {
            set("fcauth_123")
        }

        val getOrFetchSync = mock<GetOrFetchSync> {
            onBlocking { invoke(any(), any()) } doReturn syncResponse
        }

        makeViewModel(
            initialState = SharedPartnerAuthState(
                pane = Pane.BANK_AUTH_REPAIR,
                inModal = false,
            ),
            getOrFetchSync = getOrFetchSync,
            createAuthorizationSession = createAuthorizationSession,
            repairAuthSession = repairAuthSession,
            pendingRepairRepository = pendingRepairRepository,
        )

        verify(repairAuthSession).invoke(
            coreAuthorization = "fcauth_123",
        )

        verify(createAuthorizationSession, never()).invoke(any(), any())
    }

    @Test
    fun `Navigates back when users cancels in modal`() {
        val navigationManager = TestNavigationManager()
        val tracker = TestFinancialConnectionsAnalyticsTracker()

        val viewModel = makeViewModel(
            initialState = SharedPartnerAuthState(
                pane = Pane.PARTNER_AUTH,
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
        getOrFetchSync: GetOrFetchSync = mock(),
        createAuthorizationSession: PostAuthorizationSession = mock(),
        repairAuthSession: RepairAuthorizationSession = mock(),
        pendingRepairRepository: CoreAuthorizationPendingNetworkingRepairRepository = mock(),
    ): PartnerAuthViewModel {
        return PartnerAuthViewModel(
            completeAuthorizationSession = mock(),
            createAuthorizationSession = createAuthorizationSession,
            cancelAuthorizationSession = mock(),
            retrieveAuthorizationSession = mock(),
            eventTracker = tracker,
            applicationId = "com.app.id",
            uriUtils = UriUtils(
                logger = Logger.noop(),
                tracker = tracker,
            ),
            postAuthSessionEvent = mock(),
            getOrFetchSync = getOrFetchSync,
            browserManager = mock(),
            handleError = mock(),
            navigationManager = navigationManager,
            pollAuthorizationSessionOAuthResults = mock(),
            logger = Logger.noop(),
            presentSheet = mock(),
            initialState = initialState,
            nativeAuthFlowCoordinator = NativeAuthFlowCoordinator(),
            pendingRepairRepository = pendingRepairRepository,
            repairAuthSession = repairAuthSession,
        )
    }
}
