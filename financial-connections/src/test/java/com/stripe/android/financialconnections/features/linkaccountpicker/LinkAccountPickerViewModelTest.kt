package com.stripe.android.financialconnections.features.linkaccountpicker

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.institution
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccount
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
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
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.DataAccessNoticeBody
import com.stripe.android.financialconnections.model.Display
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.model.NetworkedAccount
import com.stripe.android.financialconnections.model.NetworkedAccountsList
import com.stripe.android.financialconnections.model.ReturningNetworkingUserAccountPicker
import com.stripe.android.financialconnections.model.TextUpdate
import com.stripe.android.financialconnections.navigation.Destination.LinkStepUpVerification
import com.stripe.android.financialconnections.navigation.PopUpToBehavior
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.repository.CachedConsumerSession
import com.stripe.android.financialconnections.utils.TestNavigationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
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
        presentSheet = mock(),
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
                    LinkedAccount(
                        account = partnerAccount().copy(id = "id1", _allowSelection = null),
                        display = NetworkedAccount(id = "id1", allowSelection = true)
                    ),
                    LinkedAccount(
                        account = partnerAccount().copy(id = "id2", _allowSelection = null),
                        display = NetworkedAccount(id = "id2", allowSelection = false)
                    ),
                    LinkedAccount(
                        account = partnerAccount().copy(id = "id3", _allowSelection = null),
                        display = NetworkedAccount(id = "id3", allowSelection = false)
                    )
                )
            )
    }

    @Test
    fun `init - Redirects to nextPaneOnNewAccount if no bank accounts`() = runTest {
        whenever(getSync()).thenReturn(syncResponse())
        whenever(getCachedConsumerSession()).thenReturn(consumerSession())

        whenever(fetchNetworkedAccounts(any())).thenReturn(
            NetworkedAccountsList(
                data = emptyList(),
                display = display(emptyList()),
                nextPaneOnAddAccount = Pane.LINK_LOGIN, // Random pane to test it
            )
        )

        buildViewModel(LinkAccountPickerState())

        navigationManager.assertNavigatedTo(
            destination = Pane.LINK_LOGIN.destination,
            pane = Pane.LINK_ACCOUNT_PICKER,
            popUpTo = PopUpToBehavior.Route(
                route = Pane.CONSENT.destination.fullRoute,
                inclusive = true,
            ),
        )
    }

    @Test
    fun `init - Redirects to institution picker if no bank accounts and no nextPaneOnAddAccount`() = runTest {
        whenever(getSync()).thenReturn(syncResponse())
        whenever(getCachedConsumerSession()).thenReturn(consumerSession())

        whenever(fetchNetworkedAccounts(any())).thenReturn(
            NetworkedAccountsList(
                data = emptyList(),
                display = display(emptyList()),
                nextPaneOnAddAccount = null,
            )
        )

        buildViewModel(LinkAccountPickerState())

        navigationManager.assertNavigatedTo(
            destination = Pane.INSTITUTION_PICKER.destination,
            pane = Pane.LINK_ACCOUNT_PICKER,
            popUpTo = PopUpToBehavior.Route(
                route = Pane.CONSENT.destination.fullRoute,
                inclusive = true,
            ),
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
                consentAcquired = any()
            )
        ).thenReturn(InstitutionResponse(showManualEntry = false, listOf(institution())))

        val viewModel = buildViewModel(LinkAccountPickerState())

        viewModel.onAccountClick(selectedAccount)
        viewModel.onSelectAccountsClick()

        verify(updateCachedAccounts).invoke(listOf(selectedAccount))
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
                    consentAcquired = any()
                )
            ).thenReturn(InstitutionResponse(showManualEntry = false, listOf(institution())))

            val viewModel = buildViewModel(LinkAccountPickerState())

            viewModel.onAccountClick(selectedAccount)
            viewModel.onSelectAccountsClick()

            verify(updateCachedAccounts).invoke(listOf(selectedAccount))
            verifyNoInteractions(selectNetworkedAccounts)
            navigationManager.assertNavigatedTo(LinkStepUpVerification, Pane.LINK_ACCOUNT_PICKER)
        }

    @Test
    fun `init - first selectable account is pre-selected`() = runTest {
        // Given a list of networked accounts with mixed selection permissions
        val accountsData = listOf(
            partnerAccount().copy(id = "id1", _allowSelection = null),
            partnerAccount().copy(id = "id2", _allowSelection = null),
            partnerAccount().copy(id = "id3", _allowSelection = null)
        )
        val displayAccounts = listOf(
            NetworkedAccount(id = "id1", allowSelection = false),
            NetworkedAccount(id = "id2", allowSelection = false),
            NetworkedAccount(id = "id3", allowSelection = true)
        )
        whenever(getSync()).thenReturn(syncResponse())
        whenever(getCachedConsumerSession()).thenReturn(consumerSession())
        whenever(fetchNetworkedAccounts(any())).thenReturn(
            NetworkedAccountsList(
                data = accountsData,
                display = display(displayAccounts)
            )
        )

        // When initializing the ViewModel
        val viewModel = buildViewModel(LinkAccountPickerState())

        val expectedPreselectedAccountId = "id3"
        val actualPreselectedAccountId = viewModel.stateFlow.value.payload()!!.selectedAccountIds.firstOrNull()

        assertThat(actualPreselectedAccountId).isEqualTo(expectedPreselectedAccountId)
    }

    @Test
    fun `onAccountClick - uses data access notice for multiple selected account types`() = runTest {
        val accountsData = listOf(
            partnerAccount().copy(id = "type1_1", _allowSelection = true),
            partnerAccount().copy(id = "type2_1", _allowSelection = true)
        )
        val displayAccounts = listOf(
            NetworkedAccount(id = "type1_1", allowSelection = true),
            NetworkedAccount(id = "type2_1", allowSelection = true)
        )
        val expectedDataAccessNotice = DataAccessNotice(
            title = "Generic Notice",
            body = DataAccessNoticeBody(bullets = listOf()),
            cta = "Generic CTA"
        )
        whenever(getSync()).thenReturn(
            syncResponse().copy(manifest = sessionManifest().copy(singleAccount = false))
        )
        whenever(getCachedConsumerSession()).thenReturn(consumerSession())
        whenever(fetchNetworkedAccounts(any())).thenReturn(
            NetworkedAccountsList(
                data = accountsData,
                display = display(displayAccounts, expectedDataAccessNotice)
            )
        )

        // When selecting an additional account of a different type (there's a preselection)
        val viewModel = buildViewModel(LinkAccountPickerState())
        viewModel.onAccountClick(partnerAccount().copy(id = "type2_1", _allowSelection = true))

        // generic data access notice for multiple selected accounts
        val actualDataAccessNotice = viewModel.stateFlow.value.activeDataAccessNotice

        assertThat(actualDataAccessNotice).isEqualTo(expectedDataAccessNotice)
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

    private fun display(
        networkedAccounts: List<NetworkedAccount> = emptyList(),
        multipleAccountTypesSelectedDataAccessNotice: DataAccessNotice? = null
    ) = Display(
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
                ),
                multipleAccountTypesSelectedDataAccessNotice = multipleAccountTypesSelectedDataAccessNotice
            )
        )
    )

    private fun consumerSession(): CachedConsumerSession {
        return CachedConsumerSession(
            clientSecret = "clientSecret",
            emailAddress = "test@test.com",
            phoneNumber = "(***) *** **12",
            isVerified = false,
            publishableKey = null
        )
    }
}
