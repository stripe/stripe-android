package com.stripe.android.connections

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.connections.ConnectionsSheetResult.Completed
import com.stripe.android.connections.ConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.connections.analytics.ConnectionsEventReporter
import com.stripe.android.connections.domain.FetchLinkAccountSession
import com.stripe.android.connections.domain.GenerateLinkAccountSessionManifest
import com.stripe.android.connections.model.LinkAccountSession
import com.stripe.android.connections.model.LinkAccountSessionManifest
import com.stripe.android.connections.model.LinkedAccountFixtures
import com.stripe.android.connections.model.LinkedAccountList
import com.stripe.android.core.exception.APIException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class ConnectionsSheetViewModelTest {

    private val eventReporter = mock<ConnectionsEventReporter>()
    private val configuration = ConnectionsSheet.Configuration(
        ApiKeyFixtures.DEFAULT_LINK_ACCOUNT_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )
    private val manifest = LinkAccountSessionManifest(
        ApiKeyFixtures.HOSTED_AUTH_URL,
        ApiKeyFixtures.SUCCESS_URL,
        ApiKeyFixtures.CANCEL_URL
    )
    private val fetchLinkAccountSession = mock<FetchLinkAccountSession>()
    private val generateLinkAccountSessionManifest = mock<GenerateLinkAccountSessionManifest>()

    @Test
    fun `init - eventReporter fires onPresented`() {
        createViewModel(configuration)
        verify(eventReporter)
            .onPresented(configuration)
    }

    @Test
    fun `handleOnNewIntent - wrong intent should fire analytics event and set fail result`() {
        runTest {
            // Given
            val viewModel = createViewModel(configuration)

            // When
            viewModel.handleOnNewIntent(Intent("error_url"))

            // Then
            verify(eventReporter)
                .onResult(eq(configuration), any<ConnectionsSheetResult.Failed>())
        }
    }

    @Test
    fun `handleOnNewIntent - intent with cancel url should fire analytics event and set cancel result`() {
        runTest {
            // Given
            whenever(generateLinkAccountSessionManifest(any(), any())).thenReturn(manifest)
            val viewModel = createViewModel(configuration)
            val cancelIntent = cancelIntent()

            // When
            // end auth flow
            viewModel.handleOnNewIntent(cancelIntent)

            // Then
            verify(eventReporter)
                .onResult(configuration, ConnectionsSheetResult.Canceled)
        }
    }

    @Test
    fun `handleOnNewIntent - when intent with cancel URL received, then finish with Result#Cancel`() =
        runTest {
            // Given
            whenever(generateLinkAccountSessionManifest(any(), any())).thenReturn(manifest)
            val viewModel = createViewModel(configuration)
            viewModel.viewEffect.test {
                // When
                // end auth flow
                viewModel.handleOnNewIntent(cancelIntent())

                // Then
                assertThat(viewModel.state.value.authFlowActive).isFalse()
                assertThat(FinishWithResult(ConnectionsSheetResult.Canceled)).isEqualTo(awaitItem())
            }
        }

    @Test
    fun `handleOnNewIntent - when intent with unknown received, then finish with Result#Failed`() =
        runTest {
            val viewModel = createViewModel(configuration)
            viewModel.viewEffect.test {
                // Given
                val errorIntent = Intent()

                // When
                // end auth flow
                viewModel.handleOnNewIntent(errorIntent)

                // Then
                assertThat(viewModel.state.value.authFlowActive).isFalse()
                val viewEffect = awaitItem() as FinishWithResult
                assertThat(viewEffect.result).isInstanceOf(ConnectionsSheetResult.Failed::class.java)
            }
        }

    @Test
    fun `handleOnNewIntent - when intent with success, then finish with Result#Success`() =
        runTest {
            // Given
            val expectedLinkAccountSession = linkAccountSession()
            whenever(generateLinkAccountSessionManifest(any(), any())).thenReturn(manifest)
            whenever(fetchLinkAccountSession(any())).thenReturn(expectedLinkAccountSession)

            val viewModel = createViewModel(configuration)
            viewModel.viewEffect.test {

                // When
                // end auth flow
                viewModel.handleOnNewIntent(successIntent())

                // Then
                assertThat(viewModel.state.value.authFlowActive).isFalse()
                assertThat(awaitItem()).isEqualTo(
                    FinishWithResult(result = Completed(expectedLinkAccountSession))
                )
            }
        }

    @Test
    fun `handleOnNewIntent - with success URL but fails fetching session, redirect with error response`() =
        runTest {
            // Given
            val apiException = APIException()
            whenever(generateLinkAccountSessionManifest(any(), any())).thenReturn(manifest)
            whenever(fetchLinkAccountSession.invoke(any())).thenAnswer { throw apiException }
            val viewModel = createViewModel(configuration)
            viewModel.viewEffect.test {
                // When
                // end auth flow
                viewModel.handleOnNewIntent(successIntent())

                // Then
                assertThat(viewModel.state.value.authFlowActive).isFalse()
                assertThat(awaitItem()).isEqualTo(
                    FinishWithResult(ConnectionsSheetResult.Failed(apiException))
                )
            }
        }

    @Test
    fun `handleOnNewIntent - when error fetching account session, then finish with Result#Failed`() =
        runTest {
            // Given
            whenever(generateLinkAccountSessionManifest(any(), any())).thenReturn(manifest)
            whenever(fetchLinkAccountSession(any())).thenAnswer { throw APIException() }
            val viewModel = createViewModel(configuration)
            viewModel.viewEffect.test {
                // When
                // end auth flow
                viewModel.handleOnNewIntent(successIntent())

                // Then
                assertThat(viewModel.state.value.authFlowActive).isFalse()
                assertThat(awaitItem()).isEqualTo(
                    FinishWithResult(ConnectionsSheetResult.Failed(APIException()))
                )
            }
        }

    @Test
    fun `onResume - when flow is still active and no config changes, finish with Result#Cancelled`() {
        runTest {
            // Given
            val viewModel = createViewModel(configuration)
            viewModel.viewEffect.test {
                // When
                // end auth flow (activity resumed without new intent received)
                viewModel.onResume()

                // Then
                assertThat(viewModel.state.value.authFlowActive).isTrue()
                assertThat(awaitItem()).isEqualTo(FinishWithResult(ConnectionsSheetResult.Canceled))
            }
        }
    }

    @Test
    fun `onActivityResult - when flow is still active and config changed, finish with Result#Cancelled`() {
        runTest {
            // Given
            val viewModel = createViewModel(configuration)
            viewModel.viewEffect.test {
                // When
                // configuration changes, changing lifecycle flow.
                viewModel.onActivityRecreated()
                // auth flow ends (activity received result without new intent received)
                viewModel.onActivityResult()

                // Then
                assertThat(viewModel.state.value.authFlowActive).isTrue()
                assertThat(awaitItem()).isEqualTo(FinishWithResult(ConnectionsSheetResult.Canceled))
            }
        }
    }

    @Test
    fun `init - when repository returns manifest, manifest fetched and stored in state`() {
        runTest {
            // Given
            whenever(generateLinkAccountSessionManifest(any(), any())).thenReturn(manifest)

            // When
            val viewModel = createViewModel(configuration)

            // Then
            assertThat(viewModel.state.value.manifest).isEqualTo(manifest)
        }
    }

    private fun successIntent(): Intent = Intent().apply {
        data = Uri.parse(ApiKeyFixtures.SUCCESS_URL)
    }

    private fun cancelIntent() = Intent().also {
        it.data = Uri.parse(ApiKeyFixtures.CANCEL_URL)
    }

    private fun linkAccountSession() = LinkAccountSession(
        id = "las_no_more",
        clientSecret = configuration.linkAccountSessionClientSecret,
        livemode = true,
        linkedAccounts = LinkedAccountList(
            hasMore = false,
            count = 2,
            url = "url",
            totalCount = 2,
            linkedAccounts = listOf(
                LinkedAccountFixtures.CREDIT_CARD,
                LinkedAccountFixtures.CHECKING_ACCOUNT
            ),
        )
    )

    private fun createViewModel(
        configuration: ConnectionsSheet.Configuration
    ): ConnectionsSheetViewModel {
        val args = ConnectionsSheetContract.Args(configuration)
        return ConnectionsSheetViewModel(
            applicationId = "com.example.app",
            starterArgs = args,
            generateLinkAccountSessionManifest = generateLinkAccountSessionManifest,
            fetchLinkAccountSession = fetchLinkAccountSession,
            eventReporter = eventReporter
        )
    }
}
