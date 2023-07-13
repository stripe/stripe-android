package com.stripe.android.financialconnections.features.linkaccountpicker

import com.airbnb.mvrx.test.MavericksTestRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSession
import com.stripe.android.financialconnections.ApiKeyFixtures.institution
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccount
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccountList
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.FetchNetworkedAccounts
import com.stripe.android.financialconnections.domain.GetCachedConsumerSession
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.SelectNetworkedAccount
import com.stripe.android.financialconnections.domain.UpdateCachedAccounts
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount.Status
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.utils.TestNavigationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class LinkAccountPickerViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule()

    private val getManifest = mock<GetManifest>()
    private val navigationManager = TestNavigationManager()
    private val getCachedConsumerSession = mock<GetCachedConsumerSession>()
    private val fetchNetworkedAccounts = mock<FetchNetworkedAccounts>()
    private val updateLocalManifest = mock<UpdateLocalManifest>()
    private val updateCachedAccounts = mock<UpdateCachedAccounts>()
    private val selectNetworkedAccount = mock<SelectNetworkedAccount>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()

    private fun buildViewModel(
        state: LinkAccountPickerState
    ) = LinkAccountPickerViewModel(
        getManifest = getManifest,
        logger = Logger.noop(),
        eventTracker = eventTracker,
        getCachedConsumerSession = getCachedConsumerSession,
        fetchNetworkedAccounts = fetchNetworkedAccounts,
        selectNetworkedAccount = selectNetworkedAccount,
        updateLocalManifest = updateLocalManifest,
        updateCachedAccounts = updateCachedAccounts,
        navigationManager = navigationManager,
        initialState = state
    )

    @Test
    fun `init - fetches existing accounts`() = runTest {
        val accounts = twoAccounts()
        whenever(getManifest()).thenReturn(sessionManifest())
        whenever(getCachedConsumerSession()).thenReturn(consumerSession())
        whenever(fetchNetworkedAccounts(any())).thenReturn(accounts)

        val viewModel = buildViewModel(LinkAccountPickerState())

        assertThat(viewModel.awaitState().payload()!!.accounts)
            .isEqualTo(accounts.data)
    }

    @Test
    fun `init - non active accounts are rendered as disabled`() = runTest {
        whenever(getManifest()).thenReturn(sessionManifest())
        whenever(getCachedConsumerSession()).thenReturn(consumerSession())
        whenever(fetchNetworkedAccounts(any())).thenReturn(
            partnerAccountList().copy(
                count = 3,
                data = listOf(
                    partnerAccount().copy(_allowSelection = null, status = Status.DISCONNECTED),
                    partnerAccount().copy(_allowSelection = null, status = Status.ACTIVE),
                    partnerAccount().copy(_allowSelection = null, status = Status.DISCONNECTED)
                )
            )
        )

        val viewModel = buildViewModel(LinkAccountPickerState())

        // accounts are sorted by status, so the first account should be active
        // allow selection should be true for active accounts, false for others
        assertThat(viewModel.awaitState().payload()!!.accounts)
            .isEqualTo(
                listOf(
                    partnerAccount().copy(_allowSelection = true, status = Status.ACTIVE),
                    partnerAccount().copy(_allowSelection = false, status = Status.DISCONNECTED),
                    partnerAccount().copy(_allowSelection = false, status = Status.DISCONNECTED)
                )
            )
    }

    @Test
    fun `onNewBankAccountClick - navigates to Institution picker`() = runTest {
        val accounts = twoAccounts()
        whenever(getManifest()).thenReturn(sessionManifest())
        whenever(getCachedConsumerSession()).thenReturn(consumerSession())
        whenever(fetchNetworkedAccounts(any())).thenReturn(accounts)

        val viewModel = buildViewModel(LinkAccountPickerState())
        viewModel.onNewBankAccountClick()

        navigationManager.assertNavigatedTo(NavigationDirections.institutionPicker)
    }

    @Test
    fun `onSelectAccountClick - if valid account updates local accounts and navigates`() = runTest {
        val accounts = twoAccounts()
        val selectedAccount = accounts.data.first()
        whenever(getManifest()).thenReturn(sessionManifest())
        whenever(getCachedConsumerSession()).thenReturn(consumerSession())
        whenever(fetchNetworkedAccounts(any())).thenReturn(accounts)
        whenever(
            selectNetworkedAccount(
                consumerSessionClientSecret = any(),
                selectedAccountId = any()
            )
        ).thenReturn(InstitutionResponse(showManualEntry = false, listOf(institution())))

        val viewModel = buildViewModel(LinkAccountPickerState())

        viewModel.onAccountClick(selectedAccount)
        viewModel.onSelectAccountClick()

        with(argumentCaptor<(List<PartnerAccount>?) -> List<PartnerAccount>?>()) {
            verify(updateCachedAccounts).invoke(capture())
            assertThat(firstValue(null)).isEqualTo(listOf(selectedAccount))
        }
        navigationManager.assertNavigatedTo(NavigationDirections.success)
    }

    @Test
    fun `onSelectAccountClick - if step up required, navigates to it`() = runTest {
        val accounts = twoAccounts()
        val selectedAccount = accounts.data.first()
        whenever(getManifest()).thenReturn(sessionManifest().copy(stepUpAuthenticationRequired = true))
        whenever(getCachedConsumerSession()).thenReturn(consumerSession())
        whenever(fetchNetworkedAccounts(any())).thenReturn(accounts)
        whenever(
            selectNetworkedAccount(
                consumerSessionClientSecret = any(),
                selectedAccountId = any()
            )
        ).thenReturn(InstitutionResponse(showManualEntry = false, listOf(institution())))

        val viewModel = buildViewModel(LinkAccountPickerState())

        viewModel.onAccountClick(selectedAccount)
        viewModel.onSelectAccountClick()

        verifyNoInteractions(updateCachedAccounts)
        verifyNoInteractions(updateLocalManifest)
        verifyNoInteractions(selectNetworkedAccount)
        navigationManager.assertNavigatedTo(NavigationDirections.linkStepUpVerification)
    }

    private fun twoAccounts() = partnerAccountList().copy(
        count = 2,
        data = listOf(partnerAccount(), partnerAccount())
    )
}
