package com.stripe.android.financialconnections.features.networkinglinksignup

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.PermissionException
import com.stripe.android.financialconnections.ApiKeyFixtures.cachedPartnerAccounts
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSessionSignup
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ConsentAgree.analyticsValue
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
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
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.financialconnections.utils.TestHandleError
import com.stripe.android.financialconnections.utils.UriUtils
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.LinkMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkingLinkSignupViewModelTest {

    @get:Rule
    val testRule = CoroutineTestRule()

    private val getOrFetchSync = mock<GetOrFetchSync>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val navigationManager = NavigationManagerImpl()
    private val lookupAccount = mock<LookupAccount>()
    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()
    private val handleError = TestHandleError()

    private fun buildViewModel(
        state: NetworkingLinkSignupState,
        signupHandler: LinkSignupHandler = mockLinkSignupHandlerForNetworking(),
        elementsSessionContext: ElementsSessionContext? = null,
    ) = NetworkingLinkSignupViewModel(
        getOrFetchSync = getOrFetchSync,
        logger = Logger.noop(),
        eventTracker = eventTracker,
        initialState = state,
        navigationManager = navigationManager,
        lookupAccount = lookupAccount,
        uriUtils = UriUtils(Logger.noop(), eventTracker),
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        presentSheet = mock(),
        linkSignupHandler = signupHandler,
        elementsSessionContext = elementsSessionContext,
        handleError = handleError,
    )

    @Test
    fun `init - creates controllers with prefilled account holder email`() = runTest {
        val manifest = sessionManifest().copy(
            businessName = "Business",
            accountholderCustomerEmailAddress = "test@test.com"
        )

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(
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
    fun `init - creates controllers with Elements billing details`() = runTest {
        val manifest = sessionManifest()

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(
            syncResponse().copy(
                manifest = manifest,
                text = TextUpdate(
                    consent = null,
                    networkingLinkSignupPane = networkingLinkSignupPane(),
                )
            )
        )
        whenever(lookupAccount(any())).thenReturn(ConsumerSessionLookup(exists = false))

        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(),
            elementsSessionContext = ElementsSessionContext(
                amount = null,
                currency = null,
                linkMode = LinkMode.LinkPaymentMethod,
                billingDetails = null,
                prefillDetails = ElementsSessionContext.PrefillDetails(
                    email = "email@email.com",
                    phone = "5555555555",
                    phoneCountryCode = "US",
                ),
                incentiveEligibilitySession = null,
            )
        )

        val state = viewModel.stateFlow.value
        val payload = requireNotNull(state.payload())
        assertThat(payload.emailController.fieldValue.value).isEqualTo("email@email.com")
        assertThat(payload.phoneController.fieldValue.value).isEqualTo("5555555555")
        assertThat(payload.phoneController.countryDropdownController.rawFieldValue.value).isEqualTo("US")
    }

    @Test
    fun `init - focuses email field if no email provided in Instant Debits flow`() = runTest {
        val manifest = sessionManifest().copy(
            businessName = "Business",
            accountholderCustomerEmailAddress = "",
        )

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(
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

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(
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

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(
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

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(
            syncResponse().copy(
                manifest = manifest,
                text = TextUpdate(
                    consent = null,
                    linkLoginPane = linkLoginPane(),
                ),
            )
        )

        whenever(lookupAccount(any())).thenReturn(ConsumerSessionLookup(exists = true))

        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = true),
            signupHandler = mockLinkSignupHandlerForInstantDebits(
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
        val manifest = sessionManifest()

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(
            syncResponse().copy(
                text = TextUpdate(
                    consent = null,
                    networkingLinkSignupPane = networkingLinkSignupPane()
                )
            )
        )
        whenever(getOrFetchSync().manifest).thenReturn(manifest)
        whenever(lookupAccount(any())).thenReturn(ConsumerSessionLookup(exists = true))

        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(),
            signupHandler = mockLinkSignupHandlerForNetworking(
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
        val manifest = sessionManifest().copy(
            isLinkWithStripe = false,
        )

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(
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

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(
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

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(syncResponse)
        whenever(lookupAccount(any())).thenReturn(ConsumerSessionLookup(exists = false))

        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = false),
            signupHandler = mockLinkSignupHandlerForNetworking(),
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
        val initialSyncResponse = syncResponse().copy(
            manifest = sessionManifest().copy(isLinkWithStripe = true),
            text = TextUpdate(linkLoginPane = linkLoginPane()),
        )

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(initialSyncResponse)
        whenever(lookupAccount(any())).thenReturn(ConsumerSessionLookup(exists = false))

        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = true),
            signupHandler = mockLinkSignupHandlerForInstantDebits(),
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
        val syncResponse = syncResponse().copy(
            manifest = sessionManifest().copy(isLinkWithStripe = false),
            text = TextUpdate(networkingLinkSignupPane = networkingLinkSignupPane()),
        )

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(syncResponse)
        whenever(lookupAccount(any())).thenReturn(ConsumerSessionLookup(exists = false))

        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = false),
            signupHandler = mockLinkSignupHandlerForNetworking(failOnSignup = true),
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
        val initialSyncResponse = syncResponse().copy(
            manifest = sessionManifest().copy(isLinkWithStripe = true),
            text = TextUpdate(linkLoginPane = linkLoginPane()),
        )

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(initialSyncResponse)
        whenever(lookupAccount(any())).thenReturn(ConsumerSessionLookup(exists = false))

        val viewModel = buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = true),
            signupHandler = mockLinkSignupHandlerForInstantDebits(failOnSignup = true),
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

    @Test
    fun `Navigates to error pane if encountering permission exception in lookup in Instant Debits`() = runTest {
        val initialSyncResponse = syncResponse().copy(
            manifest = sessionManifest().copy(
                accountholderCustomerEmailAddress = "known_user@email.com",
                isLinkWithStripe = true,
            ),
            text = TextUpdate(linkLoginPane = linkLoginPane()),
        )

        val permissionException = PermissionException(stripeError = StripeError())

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(initialSyncResponse)
        whenever(lookupAccount(any())).then {
            throw permissionException
        }

        buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = true),
            signupHandler = mockLinkSignupHandlerForInstantDebits(failOnSignup = true),
        )

        delay(300)

        handleError.assertError(
            extraMessage = "Error looking up account",
            error = permissionException,
            pane = LINK_LOGIN,
            displayErrorScreen = true,
        )
    }

    @Test
    fun `Does not navigate to error pane if encountering non-permission exception in lookup in Instant Debits`() = runTest {
        val initialSyncResponse = syncResponse().copy(
            manifest = sessionManifest().copy(
                accountholderCustomerEmailAddress = "known_user@email.com",
                isLinkWithStripe = true,
            ),
            text = TextUpdate(linkLoginPane = linkLoginPane()),
        )

        val apiException = APIConnectionException()

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(initialSyncResponse)
        whenever(lookupAccount(any())).then {
            throw apiException
        }

        buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = true),
            signupHandler = mockLinkSignupHandlerForInstantDebits(failOnSignup = true),
        )

        delay(300)

        handleError.assertError(
            extraMessage = "Error looking up account",
            error = apiException,
            pane = LINK_LOGIN,
            displayErrorScreen = false,
        )
    }

    @Test
    fun `Does not navigate to error pane if encountering any exception in lookup in Financial Connections`() = runTest {
        val initialSyncResponse = syncResponse().copy(
            manifest = sessionManifest().copy(
                accountholderCustomerEmailAddress = "known_user@email.com",
                isLinkWithStripe = false,
            ),
            text = TextUpdate(networkingLinkSignupPane = networkingLinkSignupPane()),
        )

        val permissionException = PermissionException(stripeError = StripeError())

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(initialSyncResponse)
        whenever(lookupAccount(any())).then {
            throw permissionException
        }

        buildViewModel(
            state = NetworkingLinkSignupState(isInstantDebits = false),
            signupHandler = mockLinkSignupHandlerForNetworking(failOnSignup = true),
        )

        delay(300)

        handleError.assertError(
            extraMessage = "Error looking up account",
            error = permissionException,
            pane = NETWORKING_LINK_SIGNUP_PANE,
            displayErrorScreen = false,
        )
    }

    private fun mockLinkSignupHandlerForNetworking(
        failOnSignup: Boolean = false,
    ): LinkSignupHandler {
        val manifest = sessionManifest().copy(
            businessName = "Business",
            accountholderCustomerEmailAddress = "test@test.com"
        )

        val getOrFetchSync = mock<GetOrFetchSync> {
            onBlocking { invoke(any(), anyOrNull()) } doReturn syncResponse().copy(
                manifest = manifest,
                text = TextUpdate(
                    consent = null,
                    networkingLinkSignupPane = networkingLinkSignupPane(),
                )
            )
        }

        val getCachedAccounts = mock<GetCachedAccounts> {
            onBlocking { invoke() } doReturn cachedPartnerAccounts()
        }

        val saveAccountToLink = mock<SaveAccountToLink> {
            if (failOnSignup) {
                onBlocking { new(any(), any(), any(), any(), any()) } doAnswer {
                    throw APIConnectionException()
                }
            } else {
                onBlocking { new(any(), any(), any(), any(), any()) } doReturn manifest
            }
        }

        return LinkSignupHandlerForNetworking(
            getOrFetchSync = getOrFetchSync,
            getCachedAccounts = getCachedAccounts,
            saveAccountToLink = saveAccountToLink,
            eventTracker = eventTracker,
            navigationManager = navigationManager,
            logger = Logger.noop(),
        )
    }

    private fun mockLinkSignupHandlerForInstantDebits(
        failOnSignup: Boolean = false,
    ): LinkSignupHandler {
        val manifest = sessionManifest().copy(
            businessName = "Business",
            accountholderCustomerEmailAddress = "test@test.com",
            nextPane = Pane.INSTITUTION_PICKER,
        )

        val getOrFetchSync = mock<GetOrFetchSync> {
            onBlocking { invoke(any(), anyOrNull()) } doReturn syncResponse().copy(
                manifest = manifest,
                text = TextUpdate(
                    consent = null,
                    linkLoginPane = linkLoginPane(),
                )
            )
        }

        val consumerRepository = mock<FinancialConnectionsConsumerSessionRepository> {
            if (failOnSignup) {
                onBlocking { signUp(any(), any(), any()) } doAnswer {
                    throw APIConnectionException()
                }
            } else {
                onBlocking { signUp(any(), any(), any()) } doReturn consumerSessionSignup()
            }
        }

        return LinkSignupHandlerForInstantDebits(
            getOrFetchSync = getOrFetchSync,
            consumerRepository = consumerRepository,
            attachConsumerToLinkAccountSession = {
                // Mock a successful attach
            },
            navigationManager = navigationManager,
            handleError = handleError,
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
