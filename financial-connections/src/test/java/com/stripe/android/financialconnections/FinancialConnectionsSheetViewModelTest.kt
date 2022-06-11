package com.stripe.android.financialconnections

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.airbnb.mvrx.test.MvRxTestRule
import com.airbnb.mvrx.withState
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEventReporter
import com.stripe.android.financialconnections.domain.FetchFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.FetchFinancialConnectionsSessionForToken
import com.stripe.android.financialconnections.domain.GenerateFinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Canceled
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountFixtures
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class FinancialConnectionsSheetViewModelTest {

    @get:Rule
    val mvrxRule = MvRxTestRule(testDispatcher = UnconfinedTestDispatcher())

    private val eventReporter = mock<FinancialConnectionsEventReporter>()
    private val configuration = FinancialConnectionsSheet.Configuration(
        ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )

    private val manifest = sessionManifest()

    private val fetchFinancialConnectionsSession = mock<FetchFinancialConnectionsSession>()
    private val fetchFinancialConnectionsSessionForToken =
        mock<FetchFinancialConnectionsSessionForToken>()
    private val generateFinancialConnectionsSessionManifest =
        mock<GenerateFinancialConnectionsSessionManifest>()
    private val defaultInitialState = FinancialConnectionsSheetState(
        FinancialConnectionsSheetActivityArgs.ForData(configuration)
    )

    @Test
    fun `init - eventReporter fires onPresented`() {
        createViewModel(defaultInitialState)
        verify(eventReporter).onPresented(configuration)
    }

    @Test
    fun `init - if manifest not present in initial state, fetchManifest triggered`() =
        runTest {
            createViewModel(defaultInitialState)

            verify(generateFinancialConnectionsSessionManifest).invoke(any(), any())
        }

    @Test
    fun `init - if manifest restored from SavedStateHandle, fetchManifest not triggered`() {
        runTest {
            createViewModel(defaultInitialState.copy(manifest = manifest))

            verifyNoInteractions(generateFinancialConnectionsSessionManifest)
        }
    }

    @Test
    fun `handleOnNewIntent - wrong intent should fire analytics event and set fail result`() {
        runTest {
            // Given
            val viewModel = createViewModel(defaultInitialState)

            // When
            viewModel.handleOnNewIntent(Intent("error_url"))

            // Then
            verify(eventReporter)
                .onResult(eq(configuration), any<Failed>())
        }
    }

    @Test
    fun `handleOnNewIntent - intent with cancel url should fire analytics event and set cancel result`() {
        runTest {
            // Given
            whenever(generateFinancialConnectionsSessionManifest(any(), any())).thenReturn(manifest)
            val viewModel = createViewModel(defaultInitialState)
            val cancelIntent = cancelIntent()

            // When
            // end auth flow
            viewModel.handleOnNewIntent(cancelIntent)

            // Then
            verify(eventReporter)
                .onResult(configuration, Canceled)
        }
    }

    @Test
    fun `handleOnNewIntent - when intent with cancel URL received, then finish with Result#Cancel`() =
        runTest {
            // Given
            whenever(generateFinancialConnectionsSessionManifest(any(), any())).thenReturn(manifest)
            val viewModel = createViewModel(defaultInitialState)

            // When
            // end auth flow
            viewModel.handleOnNewIntent(cancelIntent())

            // Then
            withState(viewModel) {
                assertThat(it.authFlowActive).isFalse()
                assertThat(it.viewEffect).isEqualTo(FinishWithResult(Canceled))
            }
        }

    @Test
    fun `handleOnNewIntent - when intent with unknown received, then finish with Result#Failed`() =
        runTest {
            // Given
            val viewModel = createViewModel(defaultInitialState)
            val errorIntent = Intent()

            // When
            // end auth flow
            viewModel.handleOnNewIntent(errorIntent)

            // Then
            withState(viewModel) {
                assertThat(it.authFlowActive).isFalse()
                val viewEffect = it.viewEffect as FinishWithResult
                assertThat(viewEffect.result).isInstanceOf(Failed::class.java)
            }
        }

    @Test
    fun `handleOnNewIntent - when intent with success, then finish with Result#Success`() =
        runTest {
            // Given
            val expectedSession = financialConnectionsSession()
            whenever(generateFinancialConnectionsSessionManifest(any(), any())).thenReturn(manifest)
            whenever(fetchFinancialConnectionsSession(any())).thenReturn(expectedSession)

            val viewModel = createViewModel(defaultInitialState)

            // When
            // end auth flow
            viewModel.handleOnNewIntent(successIntent())

            // Then
            withState(viewModel) {
                assertThat(it.authFlowActive).isFalse()
                val viewEffect = it.viewEffect as FinishWithResult
                assertThat(viewEffect.result).isEqualTo(Completed(expectedSession))
            }
        }

    @Test
    fun `handleOnNewIntent - with success URL but fails fetching session, redirect with error response`() =
        runTest {
            // Given
            val apiException = APIException()
            whenever(generateFinancialConnectionsSessionManifest(any(), any())).thenReturn(manifest)
            whenever(fetchFinancialConnectionsSession.invoke(any())).thenAnswer { throw apiException }
            val viewModel = createViewModel(defaultInitialState)

            // When
            // end auth flow
            viewModel.handleOnNewIntent(successIntent())

            // Then
            withState(viewModel) {
                assertThat(it.authFlowActive).isFalse()
                val viewEffect = it.viewEffect as FinishWithResult
                assertThat(viewEffect.result).isEqualTo(Failed(apiException))
            }
        }

    @Test
    fun `handleOnNewIntent - when error fetching account session, then finish with Result#Failed`() =
        runTest {
            // Given
            whenever(generateFinancialConnectionsSessionManifest(any(), any())).thenReturn(manifest)
            whenever(fetchFinancialConnectionsSession(any())).thenAnswer { throw APIException() }
            val viewModel = createViewModel(defaultInitialState)
            // When
            // end auth flow
            viewModel.handleOnNewIntent(successIntent())

            // Then
            // Then
            withState(viewModel) {
                assertThat(it.authFlowActive).isFalse()
                val viewEffect = it.viewEffect as FinishWithResult
                assertThat(viewEffect.result).isEqualTo(Failed(APIException()))
            }
        }

    @Test
    fun `onResume - when flow is still active and no config changes, finish with Result#Cancelled`() {
        runTest {
            // Given
            whenever(generateFinancialConnectionsSessionManifest(any(), any())).thenReturn(manifest)
            val viewModel = createViewModel(defaultInitialState)

            // When
            // end auth flow (activity resumed without new intent received)
            viewModel.onResume()

            // Then
            withState(viewModel) {
                assertThat(it.authFlowActive).isTrue()
                val viewEffect = it.viewEffect as FinishWithResult
                assertThat(viewEffect.result).isEqualTo(Canceled)
            }
        }
    }

    @Test
    fun `onActivityResult - when flow is still active and config changed, finish with Result#Cancelled`() {
        runTest {
            // Given
            val viewModel = createViewModel(
                defaultInitialState.copy(
                    manifest = manifest,
                    authFlowActive = true
                )
            )

            // When
            // simulate a config change
            viewModel.onActivityRecreated()
            // auth flow ends (activity received result without new intent received)
            viewModel.onBrowserActivityResult()

            // Then
            withState(viewModel) {
                assertThat(it.authFlowActive).isTrue()
                val viewEffect = it.viewEffect as FinishWithResult
                assertThat(viewEffect.result).isEqualTo(Canceled)
            }
        }
    }

    @Test
    fun `init - when repository returns manifest, manifest fetched and stored in state`() {
        runTest {
            // Given
            whenever(generateFinancialConnectionsSessionManifest(any(), any())).thenReturn(manifest)

            // When
            val viewModel = createViewModel(defaultInitialState)

            // Then
            withState(viewModel) { assertThat(it.manifest).isEqualTo(manifest) }
        }
    }

    private fun successIntent(): Intent = Intent().apply {
        data = Uri.parse(ApiKeyFixtures.SUCCESS_URL)
    }

    private fun cancelIntent() = Intent().also {
        it.data = Uri.parse(ApiKeyFixtures.CANCEL_URL)
    }

    private fun financialConnectionsSession() = FinancialConnectionsSession(
        id = "las_no_more",
        clientSecret = configuration.financialConnectionsSessionClientSecret,
        livemode = true,
        accountsNew = FinancialConnectionsAccountList(
            hasMore = false,
            count = 2,
            url = "url",
            totalCount = 2,
            data = listOf(
                FinancialConnectionsAccountFixtures.CREDIT_CARD,
                FinancialConnectionsAccountFixtures.CHECKING_ACCOUNT
            ),
        )
    )

    private fun createViewModel(
        initialState: FinancialConnectionsSheetState,
    ): FinancialConnectionsSheetViewModel {
        return FinancialConnectionsSheetViewModel(
            applicationId = "com.example.app",
            initialState = initialState,
            generateFinancialConnectionsSessionManifest = generateFinancialConnectionsSessionManifest,
            fetchFinancialConnectionsSession = fetchFinancialConnectionsSession,
            fetchFinancialConnectionsSessionForToken = fetchFinancialConnectionsSessionForToken,
            eventReporter = eventReporter
        )
    }
}
