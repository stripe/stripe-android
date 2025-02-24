package com.stripe.android.financialconnections.features.partnerauth

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.ApiKeyFixtures.authorizationSession
import com.stripe.android.financialconnections.ApiKeyFixtures.institution
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.AuthSessionEvent
import com.stripe.android.financialconnections.domain.CancelAuthorizationSession
import com.stripe.android.financialconnections.domain.CompleteAuthorizationSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionOAuthResults
import com.stripe.android.financialconnections.domain.PostAuthSessionEvent
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.domain.RetrieveAuthorizationSession
import com.stripe.android.financialconnections.exception.InstitutionUnplannedDowntimeError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.MixedOAuthParams
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.WebAuthFlowState
import com.stripe.android.financialconnections.presentation.withState
import com.stripe.android.financialconnections.repository.CoreAuthorizationPendingNetworkingRepairRepository
import com.stripe.android.financialconnections.utils.TestHandleError
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@Suppress("MaxLineLength")
@ExperimentalCoroutinesApi
internal class SupportabilityViewModelTest {

    @get:Rule
    val testRule = CoroutineTestRule()

    private val applicationId = "com.sample.applicationid"
    private val getOrFetchSync = mock<GetOrFetchSync>()
    private val postAuthSessionEvent = mock<PostAuthSessionEvent>()
    private val retrieveAuthorizationSession = mock<RetrieveAuthorizationSession>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val pollAuthorizationSessionOAuthResults = mock<PollAuthorizationSessionOAuthResults>()
    private val completeAuthorizationSession = mock<CompleteAuthorizationSession>()
    private val cancelAuthorizationSession = mock<CancelAuthorizationSession>()
    private val navigationManager = TestNavigationManager()
    private val createAuthorizationSession = mock<PostAuthorizationSession>()
    private val logger = mock<Logger>()
    private val handleError = TestHandleError()
    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()

    @Test
    fun `init - when creating auth session returns unplanned downtime, error is logged and displayed`() =
        runTest {
            val unplannedDowntimeError = InstitutionUnplannedDowntimeError(
                institution = institution(),
                showManualEntry = false,
                stripeException = APIException()
            )
            whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(
                syncResponse(sessionManifest().copy(activeInstitution = institution()))
            )
            whenever(createAuthorizationSession(any(), any())).thenAnswer {
                throw unplannedDowntimeError
            }

            createViewModel()

            handleError.assertError(
                extraMessage = "Error fetching payload / posting AuthSession",
                error = unplannedDowntimeError,
                pane = Pane.PARTNER_AUTH,
                displayErrorScreen = true
            )
        }

    @Test
    fun `init - when existing auth session available, payload succeeds pane loaded emits`() = runTest {
        val activeAuthSession = authorizationSession()
        val activeInstitution = institution()
        val manifest = sessionManifest().copy(
            activeInstitution = activeInstitution,
            activeAuthSession = activeAuthSession
        )
        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(syncResponse(manifest))

        val viewModel = createViewModel(SharedPartnerAuthState(Pane.PARTNER_AUTH_DRAWER))

        eventTracker.assertContainsEvent(
            "linked_accounts.pane.loaded",
            mapOf(
                "pane" to Pane.PARTNER_AUTH.value,
            )
        )

        withState(viewModel) {
            assertThat(it.payload).isInstanceOf(Async.Success::class.java)
            assertEquals(requireNotNull(it.payload()).authSession, activeAuthSession)
        }
    }

    @Test
    fun `onWebAuthFlowFinished - when webStatus Success, polls accounts and authorizes with token`() =
        runTest {
            // Given
            val activeAuthSession = authorizationSession()
            val viewModel = createViewModel()
            val mixedOAuthParams = MixedOAuthParams(
                state = "success",
                code = "code",
                status = "status",
                publicToken = "123456"
            )

            whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(
                syncResponse(
                    sessionManifest().copy(activeAuthSession = activeAuthSession)
                )
            )
            whenever(pollAuthorizationSessionOAuthResults(activeAuthSession))
                .thenReturn(mixedOAuthParams)
            whenever(completeAuthorizationSession(any(), any()))
                .thenReturn(activeAuthSession)

            // When
            viewModel.onWebAuthFlowFinished(WebAuthFlowState.Success("stripe://success"))

            // Then
            verify(completeAuthorizationSession).invoke(
                authorizationSessionId = eq(activeAuthSession.id),
                publicToken = eq(mixedOAuthParams.publicToken)
            )
        }

    @Test
    fun `onWebAuthFlowFinished - when webStatus Success, fires success event`() = runTest {
        val activeAuthSession = authorizationSession()
        val viewModel = createViewModel()

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(
            syncResponse(
                manifest = sessionManifest().copy(activeAuthSession = activeAuthSession)
            )
        )

        viewModel.onWebAuthFlowFinished(WebAuthFlowState.Success("stripe://success"))

        verify(postAuthSessionEvent).invoke(
            eq(activeAuthSession.id),
            any<AuthSessionEvent.Success>()
        )
    }

    @Test
    fun `onWebAuthFlowFinished - when webStatus is cancelled and retrieve pane is PARTNER_AUTH, cancels (Legacy)`() =
        runTest {
            val activeAuthSession = authorizationSession().copy(_isOAuth = false)
            val viewModel = createViewModel()

            whenever(getOrFetchSync(anyOrNull(), anyOrNull()))
                .thenReturn(
                    syncResponse(
                        sessionManifest().copy(
                            activeAuthSession = activeAuthSession.copy(_isOAuth = false)
                        )
                    )
                )

            // simulate that auth session succeeded in abstract auth:
            whenever(retrieveAuthorizationSession.invoke(any()))
                .thenReturn(activeAuthSession.copy(nextPane = Pane.PARTNER_AUTH))

            viewModel.onWebAuthFlowFinished(WebAuthFlowState.Canceled(activeAuthSession.url))

            verify(cancelAuthorizationSession).invoke(
                eq(activeAuthSession.id),
            )
            verify(postAuthSessionEvent).invoke(
                eq(activeAuthSession.id),
                any<AuthSessionEvent.Cancel>()
            )
        }

    @Test
    fun `onWebAuthFlowFinished - when webStatus is cancelled and retrieve pane is ACCOUNT_PICKER, navigates to next pane (OAuth)`() =
        runTest {
            val activeAuthSession = authorizationSession().copy(url = null)
            val activeInstitution = institution()
            val manifest = sessionManifest().copy(
                activeAuthSession = activeAuthSession.copy(_isOAuth = true),
                activeInstitution = activeInstitution
            )
            val syncResponse = syncResponse(manifest)
            whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(syncResponse)
            whenever(createAuthorizationSession.invoke(any(), any())).thenReturn(activeAuthSession)
            // simulate that auth session succeeded in abstract auth:
            whenever(retrieveAuthorizationSession.invoke(any()))
                .thenReturn(activeAuthSession.copy(nextPane = Pane.ACCOUNT_PICKER))

            val viewModel = createViewModel()
            viewModel.onWebAuthFlowFinished(WebAuthFlowState.Canceled(activeAuthSession.url))

            verifyNoInteractions(cancelAuthorizationSession)

            // stays in partner auth pane
            assertThat(navigationManager.emittedIntents).isEmpty()
        }

    @Test
    fun `onWebAuthFlowFinished - when webStatus is cancelled but kill switch is on, cancels (OAuth)`() =
        runTest {
            val activeAuthSession = authorizationSession().copy(url = null)
            val activeInstitution = institution()
            // An auth session was created on the previous pane, before launching Partner Auth.
            val manifest = sessionManifest().copy(
                activeAuthSession = activeAuthSession.copy(_isOAuth = true),
                activeInstitution = activeInstitution,
                features = mapOf(
                    "bank_connections_disable_defensive_auth_session_retrieval_on_complete" to true
                )
            )
            val syncResponse = syncResponse(manifest)
            whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(syncResponse)
            whenever(createAuthorizationSession.invoke(any(), any())).thenReturn(activeAuthSession)
            // simulate that auth session succeeded in abstract auth:
            whenever(retrieveAuthorizationSession.invoke(any()))
                .thenReturn(activeAuthSession.copy(nextPane = Pane.ACCOUNT_PICKER))

            val viewModel = createViewModel()
            viewModel.onWebAuthFlowFinished(WebAuthFlowState.Canceled(activeAuthSession.url))

            verify(cancelAuthorizationSession).invoke(eq(activeAuthSession.id))

            // stays in partner auth pane
            assertThat(navigationManager.emittedIntents).isEmpty()

            // creates an additional session (cancel triggers a retry)
            verify(createAuthorizationSession).invoke(
                eq(activeInstitution),
                eq(syncResponse)
            )

            // sends retry event
            verify(postAuthSessionEvent).invoke(
                eq(activeAuthSession.id),
                any<AuthSessionEvent.Retry>()
            )
        }

    @Test
    fun `onWebAuthFlowFinished - when cancels with no deeplink and retrieve pane is PARTNER_AUTH, retries (OAuth)`() =
        runTest {
            val activeAuthSession = authorizationSession().copy(url = null)
            val activeInstitution = institution()
            // An auth session was created on the previous pane, before launching Partner Auth.
            val manifest = sessionManifest().copy(
                activeAuthSession = activeAuthSession.copy(_isOAuth = true),
                activeInstitution = activeInstitution
            )
            val syncResponse = syncResponse(manifest)
            whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(syncResponse)
            whenever(createAuthorizationSession.invoke(any(), any())).thenReturn(activeAuthSession)
            // simulate that auth session succeeded in abstract auth:
            whenever(retrieveAuthorizationSession.invoke(any()))
                .thenReturn(activeAuthSession.copy(nextPane = Pane.PARTNER_AUTH))

            val viewModel = createViewModel()
            viewModel.onWebAuthFlowFinished(WebAuthFlowState.Canceled(activeAuthSession.url))

            verify(cancelAuthorizationSession).invoke(eq(activeAuthSession.id))

            // stays in partner auth pane
            assertThat(navigationManager.emittedIntents).isEmpty()

            // creates an additional session (cancel triggers a retry)
            verify(createAuthorizationSession).invoke(
                eq(activeInstitution),
                eq(syncResponse)
            )

            // sends retry event
            verify(postAuthSessionEvent).invoke(
                eq(activeAuthSession.id),
                any<AuthSessionEvent.Retry>()
            )
        }

    private fun createViewModel(
        initialState: SharedPartnerAuthState = SharedPartnerAuthState(Pane.PARTNER_AUTH)
    ): PartnerAuthViewModel {
        return PartnerAuthViewModel(
            navigationManager = TestNavigationManager(),
            completeAuthorizationSession = completeAuthorizationSession,
            createAuthorizationSession = createAuthorizationSession,
            cancelAuthorizationSession = cancelAuthorizationSession,
            retrieveAuthorizationSession = retrieveAuthorizationSession,
            eventTracker = eventTracker,
            postAuthSessionEvent = postAuthSessionEvent,
            getOrFetchSync = getOrFetchSync,
            pollAuthorizationSessionOAuthResults = pollAuthorizationSessionOAuthResults,
            logger = logger,
            initialState = initialState,
            browserManager = mock(),
            uriUtils = UriUtils(Logger.noop(), mock()),
            handleError = handleError,
            applicationId = applicationId,
            nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
            presentSheet = mock(),
            pendingRepairRepository = CoreAuthorizationPendingNetworkingRepairRepository(
                savedStateHandle = SavedStateHandle(),
                logger = Logger.noop(),
            ),
            repairAuthSession = mock(),
        )
    }
}
