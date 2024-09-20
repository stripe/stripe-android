package com.stripe.android.financialconnections.features.networkinglinksignup

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.financialconnections.ApiKeyFixtures.cachedPartnerAccounts
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSessionLookup
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSessionSignup
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ConsentAgree.analyticsValue
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.SignUpToLink
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.LINK_LOGIN
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.NETWORKING_LINK_SIGNUP_PANE
import com.stripe.android.financialconnections.model.LinkLoginPane
import com.stripe.android.financialconnections.model.NetworkingLinkSignupBody
import com.stripe.android.financialconnections.model.NetworkingLinkSignupPane
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.model.TextUpdate
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.Destination.NetworkingLinkVerification
import com.stripe.android.financialconnections.navigation.Destination.NetworkingSaveToLinkVerification
import com.stripe.android.financialconnections.navigation.NavigationIntent
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.NavigationManagerImpl
import com.stripe.android.financialconnections.utils.GetOrFetchSync
import com.stripe.android.financialconnections.utils.SaveAccountToLink
import com.stripe.android.financialconnections.utils.TestHandleError
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkingLinkSignupViewModelTest {

    @get:Rule
    val testRule = CoroutineTestRule()

    private fun buildViewModel(
        state: NetworkingLinkSignupState,
        getOrFetchSync: GetOrFetchSync = GetOrFetchSync { syncResponseForNetworking() },
        lookupAccount: LookupAccount = LookupAccount { consumerSessionLookup(exists = true) },
        eventTracker: FinancialConnectionsAnalyticsTracker = TestFinancialConnectionsAnalyticsTracker(),
        nativeAuthFlowCoordinator: NativeAuthFlowCoordinator = NativeAuthFlowCoordinator(),
        signupHandler: LinkSignupHandler = mockLinkSignupHandlerForNetworking(),
    ) = NetworkingLinkSignupViewModel(
        getOrFetchSync = getOrFetchSync,
        logger = Logger.noop(),
        eventTracker = eventTracker,
        initialState = state,
        lookupAccount = lookupAccount,
        uriUtils = UriUtils(Logger.noop(), eventTracker),
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        presentSheet = { _, _ -> },
        linkSignupHandler = signupHandler,
    )

    @Test
    fun `init - creates controllers with prefilled account holder email`() = runTest {
        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(),
            getOrFetchSync = GetOrFetchSync { syncResponseForNetworking(email = "test@test.com") },
            lookupAccount = { consumerSessionLookup(exists = false) },
        )

        val state = viewModel.stateFlow.value
        val payload = requireNotNull(state.payload())
        assertThat(payload.emailController.fieldValue.value).isEqualTo("test@test.com")
    }

    @Test
    fun `init - focuses email field if no email provided in Instant Debits flow`() = runTest {
        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = true),
            getOrFetchSync = GetOrFetchSync { syncResponseForInstantDebits() },
        )

        val state = viewModel.stateFlow.value
        val payload = requireNotNull(state.payload())
        assertThat(payload.focusEmailField).isTrue()
    }

    @Test
    fun `init - does not focus email field if no email provided in Financial Connections flow`() = runTest {
        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = false),
            getOrFetchSync = GetOrFetchSync { syncResponseForNetworking() },
        )

        val state = viewModel.stateFlow.value
        val payload = requireNotNull(state.payload())
        assertThat(payload.focusEmailField).isFalse()
    }

    @Test
    fun `Redirects to save-to-link verification screen if entering returning user email`() = runTest {
        val navigationManager = NavigationManagerImpl()

        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(),
            getOrFetchSync = GetOrFetchSync { syncResponseForNetworking() },
            lookupAccount = { consumerSessionLookup(exists = true) },
            signupHandler = mockLinkSignupHandlerForNetworking(
                navigationManager = navigationManager,
            ),
        )

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
        val navigationManager = NavigationManagerImpl()

        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = true),
            getOrFetchSync = GetOrFetchSync { syncResponseForInstantDebits() },
            lookupAccount = { consumerSessionLookup(exists = true) },
            signupHandler = mockLinkSignupHandlerForInstantDebits(
                navigationManager = navigationManager,
                // We don't expect a signup to be performed
                failOnSignup = true,
            ),
        )

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
        val navigationManager = NavigationManagerImpl()

        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(),
            getOrFetchSync = GetOrFetchSync { syncResponseForNetworking() },
            lookupAccount = { consumerSessionLookup(exists = true) },
            signupHandler = mockLinkSignupHandlerForNetworking(
                navigationManager = navigationManager,
                // We don't expect a signup to be performed
                failOnSignup = true,
            )
        )

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
        val eventTracker = TestFinancialConnectionsAnalyticsTracker()

        buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = false),
            eventTracker = eventTracker,
        )

        eventTracker.assertContainsEvent(
            expectedEventName = "linked_accounts.pane.loaded",
            expectedParams = mapOf(
                "pane" to NETWORKING_LINK_SIGNUP_PANE.analyticsValue,
            ),
        )
    }

    @Test
    fun `Reports correct pane in analytics events in Instant Debits flow`() = runTest {
        val eventTracker = TestFinancialConnectionsAnalyticsTracker()

        buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = true),
            eventTracker = eventTracker,
        )

        eventTracker.assertContainsEvent(
            expectedEventName = "linked_accounts.pane.loaded",
            expectedParams = mapOf(
                "pane" to LINK_LOGIN.analyticsValue,
            ),
        )
    }

    @Test
    fun `Navigates to correct pane after signing up in Networking flow`() = runTest {
        val navigationManager = NavigationManagerImpl()

        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = false),
            getOrFetchSync = GetOrFetchSync { syncResponseForNetworking() },
            lookupAccount = { consumerSessionLookup(exists = false) },
            signupHandler = mockLinkSignupHandlerForNetworking(
                navigationManager = navigationManager,
            ),
        )

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
        val navigationManager = NavigationManagerImpl()

        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = true),
            getOrFetchSync = GetOrFetchSync { syncResponseForInstantDebits() },
            lookupAccount = { consumerSessionLookup(exists = false) },
            signupHandler = mockLinkSignupHandlerForInstantDebits(
                navigationManager = navigationManager,
                nextPaneAfterSignup = Pane.INSTITUTION_PICKER,
            ),
        )

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
        val navigationManager = NavigationManagerImpl()

        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = false),
            getOrFetchSync = GetOrFetchSync { syncResponseForNetworking() },
            lookupAccount = { consumerSessionLookup(exists = false) },
            signupHandler = mockLinkSignupHandlerForNetworking(
                navigationManager = navigationManager,
                failOnSignup = true,
            ),
        )

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
        val navigationManager = NavigationManagerImpl()

        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = true),
            getOrFetchSync = GetOrFetchSync { syncResponseForInstantDebits() },
            lookupAccount = { consumerSessionLookup(exists = false) },
            signupHandler = mockLinkSignupHandlerForInstantDebits(
                navigationManager = navigationManager,
                failOnSignup = true,
            ),
        )

        navigationManager.navigationFlow.test {
            val state = viewModel.stateFlow.value

            val payload = requireNotNull(state.payload())
            payload.emailController.onValueChange("email@email.com")
            payload.phoneController.onValueChange("+15555555555")

            viewModel.onSaveAccount()
            expectNoEvents()
        }
    }

    private fun mockLinkSignupHandlerForNetworking(
        navigationManager: NavigationManager = NavigationManagerImpl(),
        failOnSignup: Boolean = false,
    ): LinkSignupHandler {
        val manifest = sessionManifest().copy(
            businessName = "Business",
            accountholderCustomerEmailAddress = "test@test.com"
        )

        val saveAccountToLink = SaveAccountToLink {
            if (failOnSignup) {
                throw APIConnectionException()
            } else {
                manifest
            }
        }

        return LinkSignupHandlerForNetworking(
            getOrFetchSync = GetOrFetchSync { syncResponseForNetworking() },
            getCachedAccounts = { cachedPartnerAccounts() },
            saveAccountToLink = saveAccountToLink,
            eventTracker = TestFinancialConnectionsAnalyticsTracker(),
            navigationManager = navigationManager,
            logger = Logger.noop(),
        )
    }

    private fun mockLinkSignupHandlerForInstantDebits(
        navigationManager: NavigationManager = NavigationManagerImpl(),
        handleError: HandleError = TestHandleError(),
        failOnSignup: Boolean = false,
        nextPaneAfterSignup: Pane = Pane.CONSENT,
    ): LinkSignupHandler {
        val signUpToLink = SignUpToLink { _, _, _ ->
            if (failOnSignup) {
                throw APIConnectionException()
            } else {
                consumerSessionSignup()
            }
        }

        return LinkSignupHandlerForInstantDebits(
            signUpToLink = signUpToLink,
            getOrFetchSync = GetOrFetchSync {
                syncResponseForInstantDebits(nextPane = nextPaneAfterSignup)
            },
            attachConsumerToLinkAccountSession = {
                // Mock a successful attach
            },
            eventTracker = TestFinancialConnectionsAnalyticsTracker(),
            navigationManager = navigationManager,
            handleError = handleError,
            logger = Logger.noop(),
        )
    }

    private companion object Defaults {

        fun syncResponseForNetworking(
            email: String? = null,
        ): SynchronizeSessionResponse {
            return syncResponse().copy(
                manifest = sessionManifest().copy(
                    businessName = "Business",
                    accountholderCustomerEmailAddress = email,
                ),
                text = TextUpdate(
                    consent = null,
                    networkingLinkSignupPane = NetworkingLinkSignupPane(
                        aboveCta = "Above CTA",
                        body = NetworkingLinkSignupBody(emptyList()),
                        cta = "CTA",
                        skipCta = "Skip CTA",
                        title = "Title"
                    ),
                ),
            )
        }

        fun syncResponseForInstantDebits(
            email: String? = null,
            nextPane: Pane = Pane.CONSENT,
        ): SynchronizeSessionResponse {
            return syncResponse().copy(
                manifest = sessionManifest().copy(
                    businessName = "Business",
                    accountholderCustomerEmailAddress = email,
                    isLinkWithStripe = true,
                    nextPane = nextPane,
                ),
                text = TextUpdate(
                    consent = null,
                    linkLoginPane = LinkLoginPane(
                        title = "Title",
                        body = "Body",
                        aboveCta = "Above CTA",
                        cta = "CTA",
                    ),
                ),
            )
        }
    }
}
