package com.stripe.android.financialconnections.features.partnerauth

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.test.MvRxTestRule
import com.stripe.android.financialconnections.ApiKeyFixtures.authorizationSession
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.analytics.AuthSessionEvent
import com.stripe.android.financialconnections.domain.CancelAuthorizationSession
import com.stripe.android.financialconnections.domain.CompleteAuthorizationSession
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionOAuthResults
import com.stripe.android.financialconnections.domain.PostAuthSessionEvent
import com.stripe.android.financialconnections.exception.WebAuthFlowCancelledException
import com.stripe.android.financialconnections.model.MixedOAuthParams
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class PartnerAuthViewModelTest {

    @get:Rule
    val mvrxRule = MvRxTestRule(testDispatcher = UnconfinedTestDispatcher())

    private val getManifest = mock<GetManifest>()
    private val postAuthSessionEvent = mock<PostAuthSessionEvent>()
    private val pollAuthorizationSessionOAuthResults = mock<PollAuthorizationSessionOAuthResults>()
    private val completeAuthorizationSession = mock<CompleteAuthorizationSession>()
    private val cancelAuthorizationSession = mock<CancelAuthorizationSession>()

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
            viewModel.onWebAuthFlowFinished(Success("stripe://success"))

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

        viewModel.onWebAuthFlowFinished(Success("stripe://success"))

        verify(postAuthSessionEvent).invoke(
            eq(activeAuthSession.id),
            any<AuthSessionEvent.Success>()
        )
    }

    @Test
    fun `onWebAuthFlowFinished - when webStatus Cancelled, cancels fires retry event (OAuth)`() = runTest {
        val activeAuthSession = authorizationSession()
        val viewModel = createViewModel()

        whenever(getManifest())
            .thenReturn(
                sessionManifest().copy(
                    activeAuthSession = activeAuthSession.copy(_isOAuth = true)
                )
            )

        viewModel.onWebAuthFlowFinished(Fail(WebAuthFlowCancelledException()))

        verify(cancelAuthorizationSession).invoke(
            eq(activeAuthSession.id),
        )
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

            viewModel.onWebAuthFlowFinished(Fail(WebAuthFlowCancelledException()))

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
            completeAuthorizationSession = completeAuthorizationSession,
            createAuthorizationSession = mock(),
            cancelAuthorizationSession = cancelAuthorizationSession,
            eventTracker = mock(),
            postAuthSessionEvent = postAuthSessionEvent,
            getManifest = getManifest,
            goNext = mock(),
            navigationManager = mock(),
            pollAuthorizationSessionOAuthResults = pollAuthorizationSessionOAuthResults,
            logger = mock(),
            initialState = initialState
        )
    }
}
