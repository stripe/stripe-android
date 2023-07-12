package com.stripe.android.financialconnections.features.partnerauth

import com.airbnb.mvrx.test.MavericksTestRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.authorizationSession
import com.stripe.android.financialconnections.ApiKeyFixtures.institution
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.analytics.AuthSessionEvent
import com.stripe.android.financialconnections.domain.CancelAuthorizationSession
import com.stripe.android.financialconnections.domain.CompleteAuthorizationSession
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionOAuthResults
import com.stripe.android.financialconnections.domain.PostAuthSessionEvent
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.model.MixedOAuthParams
import com.stripe.android.financialconnections.presentation.WebAuthFlowState
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class PartnerAuthViewModelTest {

    @get:Rule
    val mvrxRule = MavericksTestRule(testDispatcher = UnconfinedTestDispatcher())

    private val applicationId = "com.sample.applicationid"
    private val getManifest = mock<GetManifest>()
    private val postAuthSessionEvent = mock<PostAuthSessionEvent>()
    private val pollAuthorizationSessionOAuthResults = mock<PollAuthorizationSessionOAuthResults>()
    private val completeAuthorizationSession = mock<CompleteAuthorizationSession>()
    private val cancelAuthorizationSession = mock<CancelAuthorizationSession>()
    private val navigationManager = TestNavigationManager()
    private val createAuthorizationSession = mock<PostAuthorizationSession>()

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

            whenever(getManifest())
                .thenReturn(sessionManifest().copy(activeAuthSession = activeAuthSession))
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

        whenever(getManifest())
            .thenReturn(sessionManifest().copy(activeAuthSession = activeAuthSession))

        viewModel.onWebAuthFlowFinished(WebAuthFlowState.Success("stripe://success"))

        verify(postAuthSessionEvent).invoke(
            eq(activeAuthSession.id),
            any<AuthSessionEvent.Success>()
        )
    }

    @Test
    fun `onWebAuthFlowFinished - when webStatus Cancelled, cancels, reloads session and fires retry event (OAuth)`() =
        runTest {
            val activeAuthSession = authorizationSession()
            val activeInstitution = institution()
            val manifest = sessionManifest().copy(
                activeAuthSession = activeAuthSession.copy(_isOAuth = true),
                activeInstitution = activeInstitution
            )

            whenever(getManifest()).thenReturn(manifest)
            whenever(createAuthorizationSession.invoke(any(), any())).thenReturn(activeAuthSession)

            val viewModel = createViewModel()
            viewModel.onWebAuthFlowFinished(WebAuthFlowState.Canceled)

            verify(cancelAuthorizationSession).invoke(eq(activeAuthSession.id))

            // stays in partner auth pane
            assertThat(navigationManager.emittedEvents).isEmpty()

            // creates two sessions (initial and retry)
            verify(createAuthorizationSession, times(2)).invoke(
                eq(activeInstitution),
                eq(manifest.allowManualEntry)
            )

            // sends retry event
            verify(postAuthSessionEvent).invoke(
                eq(activeAuthSession.id),
                any<AuthSessionEvent.Retry>()
            )
        }

    @Test
    fun `onWebAuthFlowFinished - when webStatus Cancelled, cancels and fires cancel event (Legacy)`() =
        runTest {
            val activeAuthSession = authorizationSession()
            val viewModel = createViewModel()

            whenever(getManifest())
                .thenReturn(
                    sessionManifest().copy(
                        activeAuthSession = activeAuthSession.copy(_isOAuth = false)
                    )
                )

            viewModel.onWebAuthFlowFinished(WebAuthFlowState.Canceled)

            verify(cancelAuthorizationSession).invoke(
                eq(activeAuthSession.id),
            )
            verify(postAuthSessionEvent).invoke(
                eq(activeAuthSession.id),
                any<AuthSessionEvent.Cancel>()
            )
        }

    private fun createViewModel(
        initialState: PartnerAuthState = PartnerAuthState()
    ): PartnerAuthViewModel {
        return PartnerAuthViewModel(
            navigationManager = TestNavigationManager(),
            completeAuthorizationSession = completeAuthorizationSession,
            createAuthorizationSession = createAuthorizationSession,
            cancelAuthorizationSession = cancelAuthorizationSession,
            eventTracker = mock(),
            postAuthSessionEvent = postAuthSessionEvent,
            getManifest = getManifest,
            pollAuthorizationSessionOAuthResults = pollAuthorizationSessionOAuthResults,
            logger = mock(),
            initialState = initialState,
            uriUtils = UriUtils(Logger.noop()),
            applicationId = applicationId
        )
    }
}
