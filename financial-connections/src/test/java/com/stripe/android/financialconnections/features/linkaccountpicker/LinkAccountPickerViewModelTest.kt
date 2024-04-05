package com.stripe.android.financialconnections.features.linkaccountpicker

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSession
import com.stripe.android.financialconnections.ApiKeyFixtures.institution
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccount
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.FetchNetworkedAccounts
import com.stripe.android.financialconnections.domain.GetCachedConsumerSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.SelectNetworkedAccounts
import com.stripe.android.financialconnections.domain.UpdateCachedAccounts
import com.stripe.android.financialconnections.model.AddNewAccount
import com.stripe.android.financialconnections.model.Display
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.model.NetworkedAccount
import com.stripe.android.financialconnections.model.NetworkedAccountsList
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.model.ReturningNetworkingUserAccountPicker
import com.stripe.android.financialconnections.model.TextUpdate
import com.stripe.android.financialconnections.navigation.Destination.LinkStepUpVerification
import com.stripe.android.financialconnections.navigation.destination
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
    val testRule = CoroutineTestRule()

    private val getSync = mock<GetOrFetchSync>()
    private val navigationManager = TestNavigationManager()
    private val getCachedConsumerSession = mock<GetCachedConsumerSession>()
    private val fetchNetworkedAccounts = mock<FetchNetworkedAccounts>()
    private val updateCachedAccounts = mock<UpdateCachedAccounts>()
    private val selectNetworkedAccounts = mock<SelectNetworkedAccounts>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()

    private fun buildViewModel(
        state: LinkAccountPickerState
    ) = LinkAccountPickerViewModel(
        navigationManager = navigationManager,
        getSync = getSync,
        logger = Logger.noop(),
        eventTracker = eventTracker,
        getCachedConsumerSession = getCachedConsumerSession,
        fetchNetworkedAccounts = fetchNetworkedAccounts,
        selectNetworkedAccounts = selectNetworkedAccounts,
        updateCachedAccounts = updateCachedAccounts,
        initialState = state,
        handleClickableUrl = mock(),
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        presentNoticeSheet = mock(),
        presentUpdateRequiredSheet = mock(),
    )

    @Test
    fun `init - Fetches existing accounts and zips them by id`() = runTest {
        whenever(getSync()).thenReturn(syncResponse())
        whenever(getCachedConsumerSession()).thenReturn(consumerSession())
        whenever(fetchNetworkedAccounts(any())).thenReturn(
            NetworkedAccountsList(
                data = listOf(
                    partnerAccount().copy(id = "id1", _allowSelection = null),
                    partnerAccount().copy(id = "id2", _allowSelection = null),
                    partnerAccount().copy(id = "id3", _allowSelection = null)
                ),
                display = display(
                    listOf(
                        NetworkedAccount(id = "id1", allowSelection = true),
                        NetworkedAccount(id = "id2", allowSelection = false),
                        NetworkedAccount(id = "id3", allowSelection = false),
                    )
                )
            )
        )

        val viewModel = buildViewModel(LinkAccountPickerState())

        assertThat(viewModel.stateFlow.value.payload()!!.accounts)
            .isEqualTo(
                listOf(
                    partnerAccount().copy(id = "id1", _allowSelection = null) to
                        NetworkedAccount(id = "id1", allowSelection = true),
                    partnerAccount().copy(id = "id2", _allowSelection = null) to
                        NetworkedAccount(id = "id2", allowSelection = false),
                    partnerAccount().copy(id = "id3", _allowSelection = null) to
                        NetworkedAccount(id = "id3", allowSelection = false)
                )
            )
    }

    @Test
    fun `onNewBankAccountClick - navigates to AddNewAccount#NextPane`() = runTest {
        val response = twoAccounts().copy(
            nextPaneOnAddAccount = Pane.INSTITUTION_PICKER
        )
        whenever(getSync()).thenReturn(syncResponse())
        whenever(getCachedConsumerSession()).thenReturn(consumerSession())
        whenever(fetchNetworkedAccounts(any())).thenReturn(response)

        val viewModel = buildViewModel(LinkAccountPickerState())
        viewModel.onNewBankAccountClick()

        val destination = response.nextPaneOnAddAccount!!.destination
        navigationManager.assertNavigatedTo(destination, Pane.LINK_ACCOUNT_PICKER)
    }

    @Test
    fun `onSelectAccountClick - if valid account updates local accounts and navigates`() = runTest {
        val accounts = NetworkedAccountsList(
            data = listOf(
                partnerAccount().copy(
                    id = "id1",
                    nextPaneOnSelection = Pane.ATTACH_LINKED_PAYMENT_ACCOUNT
                )
            ),
            display = display(
                listOf(
                    NetworkedAccount(
                        id = "id1",
                        allowSelection = true
                    ),
                )
            )
        )
        val selectedAccount = accounts.data.first()
        whenever(getSync()).thenReturn(syncResponse())
        whenever(getCachedConsumerSession()).thenReturn(consumerSession())
        whenever(fetchNetworkedAccounts(any())).thenReturn(accounts)
        whenever(
            selectNetworkedAccounts(
                consumerSessionClientSecret = any(),
                selectedAccountIds = any(),
            )
        ).thenReturn(InstitutionResponse(showManualEntry = false, listOf(institution())))

        val viewModel = buildViewModel(LinkAccountPickerState())

        viewModel.onAccountClick(selectedAccount)
        viewModel.onSelectAccountsClick()

        with(argumentCaptor<(List<PartnerAccount>?) -> List<PartnerAccount>?>()) {
            verify(updateCachedAccounts).invoke(capture())
            assertThat(firstValue(null)).isEqualTo(listOf(selectedAccount))
        }
        val destination = accounts.data.first().nextPaneOnSelection!!.destination
        navigationManager.assertNavigatedTo(destination, Pane.LINK_ACCOUNT_PICKER)
    }

    @Test
    fun `onSelectAccountClick - if next pane is step up, caches accounts and navigates`() =
        runTest {
            val accounts = NetworkedAccountsList(
                data = listOf(
                    partnerAccount().copy(
                        id = "id1",
                        nextPaneOnSelection = Pane.LINK_STEP_UP_VERIFICATION
                    )
                ),
                display = display(
                    listOf(
                        NetworkedAccount(
                            id = "id1",
                            allowSelection = true
                        ),
                    )
                )
            )
            val selectedAccount = accounts.data.first()
            whenever(getSync()).thenReturn(syncResponse())
            whenever(getCachedConsumerSession()).thenReturn(consumerSession())
            whenever(fetchNetworkedAccounts(any())).thenReturn(accounts)
            whenever(
                selectNetworkedAccounts(
                    consumerSessionClientSecret = any(),
                    selectedAccountIds = any(),
                )
            ).thenReturn(InstitutionResponse(showManualEntry = false, listOf(institution())))

            val viewModel = buildViewModel(LinkAccountPickerState())

            viewModel.onAccountClick(selectedAccount)
            viewModel.onSelectAccountsClick()

            with(argumentCaptor<(List<PartnerAccount>?) -> List<PartnerAccount>?>()) {
                verify(updateCachedAccounts).invoke(capture())
                assertThat(firstValue(null)).isEqualTo(listOf(selectedAccount))
            }
            verifyNoInteractions(selectNetworkedAccounts)
            navigationManager.assertNavigatedTo(LinkStepUpVerification, Pane.LINK_ACCOUNT_PICKER)
        }

    private fun twoAccounts() = NetworkedAccountsList(
        nextPaneOnAddAccount = null,
        data = listOf(
            partnerAccount().copy(
                id = "id1",
            ),
            partnerAccount().copy(
                id = "id2"
            )
        ),
        display = display(
            listOf(
                NetworkedAccount(
                    id = "id1",
                    allowSelection = true,
                ),
                NetworkedAccount(
                    id = "id2",
                    allowSelection = true,
                )
            )
        )
    )

    private fun display(networkedAccounts: List<NetworkedAccount> = emptyList()) = Display(
        text = TextUpdate(
            returningNetworkingUserAccountPicker = ReturningNetworkingUserAccountPicker(
                title = "Select account",
                defaultCta = "Connect account",
                accounts = networkedAccounts,
                addNewAccount = AddNewAccount(
                    body = "New bank account",
                    icon = Image(
                        default = "https://b.stripecdn.com/connections-statics-srv/assets/SailIcon--add-purple-3x.png"
                    ),
                )
            )
        )
    )
}
