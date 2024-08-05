package com.stripe.android.financialconnections.features.networkinglinksignup

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ConsentAgree.analyticsValue
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.LINK_LOGIN
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.NETWORKING_LINK_SIGNUP_PANE
import com.stripe.android.financialconnections.model.LinkLoginPane
import com.stripe.android.financialconnections.model.NetworkingLinkSignupBody
import com.stripe.android.financialconnections.model.NetworkingLinkSignupPane
import com.stripe.android.financialconnections.model.TextUpdate
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
        signUpToLink = mock(),
        attachConsumerToLinkAccountSession = mock(),
    )

    @Test
    fun `init - creates controllers with prefilled account holder email`() = runTest {
        val manifest = ApiKeyFixtures.sessionManifest().copy(
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
        val manifest = ApiKeyFixtures.sessionManifest().copy(
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
        val manifest = ApiKeyFixtures.sessionManifest().copy(
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
        val manifest = ApiKeyFixtures.sessionManifest()

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
        val manifest = ApiKeyFixtures.sessionManifest().copy(
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
        val manifest = ApiKeyFixtures.sessionManifest()

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
        val manifest = ApiKeyFixtures.sessionManifest().copy(
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
        val manifest = ApiKeyFixtures.sessionManifest().copy(
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
