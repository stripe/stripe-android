package com.stripe.android.financialconnections.features.streamlinedconsent

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker.PublicEvent
import com.stripe.android.financialconnections.ThemeMode
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.features.notice.FakePresentSheet
import com.stripe.android.financialconnections.features.notice.FakePresentSheet.PresentCall
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.DataAccess
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.Legal
import com.stripe.android.financialconnections.model.ConsentPane
import com.stripe.android.financialconnections.model.ConsentPaneBody
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitutionSelected
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.StreamlinedConsentPane
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.model.TextUpdate
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.networking.FakeFinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.test.readResourceAsString
import com.stripe.android.financialconnections.ui.HandleClickableUrl
import com.stripe.android.financialconnections.utils.TestHandleError
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.financialconnections.utils.UriUtils
import com.stripe.android.testing.ViewModelStoreTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class StreamlinedConsentViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @Test
    fun `init - loads the streamlined consent pane`() = runScenario {
        val payload = requireNotNull(viewModel.stateFlow.value.payload())

        assertThat(payload.streamlinedConsentPane).isEqualTo(streamlinedConsentPane)
        assertThat(payload.genericScreenState.isHeaderIconInstitutionLogo).isTrue()
    }

    @Test
    fun `init - tracks that the pane loaded`() = runScenario {
        eventTracker.assertContainsEvent(
            expectedEventName = "linked_accounts.pane.loaded",
            expectedParams = mapOf("pane" to "streamlined_consent"),
        )
    }

    @Test
    fun `init - uses the streamlined consent pane's data access notice without a consent pane`() = runScenario {
        val payload = requireNotNull(viewModel.stateFlow.value.payload())

        assertThat(payload.dataAccessNotice).isEqualTo(streamlinedConsentPane.dataAccessNotice)
    }

    @Test
    fun `init - fails when the streamlined consent pane is missing`() = runScenario(
        text = null,
    ) {
        assertIs<Async.Fail<*>>(viewModel.stateFlow.value.payload)
    }

    @Test
    fun `init - prefers the consent pane's data access notice`() = runScenario(
        text = TextUpdate(
            consent = consentPane(),
            streamlinedConsentPane = streamlinedConsentPane,
        ),
    ) {
        val payload = requireNotNull(viewModel.stateFlow.value.payload())

        assertThat(payload.dataAccessNotice).isEqualTo(consentPane().dataAccessNotice)
    }

    @Test
    fun `top app bar - allows back navigation, closing without confirmation and clicking the logo`() = runScenario {
        val update = viewModel.updateTopAppBar(viewModel.stateFlow.value)

        assertThat(update.pane).isEqualTo(Pane.STREAMLINED_CONSENT)
        assertThat(update.allowBackNavigation).isTrue()
        assertThat(update.canCloseWithoutConfirmation).isTrue()
        assertThat(update.hideStripeLogo).isFalse()
        assertThat(update.logoClickNotice).isEqualTo(streamlinedConsentPane.moreInfoNotice)
    }

    @Test
    fun `top app bar - logo is not clickable without a more info notice`() = runScenario(
        text = TextUpdate(streamlinedConsentPane = streamlinedConsentPane.copy(moreInfoNotice = null)),
    ) {
        val update = viewModel.updateTopAppBar(viewModel.stateFlow.value)

        assertThat(update.logoClickNotice).isNull()
    }

    @Test
    fun `onContinueClick - accepts consent and navigates to the next pane`() = runScenario {
        repository.markConsentAcquiredProvider = {
            ApiKeyFixtures.sessionManifest().copy(nextPane = Pane.NETWORKING_LINK_SIGNUP_PANE)
        }

        viewModel.onContinueClick()

        assertThat(eventTracker.publicEvents.awaitItem()).isEqualTo(
            PublicEvent(Name.CONSENT_ACQUIRED, Metadata())
        )
        eventTracker.assertContainsEvent(
            expectedEventName = "linked_accounts.click.agree",
            expectedParams = mapOf("pane" to "streamlined_consent"),
        )
        navigationManager.assertNavigatedTo(
            destination = Destination.NetworkingLinkSignup,
            pane = Pane.STREAMLINED_CONSENT,
        )
        assertIs<Async.Success<*>>(viewModel.stateFlow.value.acceptConsent)
    }

    @Test
    fun `onContinueClick - navigates to the partner auth drawer for OAuth institutions`() = runScenario {
        repository.markConsentAcquiredProvider = {
            ApiKeyFixtures.sessionManifest().copy(nextPane = Pane.PARTNER_AUTH)
        }
        repository.postAuthorizationSessionProvider = {
            ApiKeyFixtures.authorizationSession().copy(_isOAuth = true)
        }

        viewModel.onContinueClick()

        assertThat(eventTracker.publicEvents.awaitItem().name).isEqualTo(Name.CONSENT_ACQUIRED)
        navigationManager.assertNavigatedTo(
            destination = Destination.PartnerAuthDrawer,
            pane = Pane.STREAMLINED_CONSENT,
        )
    }

    @Test
    fun `onContinueClick - navigates to full screen partner auth for non-OAuth institutions`() = runScenario {
        repository.markConsentAcquiredProvider = {
            ApiKeyFixtures.sessionManifest().copy(nextPane = Pane.PARTNER_AUTH)
        }
        repository.postAuthorizationSessionProvider = {
            ApiKeyFixtures.authorizationSession().copy(_isOAuth = false)
        }

        viewModel.onContinueClick()

        assertThat(eventTracker.publicEvents.awaitItem().name).isEqualTo(Name.CONSENT_ACQUIRED)
        navigationManager.assertNavigatedTo(
            destination = Destination.PartnerAuth,
            pane = Pane.STREAMLINED_CONSENT,
        )
    }

    @Test
    fun `onContinueClick - shows an error when the auth session can't be created`() = runScenario {
        val error = IllegalStateException("Institution unavailable")
        repository.markConsentAcquiredProvider = {
            ApiKeyFixtures.sessionManifest().copy(nextPane = Pane.PARTNER_AUTH)
        }
        repository.postAuthorizationSessionProvider = { throw error }

        viewModel.onContinueClick()

        assertThat(eventTracker.publicEvents.awaitItem().name).isEqualTo(Name.CONSENT_ACQUIRED)
        handleError.assertError(
            extraMessage = "Error creating session for institution",
            error = error,
            pane = Pane.STREAMLINED_CONSENT,
            displayErrorScreen = true,
        )
        assertThat(navigationManager.emittedIntents).isEmpty()
    }

    @Test
    fun `onClickableTextClick - data access notice link presents the data access sheet`() = runScenario {
        viewModel.onClickableTextClick("stripe://data-access-notice")

        assertThat(presentSheet.presentCalls.awaitItem()).isEqualTo(
            PresentCall(
                content = DataAccess(requireNotNull(streamlinedConsentPane.dataAccessNotice)),
                referrer = Pane.STREAMLINED_CONSENT,
            )
        )
    }

    @Test
    fun `onClickableTextClick - legal details link presents the legal details sheet`() = runScenario {
        viewModel.onClickableTextClick("stripe://legal-details-notice")

        assertThat(presentSheet.presentCalls.awaitItem()).isEqualTo(
            PresentCall(
                content = Legal(streamlinedConsentPane.legalDetailsNotice),
                referrer = Pane.STREAMLINED_CONSENT,
            )
        )
    }

    @Test
    fun `onClickableTextClick - manual entry link navigates to manual entry`() = runScenario {
        viewModel.onClickableTextClick("stripe://manual-entry")

        navigationManager.assertNavigatedTo(
            destination = Destination.ManualEntry,
            pane = Pane.STREAMLINED_CONSENT,
        )
    }

    @Test
    fun `onClickableTextClick - link account picker link navigates to the link account picker`() = runScenario {
        viewModel.onClickableTextClick("stripe://link-account-picker")

        navigationManager.assertNavigatedTo(
            destination = Destination.LinkAccountPicker,
            pane = Pane.STREAMLINED_CONSENT,
        )
    }

    private fun consentPane(): ConsentPane {
        val streamlinedPane = streamlinedConsentPane
        return ConsentPane(
            aboveCta = "Above CTA",
            body = ConsentPaneBody(bullets = emptyList()),
            cta = "Agree",
            dataAccessNotice = requireNotNull(streamlinedPane.dataAccessNotice).copy(title = "Consent data access"),
            legalDetailsNotice = streamlinedPane.legalDetailsNotice,
            title = "Consent",
        )
    }

    private fun runScenario(
        text: TextUpdate? = TextUpdate(streamlinedConsentPane = streamlinedConsentPane),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val repository = FakeFinancialConnectionsManifestRepository().apply {
            getSynchronizeSessionResponseProvider = {
                SynchronizeSessionResponse(
                    manifest = ApiKeyFixtures.sessionManifest().copy(
                        activeInstitution = ApiKeyFixtures.institution(),
                    ),
                    text = text,
                    visual = ApiKeyFixtures.visual(),
                )
            }
        }
        val eventTracker = TestFinancialConnectionsAnalyticsTracker()
        val navigationManager = TestNavigationManager()
        val presentSheet = FakePresentSheet()
        val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()
        val handleError = TestHandleError()

        val viewModel = StreamlinedConsentViewModel(
            initialState = StreamlinedConsentState(),
            nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
            acceptConsent = AcceptConsent(repository, configuration),
            getOrFetchSync = GetOrFetchSync(repository, configuration, "com.example.app", mock()),
            navigationManager = navigationManager,
            eventTracker = eventTracker,
            handleClickableUrl = HandleClickableUrl(
                uriUtils = UriUtils(Logger.noop(), eventTracker),
                eventTracker = eventTracker,
                logger = Logger.noop(),
            ),
            presentSheet = presentSheet,
            postAuthorizationSession = PostAuthorizationSession(repository, configuration, "com.example.app"),
            handleError = handleError,
            logger = Logger.noop(),
        ).also { viewModelStoreRule.track(it) }

        Scenario(
            viewModel = viewModel,
            repository = repository,
            eventTracker = eventTracker,
            navigationManager = navigationManager,
            presentSheet = presentSheet,
            handleError = handleError,
        ).apply { block() }

        presentSheet.ensureAllEventsConsumed()
        eventTracker.publicEvents.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val viewModel: StreamlinedConsentViewModel,
        val repository: FakeFinancialConnectionsManifestRepository,
        val eventTracker: TestFinancialConnectionsAnalyticsTracker,
        val navigationManager: TestNavigationManager,
        val presentSheet: FakePresentSheet,
        val handleError: TestHandleError,
    )

    private companion object {
        val configuration = FinancialConnectionsSheetConfiguration(
            ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
            ApiKeyFixtures.DEFAULT_API_CONFIG,
            preCollectedConsent = null,
            themeMode = ThemeMode.Automatic,
        )

        val streamlinedConsentPane: StreamlinedConsentPane by lazy {
            val institutionSelected = Json { ignoreUnknownKeys = true }.decodeFromString(
                FinancialConnectionsInstitutionSelected.serializer(),
                readResourceAsString("json/institution_selected_streamlined_consent.json"),
            )
            requireNotNull(institutionSelected.text?.streamlinedConsentPane)
        }
    }
}
