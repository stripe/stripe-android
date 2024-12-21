package com.stripe.android.financialconnections.features.linkstepupverification

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.cachedConsumerSession
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.MarkLinkStepUpVerified
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.SelectNetworkedAccounts
import com.stripe.android.financialconnections.domain.StartVerification
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.ShareNetworkedAccountsResponse
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.utils.TestNavigationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class LinkStepUpVerificationViewModelTest {
    @get:Rule
    val testRule = CoroutineTestRule()

    private val getOrFetchSync = mock<GetOrFetchSync>()
    private val navigationManager = TestNavigationManager()
    private val confirmVerification = mock<ConfirmVerification>()
    private val getCachedAccounts = mock<GetCachedAccounts>()
    private val selectNetworkedAccounts = mock<SelectNetworkedAccounts>()
    private val markLinkVerified = mock<MarkLinkStepUpVerified>()
    private val startVerification = mock<StartVerification>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()

    private val cachedConsumerSession = cachedConsumerSession()

    private fun buildViewModel(
        state: LinkStepUpVerificationState = LinkStepUpVerificationState()
    ): LinkStepUpVerificationViewModel {
        return LinkStepUpVerificationViewModel(
            getOrFetchSync = getOrFetchSync,
            navigationManager = navigationManager,
            eventTracker = eventTracker,
            confirmVerification = confirmVerification,
            markLinkStepUpVerified = markLinkVerified,
            getCachedAccounts = getCachedAccounts,
            selectNetworkedAccounts = selectNetworkedAccounts,
            startVerification = startVerification,
            consumerSessionProvider = { cachedConsumerSession },
            logger = Logger.noop(),
            initialState = state,
            nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        )
    }

    @Test
    fun `init - starts EMAIL verification with existing consumer session secret`() = runTest {
        val syncResponse = syncResponse()
        givenGetSyncReturns(syncResponse)

        buildViewModel()

        verify(startVerification).email(
            consumerSessionClientSecret = eq(cachedConsumerSession.clientSecret),
            businessName = eq(syncResponse.manifest.businessName)
        )
    }

    @Test
    fun `otpEntered - on valid OTP confirms, verifies, selects account and navigates to success`() = runTest {
        val syncResponse = syncResponse()
        val consumerSession = ApiKeyFixtures.consumerSession()
        val linkVerifiedManifest = sessionManifest().copy(nextPane = Pane.INSTITUTION_PICKER)
        val selectedAccount = ApiKeyFixtures.cachedPartnerAccount()

        // verify succeeds
        givenGetSyncReturns(syncResponse)
        whenever(startVerification.email(any(), any())).thenReturn(consumerSession)

        // cached accounts are available.
        whenever(getCachedAccounts()).thenReturn(listOf(selectedAccount))
        whenever(
            selectNetworkedAccounts.invoke(
                consumerSessionClientSecret = consumerSession.clientSecret,
                selectedAccountIds = setOf(selectedAccount.id),
                consentAcquired = null
            )
        ).thenReturn(
            ShareNetworkedAccountsResponse(
                nextPane = null,
                display = null
            )
        )
        // link succeeds
        markLinkVerifiedReturns(linkVerifiedManifest)

        val viewModel = buildViewModel()

        verify(startVerification).email(
            consumerSessionClientSecret = eq(cachedConsumerSession.clientSecret),
            businessName = eq(syncResponse.manifest.businessName)
        )

        val otpController = viewModel.stateFlow.value.payload()!!.otpElement.controller

        // enters valid OTP
        for (i in 0 until otpController.otpLength) {
            otpController.onValueChanged(i, "1")
        }

        verify(confirmVerification).email(any(), eq("111111"))
        verify(markLinkVerified).invoke()
        verify(selectNetworkedAccounts).invoke(
            consumerSessionClientSecret = consumerSession.clientSecret,
            selectedAccountIds = setOf(selectedAccount.id),
            consentAcquired = null
        )
        navigationManager.assertNavigatedTo(
            Destination.Success,
            pane = Pane.LINK_STEP_UP_VERIFICATION
        )
    }

    private suspend fun givenGetSyncReturns(
        syncResponse: SynchronizeSessionResponse = syncResponse()
    ) {
        whenever(getOrFetchSync(any(), anyOrNull())).thenReturn(syncResponse)
    }

    private suspend fun markLinkVerifiedReturns(
        manifest: FinancialConnectionsSessionManifest
    ) {
        whenever(markLinkVerified()).thenReturn(manifest)
    }
}
