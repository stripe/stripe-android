package com.stripe.android.financialconnections.features.networkinglinksignup

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.financialconnections.ApiKeyFixtures.cachedPartnerAccounts
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSessionSignup
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ConsentAgree.analyticsValue
import com.stripe.android.financialconnections.domain.AttachConsumerToLinkAccountSession
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.domain.SignUpToLink
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.INSTITUTION_PICKER
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.LINK_LOGIN
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.NETWORKING_LINK_SIGNUP_PANE
import com.stripe.android.financialconnections.model.LinkLoginPane
import com.stripe.android.financialconnections.model.NetworkingLinkSignupBody
import com.stripe.android.financialconnections.model.NetworkingLinkSignupPane
import com.stripe.android.financialconnections.model.TextUpdate
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.Destination.NetworkingLinkVerification
import com.stripe.android.financialconnections.navigation.Destination.NetworkingSaveToLinkVerification
import com.stripe.android.financialconnections.navigation.NavigationIntent
import com.stripe.android.financialconnections.navigation.NavigationManagerImpl
import com.stripe.android.financialconnections.utils.UriUtils
import com.stripe.android.model.ConsumerSessionLookup
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkingLinkSignupViewModelTest {

    @get:Rule
    val testRule = CoroutineTestRule()

    private val getOrFetchSync = mock<GetOrFetchSync>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val getAuthorizationSessionAccounts = mock<GetCachedAccounts>()
    private val navigationManager = NavigationManagerImpl()
    private val lookupAccount = mock<LookupAccount>()
    private val saveAccountToLink = mock<SaveAccountToLink>()
    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()
    private val signUpToLink = mock<SignUpToLink>()
    private val attachConsumerToLinkAccountSession = mock<AttachConsumerToLinkAccountSession>()

    private fun buildViewModel(
        state: NetworkingLinkSignupState
    ) = NetworkingLinkSignupViewModel(
        getOrFetchSync = getOrFetchSync,
        logger = Logger.noop(),
        eventTracker = eventTracker,
        initialState = state,
        navigationManager = navigationManager,
        getCachedAccounts = getAuthorizationSessionAccounts,
        lookupAccount = lookupAccount,
        uriUtils = UriUtils(Logger.noop(), eventTracker),
        saveAccountToLink = saveAccountToLink,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        presentSheet = mock(),
        signUpToLink = signUpToLink,
        attachConsumerToLinkAccountSession = attachConsumerToLinkAccountSession,
    )

    @Test
    fun `init - creates controllers with prefilled account holder email`() = runTest {
        val manifest = sessionManifest().copy(
            businessName = "Business",
            accountholderCustomerEmailAddress = "test@test.com"
        )

        whenever(getOrFetchSync(any())).thenReturn(
            syncResponse().copy(
                manifest = manifest,
                text = TextUpdate(
                    consent = null,
                    networkingLinkSignupPane = networkingLinkSignupPane()
                )
            )
        )
        whenever(lookupAccount(any())).thenReturn(ConsumerSessionLookup(exists = false))

        val viewModel = buildViewModel(NetworkingLinkSignupState())
        val state = viewModel.stateFlow.value
        val payload = requireNotNull(state.payload())
        assertThat(payload.emailController.fieldValue.value).isEqualTo("test@test.com")
    }

    @Test
    fun `init - focuses email field if no email provided in Instant Debits flow`() = runTest {
        val manifest = sessionManifest().copy(
            businessName = "Business",
            accountholderCustomerEmailAddress = "",
        )

        whenever(getOrFetchSync(any())).thenReturn(
            syncResponse().copy(
                manifest = manifest.copy(isLinkWithStripe = true),
                text = TextUpdate(
                    consent = null,
                    networkingLinkSignupPane = networkingLinkSignupPane()
                )
            )
        )

        val viewModel = buildViewModel(NetworkingLinkSignupState(isInstantDebits = true))
        val state = viewModel.stateFlow.value
        val payload = requireNotNull(state.payload())
        assertThat(payload.focusEmailField).isTrue()
    }

    @Test
    fun `init - does not focus email field if no email provided in Financial Connections flow`() = runTest {
        val manifest = sessionManifest().copy(
            businessName = "Business",
            accountholderCustomerEmailAddress = "",
        )

        whenever(getOrFetchSync(any())).thenReturn(
            syncResponse().copy(
                manifest = manifest.copy(isLinkWithStripe = false),
                text = TextUpdate(
                    consent = null,
                    networkingLinkSignupPane = networkingLinkSignupPane()
                )
            )
        )

        val viewModel = buildViewModel(NetworkingLinkSignupState(isInstantDebits = false))
        val state = viewModel.stateFlow.value
        val payload = requireNotNull(state.payload())
        assertThat(payload.focusEmailField).isFalse()
    }

    @Test
    fun `Redirects to save-to-link verification screen if entering returning user email`() = runTest {
        val manifest = sessionManifest()

        whenever(getOrFetchSync(any())).thenReturn(
            syncResponse().copy(
                text = TextUpdate(
                    consent = null,
                    networkingLinkSignupPane = networkingLinkSignupPane()
                )
            )
        )
        whenever(getOrFetchSync().manifest).thenReturn(manifest)
        whenever(lookupAccount(any())).thenReturn(ConsumerSessionLookup(exists = true))

        val viewModel = buildViewModel(NetworkingLinkSignupState())

        navigationManager.navigationFlow.test {
            val state = viewModel.stateFlow.value
            val payload = requireNotNull(state.payload())
            payload.emailController.onValueChange("email@email.com")

            assertThat(awaitItem()).isEqualTo(
                NavigationIntent.NavigateTo(
                    route = NetworkingSaveToLinkVerification(referrer = NETWORKING_LINK_SIGNUP_PANE),
                    popUpTo = null,
                    isSingleTop = true,
                )
            )
        }
    }

    @Test
    fun `Redirects to verification screen if entering returning user email in Instant Debits`() = runTest {
        val manifest = sessionManifest().copy(
            isLinkWithStripe = true,
        )

        whenever(getOrFetchSync(any())).thenReturn(
            syncResponse().copy(
                manifest = manifest,
                text = TextUpdate(
                    consent = null,
                    linkLoginPane = linkLoginPane(),
                ),
            )
        )

        whenever(lookupAccount(any())).thenReturn(ConsumerSessionLookup(exists = true))

        val viewModel = buildViewModel(NetworkingLinkSignupState(isInstantDebits = true))

        navigationManager.navigationFlow.test {
            val state = viewModel.stateFlow.value
            val payload = requireNotNull(state.payload())
            payload.emailController.onValueChange("email@email.com")

            assertThat(awaitItem()).isEqualTo(
                NavigationIntent.NavigateTo(
                    route = NetworkingLinkVerification(referrer = LINK_LOGIN),
                    popUpTo = null,
                    isSingleTop = true,
                )
            )
        }
    }

    @Test
    fun `Enables Save To Link button if we encounter a returning user`() = runTest {
        val manifest = sessionManifest()

        whenever(getOrFetchSync(any())).thenReturn(
            syncResponse().copy(
                text = TextUpdate(
                    consent = null,
                    networkingLinkSignupPane = networkingLinkSignupPane()
                )
            )
        )
        whenever(getOrFetchSync().manifest).thenReturn(manifest)
        whenever(lookupAccount(any())).thenReturn(ConsumerSessionLookup(exists = true))

        val viewModel = buildViewModel(NetworkingLinkSignupState())

        navigationManager.navigationFlow.test {
            val state = viewModel.stateFlow.value
            val payload = requireNotNull(state.payload())
            payload.emailController.onValueChange("email@email.com")

            assertThat(awaitItem()).isEqualTo(
                NavigationIntent.NavigateTo(
                    route = NetworkingSaveToLinkVerification(referrer = NETWORKING_LINK_SIGNUP_PANE),
                    popUpTo = null,
                    isSingleTop = true,
                )
            )

            // Simulate the user pressing Save To Link after returning from the OTP screen
            viewModel.onSaveAccount()

            verify(saveAccountToLink, never()).new(any(), any(), any(), any(), any())

            assertThat(awaitItem()).isEqualTo(
                NavigationIntent.NavigateTo(
                    route = NetworkingSaveToLinkVerification(referrer = NETWORKING_LINK_SIGNUP_PANE),
                    popUpTo = null,
                    isSingleTop = true,
                )
            )
        }
    }

    @Test
    fun `Reports correct pane in analytics events in Financial Connections flow`() = runTest {
        val manifest = sessionManifest().copy(
            isLinkWithStripe = false,
        )

        whenever(getOrFetchSync(any())).thenReturn(
            syncResponse().copy(
                manifest = manifest,
                text = TextUpdate(
                    consent = null,
                    networkingLinkSignupPane = networkingLinkSignupPane(),
                ),
            )
        )

        buildViewModel(NetworkingLinkSignupState(isInstantDebits = false))
        eventTracker.assertContainsEvent(
            expectedEventName = "linked_accounts.pane.loaded",
            expectedParams = mapOf(
                "pane" to NETWORKING_LINK_SIGNUP_PANE.analyticsValue,
            ),
        )
    }

    @Test
    fun `Reports correct pane in analytics events in Instant Debits flow`() = runTest {
        val manifest = sessionManifest().copy(
            isLinkWithStripe = true,
        )

        whenever(getOrFetchSync(any())).thenReturn(
            syncResponse().copy(
                manifest = manifest,
                text = TextUpdate(
                    consent = null,
                    linkLoginPane = linkLoginPane(),
                ),
            )
        )

        buildViewModel(NetworkingLinkSignupState(isInstantDebits = true))
        eventTracker.assertContainsEvent(
            expectedEventName = "linked_accounts.pane.loaded",
            expectedParams = mapOf(
                "pane" to LINK_LOGIN.analyticsValue,
            ),
        )
    }

    @Test
    fun `Navigates to correct pane after signing up in Networking flow`() = runTest {
        val syncResponse = syncResponse().copy(
            manifest = sessionManifest().copy(isLinkWithStripe = false),
            text = TextUpdate(networkingLinkSignupPane = networkingLinkSignupPane()),
        )

        whenever(getOrFetchSync(any())).thenReturn(syncResponse)
        whenever(lookupAccount(any())).thenReturn(ConsumerSessionLookup(exists = false))

        val viewModel = buildViewModel(NetworkingLinkSignupState(isInstantDebits = false))

        whenever(getAuthorizationSessionAccounts()).thenReturn(cachedPartnerAccounts())
        whenever(saveAccountToLink.new(any(), any(), any(), any(), any())).thenReturn(syncResponse.manifest)

        navigationManager.navigationFlow.test {
            val state = viewModel.stateFlow.value

            val payload = requireNotNull(state.payload())
            payload.emailController.onValueChange("email@email.com")
            payload.phoneController.onValueChange("+15555555555")

            viewModel.onSaveAccount()

            assertThat(awaitItem()).isEqualTo(
                NavigationIntent.NavigateTo(
                    route = Destination.Success(referrer = NETWORKING_LINK_SIGNUP_PANE),
                    popUpTo = null,
                    isSingleTop = true,
                )
            )
        }
    }

    @Test
    fun `Navigates to correct pane after signing up in Instant Debits flow`() = runTest {
        val initialSyncResponse = syncResponse().copy(
            manifest = sessionManifest().copy(isLinkWithStripe = true),
            text = TextUpdate(linkLoginPane = linkLoginPane()),
        )

        val finalSyncResponse = syncResponse().copy(
            manifest = sessionManifest().copy(
                isLinkWithStripe = true,
                nextPane = INSTITUTION_PICKER,
            ),
            text = TextUpdate(linkLoginPane = linkLoginPane()),
        )

        whenever(getOrFetchSync(any())).thenReturn(initialSyncResponse)
        whenever(lookupAccount(any())).thenReturn(ConsumerSessionLookup(exists = false))

        val viewModel = buildViewModel(NetworkingLinkSignupState(isInstantDebits = true))

        whenever(signUpToLink(any(), any(), any())).thenReturn(consumerSessionSignup())
        whenever(attachConsumerToLinkAccountSession(any())).thenReturn(Unit)
        whenever(getOrFetchSync(any())).thenReturn(finalSyncResponse)

        navigationManager.navigationFlow.test {
            val state = viewModel.stateFlow.value

            val payload = requireNotNull(state.payload())
            payload.emailController.onValueChange("email@email.com")
            payload.phoneController.onValueChange("+15555555555")

            viewModel.onSaveAccount()

            assertThat(awaitItem()).isEqualTo(
                NavigationIntent.NavigateTo(
                    route = Destination.InstitutionPicker(referrer = LINK_LOGIN),
                    popUpTo = null,
                    isSingleTop = true,
                )
            )
        }
    }

    @Test
    fun `Navigates to success pane if failing to sign up in Networking flow`() = runTest {
        val syncResponse = syncResponse().copy(
            manifest = sessionManifest().copy(isLinkWithStripe = false),
            text = TextUpdate(networkingLinkSignupPane = networkingLinkSignupPane()),
        )

        whenever(getOrFetchSync(any())).thenReturn(syncResponse)
        whenever(lookupAccount(any())).thenReturn(ConsumerSessionLookup(exists = false))

        val viewModel = buildViewModel(NetworkingLinkSignupState(isInstantDebits = false))

        whenever(saveAccountToLink.new(any(), any(), any(), any(), any())).then {
            throw APIConnectionException()
        }

        navigationManager.navigationFlow.test {
            val state = viewModel.stateFlow.value

            val payload = requireNotNull(state.payload())
            payload.emailController.onValueChange("email@email.com")
            payload.phoneController.onValueChange("+15555555555")

            viewModel.onSaveAccount()

            assertThat(awaitItem()).isEqualTo(
                NavigationIntent.NavigateTo(
                    route = Destination.Success(referrer = NETWORKING_LINK_SIGNUP_PANE),
                    popUpTo = null,
                    isSingleTop = true,
                )
            )
        }
    }


    @Test
    fun `Does not navigate to success pane if failing to sign up in Instant Debits flow`() = runTest {
        val initialSyncResponse = syncResponse().copy(
            manifest = sessionManifest().copy(isLinkWithStripe = true),
            text = TextUpdate(linkLoginPane = linkLoginPane()),
        )

        whenever(getOrFetchSync(any())).thenReturn(initialSyncResponse)
        whenever(lookupAccount(any())).thenReturn(ConsumerSessionLookup(exists = false))

        val viewModel = buildViewModel(NetworkingLinkSignupState(isInstantDebits = true))

        whenever(signUpToLink(any(), any(), any())).then {
            throw APIConnectionException()
        }

        navigationManager.navigationFlow.test {
            val state = viewModel.stateFlow.value

            val payload = requireNotNull(state.payload())
            payload.emailController.onValueChange("email@email.com")
            payload.phoneController.onValueChange("+15555555555")

            viewModel.onSaveAccount()
            expectNoEvents()
        }
    }

    private fun networkingLinkSignupPane() = NetworkingLinkSignupPane(
        aboveCta = "Above CTA",
        body = NetworkingLinkSignupBody(emptyList()),
        cta = "CTA",
        skipCta = "Skip CTA",
        title = "Title"
    )

    private fun linkLoginPane() = LinkLoginPane(
        title = "Title",
        body = "Body",
        aboveCta = "Above CTA",
        cta = "CTA",
    )
}
