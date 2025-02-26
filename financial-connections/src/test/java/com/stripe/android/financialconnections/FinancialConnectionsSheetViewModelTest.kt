package com.stripe.android.financialconnections

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.financialconnections.FinancialConnectionsSheetState.AuthFlowStatus
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEventReporter
import com.stripe.android.financialconnections.browser.BrowserManager
import com.stripe.android.financialconnections.domain.FetchFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.FetchFinancialConnectionsSessionForToken
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.GetOrFetchSync.RefetchCondition.Always
import com.stripe.android.financialconnections.domain.NativeAuthFlowRouter
import com.stripe.android.financialconnections.exception.AppInitializationError
import com.stripe.android.financialconnections.exception.CustomManualEntryRequiredError
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForData
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForInstantDebits
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Canceled
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountFixtures
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSession.StatusDetails
import com.stripe.android.financialconnections.presentation.withState
import com.stripe.android.financialconnections.utils.TestIntegrityRequestManager
import com.stripe.android.model.IncentiveEligibilitySession
import com.stripe.android.model.LinkMode
import com.stripe.attestation.IntegrityRequestManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class FinancialConnectionsSheetViewModelTest {

    @get:Rule
    val rule: TestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    private val eventReporter = mock<FinancialConnectionsEventReporter>()
    private val configuration = FinancialConnectionsSheet.Configuration(
        ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )

    private val syncResponse = syncResponse()
    private val sessionId = syncResponse.manifest.id

    private val fetchFinancialConnectionsSession = mock<FetchFinancialConnectionsSession>()
    private val browserManager = mock<BrowserManager>()
    private val analyticsTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeRouter = mock<NativeAuthFlowRouter>()
    private val fetchFinancialConnectionsSessionForToken =
        mock<FetchFinancialConnectionsSessionForToken>()
    private val getOrFetchSync =
        mock<GetOrFetchSync>()
    private val defaultInitialState = FinancialConnectionsSheetState(
        args = ForData(configuration),
        savedState = null
    )

    @Test
    fun `init - eventReporter fires onPresented`() {
        runTest {
            whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
            createViewModel(defaultInitialState.copy(manifest = sessionManifest()))
            verify(eventReporter).onPresented()
        }
    }

    @Test
    fun `init - if manifest not present in initial state and attestation ready, fetchManifest triggered`() =
        runTest {
            whenever(getOrFetchSync(any(), anyOrNull())).thenReturn(syncResponse)
            createViewModel(
                defaultInitialState,
                integrityRequestManager = TestIntegrityRequestManager(
                    prepareResult = Result.success(Unit)
                )
            )

            verify(getOrFetchSync).invoke(refetchCondition = Always, supportsAppVerification = true)
        }

    @Test
    fun `init - if manifest not present in initial state and attestation fails, fetchManifest triggered`() =
        runTest {
            whenever(getOrFetchSync(any(), anyOrNull())).thenReturn(syncResponse)
            createViewModel(
                defaultInitialState,
                integrityRequestManager = TestIntegrityRequestManager(
                    prepareResult = Result.failure(Exception())
                )
            )

            verify(getOrFetchSync).invoke(refetchCondition = Always, supportsAppVerification = false)
        }

    @Test
    fun `init - if manifest restored from SavedStateHandle, fetchManifest not triggered`() {
        runTest {
            createViewModel(defaultInitialState.copy(manifest = sessionManifest()))

            verifyNoInteractions(getOrFetchSync)
        }
    }

    @Test
    fun `init - When no browser available, AuthFlow closes and logs error`() = runTest {
        // Given
        whenever(browserManager.canOpenHttpsUrl()).thenReturn(false)
        whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)

        // When
        val viewModel = createViewModel(defaultInitialState)

        // Then
        withState(viewModel) {
            assertThat(it.webAuthFlowStatus).isEqualTo(AuthFlowStatus.NONE)
            require(it.viewEffect is FinishWithResult)
            require(it.viewEffect.result is Failed)
            assertThat(it.viewEffect.result.error)
                .isInstanceOf(AppInitializationError::class.java)
            analyticsTracker.assertContainsEvent(
                expectedEventName = "linked_accounts.error.unexpected",
                expectedParams = mapOf(
                    "pane" to "unexpected_error",
                    "error" to "AppInitializationError",
                    "error_type" to "AppInitializationError",
                    "error_message" to "No Web browser available to launch AuthFlow " +
                        "error Launching the Auth Flow",
                )
            )
        }
    }

    @Test
    fun `init - when instant debits flow, hosted auth url with payment_method create query param is launched`() =
        runTest {
            // Given
            whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
            whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
            whenever(nativeRouter.nativeAuthFlowEnabled(any())).thenReturn(false)

            // When
            val viewModel = createViewModel(
                defaultInitialState.copy(
                    initialArgs = ForInstantDebits(
                        configuration = configuration,
                        elementsSessionContext = ElementsSessionContext(
                            amount = 123,
                            currency = "usd",
                            linkMode = LinkMode.LinkPaymentMethod,
                            billingDetails = null,
                            prefillDetails = ElementsSessionContext.PrefillDetails(
                                email = null,
                                phone = null,
                                phoneCountryCode = null,
                            ),
                            incentiveEligibilitySession = null,
                        ),
                    )
                )
            )

            // Then
            withState(viewModel) {
                val viewEffect = it.viewEffect as OpenAuthFlowWithUrl
                assertThat(viewEffect.url).contains("return_payment_method=true")
            }
        }

    @Test
    fun `init - when instant debits flow, hosted auth url expands the payment method`() =
        runTest {
            // Given
            whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
            whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
            whenever(nativeRouter.nativeAuthFlowEnabled(any())).thenReturn(false)

            // When
            val viewModel = createViewModel(
                defaultInitialState.copy(
                    initialArgs = ForInstantDebits(
                        configuration = configuration,
                        elementsSessionContext = ElementsSessionContext(
                            amount = 123,
                            currency = "usd",
                            linkMode = LinkMode.LinkPaymentMethod,
                            billingDetails = null,
                            prefillDetails = ElementsSessionContext.PrefillDetails(
                                email = null,
                                phone = null,
                                phoneCountryCode = null,
                            ),
                            incentiveEligibilitySession = null,
                        ),
                    )
                )
            )

            // Then
            withState(viewModel) {
                val viewEffect = it.viewEffect as OpenAuthFlowWithUrl
                assertThat(viewEffect.url).contains("expand_payment_method=true")
            }
        }

    @Test
    fun `init - when instant debits flow, hosted auth url doesn't contain link_mode if unknown`() = runTest {
        // Given
        whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
        whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
        whenever(nativeRouter.nativeAuthFlowEnabled(any())).thenReturn(false)

        // When
        val viewModel = createViewModel(
            defaultInitialState.copy(
                initialArgs = ForInstantDebits(
                    configuration = configuration,
                    elementsSessionContext = ElementsSessionContext(
                        amount = 123,
                        currency = "usd",
                        linkMode = null,
                        billingDetails = null,
                        prefillDetails = ElementsSessionContext.PrefillDetails(
                            email = null,
                            phone = null,
                            phoneCountryCode = null,
                        ),
                        incentiveEligibilitySession = null,
                    ),
                )
            )
        )

        // Then
        withState(viewModel) {
            val viewEffect = it.viewEffect as OpenAuthFlowWithUrl
            assertThat(viewEffect.url).doesNotContain("link_mode")
        }
    }

    @Test
    fun `init - when instant debits flow, hosted auth url contains incentive info if eligible`() = runTest {
        // Given
        whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
        whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
        whenever(nativeRouter.nativeAuthFlowEnabled(any())).thenReturn(false)

        // When
        val viewModel = createViewModel(
            defaultInitialState.copy(
                initialArgs = ForInstantDebits(
                    configuration = configuration,
                    elementsSessionContext = ElementsSessionContext(
                        amount = 123,
                        currency = "usd",
                        linkMode = null,
                        billingDetails = null,
                        prefillDetails = ElementsSessionContext.PrefillDetails(
                            email = null,
                            phone = null,
                            phoneCountryCode = null,
                        ),
                        incentiveEligibilitySession = IncentiveEligibilitySession.PaymentIntent("pi_123"),
                    ),
                )
            )
        )

        // Then
        withState(viewModel) {
            val viewEffect = it.viewEffect as OpenAuthFlowWithUrl
            assertThat(viewEffect.url).contains("instantDebitsIncentive=true")
            assertThat(viewEffect.url).contains("incentiveEligibilitySession=pi_123")
        }
    }

    @Test
    fun `init - when instant debits flow, hosted auth url does not contain incentive info if not eligible`() = runTest {
        // Given
        whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
        whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
        whenever(nativeRouter.nativeAuthFlowEnabled(any())).thenReturn(false)

        // When
        val viewModel = createViewModel(
            defaultInitialState.copy(
                initialArgs = ForInstantDebits(
                    configuration = configuration,
                    elementsSessionContext = ElementsSessionContext(
                        amount = 123,
                        currency = "usd",
                        linkMode = null,
                        billingDetails = null,
                        prefillDetails = ElementsSessionContext.PrefillDetails(
                            email = null,
                            phone = null,
                            phoneCountryCode = null,
                        ),
                        incentiveEligibilitySession = null,
                    ),
                )
            )
        )

        // Then
        withState(viewModel) {
            val viewEffect = it.viewEffect as OpenAuthFlowWithUrl
            assertThat(viewEffect.url).contains("instantDebitsIncentive=false")
            assertThat(viewEffect.url).doesNotContain("incentiveEligibilitySession")
        }
    }

    @Test
    fun `init - hosted auth url contains prefill details`() = runTest {
        // Given
        whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
        whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
        whenever(nativeRouter.nativeAuthFlowEnabled(any())).thenReturn(false)

        // When
        val viewModel = createViewModel(
            defaultInitialState.copy(
                initialArgs = ForInstantDebits(
                    configuration = configuration,
                    elementsSessionContext = ElementsSessionContext(
                        amount = 123,
                        currency = "usd",
                        linkMode = null,
                        billingDetails = null,
                        prefillDetails = ElementsSessionContext.PrefillDetails(
                            email = "email@email.com",
                            phone = "5555551234",
                            phoneCountryCode = "US",
                        ),
                        incentiveEligibilitySession = null,
                    ),
                )
            )
        )

        // Then
        withState(viewModel) {
            val viewEffect = it.viewEffect as OpenAuthFlowWithUrl
            assertThat(viewEffect.url).contains("email=email@email.com")
            assertThat(viewEffect.url).contains("linkMobilePhone=5555551234")
            assertThat(viewEffect.url).contains("linkMobilePhoneCountry=US")
        }
    }

    @Test
    fun `init - hosted auth url contains billing details`() = runTest {
        // Given
        whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
        whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
        whenever(nativeRouter.nativeAuthFlowEnabled(any())).thenReturn(false)

        // When
        val viewModel = createViewModel(
            defaultInitialState.copy(
                initialArgs = ForInstantDebits(
                    configuration = configuration,
                    elementsSessionContext = ElementsSessionContext(
                        amount = 123,
                        currency = "usd",
                        linkMode = null,
                        billingDetails = ElementsSessionContext.BillingDetails(
                            name = "John Doe",
                            address = ElementsSessionContext.BillingDetails.Address(
                                line1 = "123 Main St",
                                line2 = "",
                                city = "Toronto",
                                state = "ON",
                                postalCode = "A1B 2C3",
                                country = "CA",
                            ),
                            email = "email@email.com",
                            phone = null,
                        ),
                        prefillDetails = ElementsSessionContext.PrefillDetails(
                            email = null,
                            phone = null,
                            phoneCountryCode = null,
                        ),
                        incentiveEligibilitySession = null,
                    ),
                )
            )
        )

        // Then
        withState(viewModel) {
            val viewEffect = it.viewEffect as OpenAuthFlowWithUrl
            assertThat(viewEffect.url).contains("billingDetails%5Bname%5D=John+Doe")
            assertThat(viewEffect.url).contains("billingDetails%5Bemail%5D=email%40email.com")
            assertThat(viewEffect.url).contains("billingDetails%5Baddress%5D%5Bline1%5D=123+Main+St")
            assertThat(viewEffect.url).contains("billingDetails%5Baddress%5D%5Bcity%5D=Toronto")
            assertThat(viewEffect.url).contains("billingDetails%5Baddress%5D%5Bstate%5D=ON")
            assertThat(viewEffect.url).contains("billingDetails%5Baddress%5D%5Bpostal_code%5D=A1B+2C3")
            assertThat(viewEffect.url).contains("billingDetails%5Baddress%5D%5Bcountry%5D=CA")
            assertThat(viewEffect.url).doesNotContain("billingDetails%5Baddress%5D%5Bline2%5D")
        }
    }

    @Test
    fun `init - when data flow and non-native, hosted auth url without query params is launched`() = runTest {
        // Given
        whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
        whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
        whenever(nativeRouter.nativeAuthFlowEnabled(any())).thenReturn(false)

        // When
        val viewModel = createViewModel(
            defaultInitialState.copy(
                initialArgs = ForData(configuration)
            )
        )

        // Then
        withState(viewModel) {
            val viewEffect = it.viewEffect as OpenAuthFlowWithUrl
            assertThat(viewEffect.url).isEqualTo("${syncResponse.manifest.hostedAuthUrl}&launched_by=android_sdk")
        }
    }

    @Test
    fun `handleOnNewIntent - wrong intent should fire analytics event and set fail result`() = runTest {
        // Given
        whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
        whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
        val viewModel = createViewModel(defaultInitialState)

        // When
        viewModel.handleOnNewIntent(Intent("error_url"))

        // A bad workaround for now
        delay(300)

        // Then
        verify(eventReporter)
            .onResult(eq(sessionId), any<Failed>())
    }

    @Test
    fun `handleOnNewIntent - on Link flows with invalid account, error is thrown`() {
        runTest {
            // Given
            whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
            whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
            val viewModel = createViewModel(
                defaultInitialState.copy(initialArgs = ForInstantDebits(configuration))
            )

            // When
            viewModel.handleOnNewIntent(
                successIntent(
                    "stripe-auth://link-accounts/com.example.app/success"
                )
            )

            // Then
            withState(viewModel) {
                val viewEffect = it.viewEffect as FinishWithResult
                assertThat(viewEffect.result).isInstanceOf(Failed::class.java)
            }
            verify(eventReporter)
                .onResult(eq(sessionId), any<Failed>())
        }
    }

    @Test
    fun `handleOnNewIntent - intent with cancel url should fire analytics event and set cancel result`() {
        runTest {
            // Given
            whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
            whenever(fetchFinancialConnectionsSession(any()))
                .thenReturn(financialConnectionsSessionWithNoMoreAccounts)
            whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
            val viewModel = createViewModel(defaultInitialState)
            val cancelIntent = cancelIntent()

            // When
            // end auth flow
            viewModel.handleOnNewIntent(cancelIntent)

            // A bad workaround for now
            delay(300)

            // Then
            verify(eventReporter)
                .onResult(sessionId, Canceled)
        }
    }

    @Test
    fun `handleOnNewIntent - when intent with cancel URL received, then finish with Result#Cancel`() =
        runTest {
            // Given
            whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
            whenever(fetchFinancialConnectionsSession(any()))
                .thenReturn(financialConnectionsSessionWithNoMoreAccounts)
            whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
            val viewModel = createViewModel(defaultInitialState)

            // When
            // end auth flow
            viewModel.handleOnNewIntent(cancelIntent())

            withState(viewModel) {
                assertThat(it.webAuthFlowStatus).isEqualTo(AuthFlowStatus.NONE)
                assertThat(it.viewEffect).isEqualTo(FinishWithResult(Canceled))
            }
        }

    @Test
    fun `handleOnNewIntent - when intent with cancel URL and custom manual entry, finish Result#Failed`() =
        runTest {
            // Given
            val expectedSession = financialConnectionsSession().copy(
                statusDetails = StatusDetails(
                    cancelled = StatusDetails.Cancelled(
                        reason = StatusDetails.Cancelled.Reason.CUSTOM_MANUAL_ENTRY
                    )
                )
            )
            whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
            whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
            whenever(fetchFinancialConnectionsSession(any())).thenReturn(expectedSession)
            val viewModel = createViewModel(defaultInitialState)

            // When
            // end auth flow
            viewModel.handleOnNewIntent(cancelIntent())

            // Then
            withState(viewModel) {
                require(it.viewEffect is FinishWithResult)
                require(it.viewEffect.result is Failed)
                assertThat(it.viewEffect.result.error).isInstanceOf(CustomManualEntryRequiredError::class.java)
                assertThat(it.webAuthFlowStatus).isEqualTo(AuthFlowStatus.NONE)
            }
        }

    @Test
    fun `handleOnNewIntent - when intent with unknown received, then finish with Result#Failed`() =
        runTest {
            // Given
            whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
            val viewModel = createViewModel(defaultInitialState)
            val errorIntent = Intent()

            // When
            // end auth flow
            viewModel.handleOnNewIntent(errorIntent)

            // Then
            withState(viewModel) {
                assertThat(it.webAuthFlowStatus).isEqualTo(AuthFlowStatus.NONE)
                val viewEffect = it.viewEffect as FinishWithResult
                assertThat(viewEffect.result).isInstanceOf(Failed::class.java)
            }
        }

    @Test
    fun `handleOnNewIntent - when intent with success, then finish with Result#Success`() =
        runTest {
            // Given
            val expectedSession = financialConnectionsSession()
            whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
            whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
            whenever(fetchFinancialConnectionsSession(any())).thenReturn(expectedSession)

            val viewModel = createViewModel(defaultInitialState)

            // When
            // end auth flow
            viewModel.handleOnNewIntent(successIntent())

            // Then
            withState(viewModel) {
                assertThat(it.webAuthFlowStatus).isEqualTo(AuthFlowStatus.NONE)
                val viewEffect = it.viewEffect as FinishWithResult
                assertThat(viewEffect.result).isEqualTo(
                    Completed(financialConnectionsSession = expectedSession)
                )
            }
        }

    @Test
    fun `handleOnNewIntent - when intent with native redirect url, opens received url`() =
        runTest {
            // Given
            val aggregatorUrl = "https://widgets.moneydesktop.com/oauth/predirect_to/MBR-123/456?" +
                "skip_aggregation=true&referral_source=APP&client_redirect_url=123"
            val nativeRedirectUrl = "stripe-auth://native-redirect/com.example.app/$aggregatorUrl"
            val expectedSession = financialConnectionsSession()
            whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
            whenever(fetchFinancialConnectionsSession(any())).thenReturn(expectedSession)

            val viewModel = createViewModel(defaultInitialState)

            // When
            // end auth flow
            viewModel.handleOnNewIntent(Intent().apply { data = Uri.parse(nativeRedirectUrl) })

            // Then
            withState(viewModel) {
                assertThat(it.webAuthFlowStatus).isEqualTo(AuthFlowStatus.INTERMEDIATE_DEEPLINK)
                val viewEffect = it.viewEffect as OpenAuthFlowWithUrl
                assertThat(viewEffect.url).isEqualTo(aggregatorUrl)
            }
        }

    @Test
    fun `handleOnNewIntent - when intent with return url, opens received url`() =
        runTest {
            // Given
            val returnUrlQueryParams = "authSessionId=bcsess_123&code=success&memberGuid=MBR-123"
            val returnUrl =
                "stripe-auth://link-accounts/com.example.app/authentication_return#$returnUrlQueryParams"
            val expectedSession = financialConnectionsSession()
            whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
            whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
            whenever(fetchFinancialConnectionsSession(any())).thenReturn(expectedSession)

            val viewModel = createViewModel(defaultInitialState)

            // When
            // end auth flow
            viewModel.handleOnNewIntent(Intent().apply { data = Uri.parse(returnUrl) })

            // Then
            withState(viewModel) {
                assertThat(it.webAuthFlowStatus).isEqualTo(AuthFlowStatus.INTERMEDIATE_DEEPLINK)
                val viewEffect = it.viewEffect as OpenAuthFlowWithUrl
                assertThat(viewEffect.url).isEqualTo(
                    "${syncResponse.manifest.hostedAuthUrl}&startPolling=true&$returnUrlQueryParams"
                )
            }
        }

    @Test
    fun `handleOnNewIntent - with success URL but fails fetching session, redirect with error response`() =
        runTest {
            // Given
            val apiException = APIException()
            whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
            whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
            whenever(fetchFinancialConnectionsSession.invoke(any())).thenAnswer { throw apiException }
            val viewModel = createViewModel(defaultInitialState)

            // When
            // end auth flow
            viewModel.handleOnNewIntent(successIntent())

            // Then
            withState(viewModel) {
                assertThat(it.webAuthFlowStatus).isEqualTo(AuthFlowStatus.NONE)
                val viewEffect = it.viewEffect as FinishWithResult
                assertThat(viewEffect.result).isEqualTo(Failed(apiException))
            }
        }

    @Test
    fun `handleOnNewIntent - when error fetching account session, then finish with Result#Failed`() =
        runTest {
            // Given
            whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
            whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)
            whenever(fetchFinancialConnectionsSession(any())).thenAnswer { throw APIException() }
            val viewModel = createViewModel(defaultInitialState)
            // When
            // end auth flow
            viewModel.handleOnNewIntent(successIntent())

            // Then
            withState(viewModel) {
                assertThat(it.webAuthFlowStatus).isEqualTo(AuthFlowStatus.NONE)
                val viewEffect = it.viewEffect as FinishWithResult
                assertThat(viewEffect.result).isEqualTo(Failed(APIException()))
            }
        }

    @Test
    fun `onResume - when flow is still active and no config changes, finish with Result#Cancelled`() {
        runTest {
            // Given
            whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
            whenever(getOrFetchSync(any(), any()))
                .thenReturn(
                    syncResponse.copy(
                        manifest = sessionManifest().copy(
                            experimentAssignments = mapOf("native" to "false")
                        )
                    )
                )
            val viewModel = createViewModel(defaultInitialState)

            // When
            // end auth flow (activity resumed without new intent received)
            viewModel.onResume()

            // Then
            withState(viewModel) {
                assertThat(it.webAuthFlowStatus).isEqualTo(AuthFlowStatus.ON_EXTERNAL_ACTIVITY)
                val viewEffect = it.viewEffect as FinishWithResult
                assertThat(viewEffect.result).isEqualTo(Canceled)
                verify(eventReporter).onResult(eq(sessionId), any<Canceled>())
            }
        }
    }

    @Test
    fun `onActivityResult - when flow is still active and config changed, finish with Result#Cancelled`() {
        runTest {
            // Given
            val viewModel = createViewModel(
                defaultInitialState.copy(
                    manifest = syncResponse.manifest,
                    webAuthFlowStatus = AuthFlowStatus.ON_EXTERNAL_ACTIVITY
                )
            )

            whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)

            // When
            // simulate a config change
            viewModel.onActivityRecreated()
            // auth flow ends (activity received result without new intent received)
            viewModel.onBrowserActivityResult()

            // A bad workaround for now
            delay(300)

            // Then
            withState(viewModel) {
                assertThat(it.webAuthFlowStatus).isEqualTo(AuthFlowStatus.ON_EXTERNAL_ACTIVITY)
                val viewEffect = it.viewEffect as FinishWithResult
                assertThat(viewEffect.result).isEqualTo(Canceled)
                verify(eventReporter).onResult(eq(sessionId), any<Canceled>())
            }
        }
    }

    @Test
    fun `Returns correct result when manual entry does not use microdeposits`() {
        runTest {
            // Given
            whenever(fetchFinancialConnectionsSession(any())).thenReturn(
                financialConnectionsSession().copy(
                    paymentAccount = BankAccount(
                        id = "id_1234",
                        last4 = "4242",
                    )
                )
            )

            whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)

            val viewModel = createViewModel(
                defaultInitialState.copy(
                    manifest = syncResponse.manifest.copy(manualEntryUsesMicrodeposits = false),
                    webAuthFlowStatus = AuthFlowStatus.ON_EXTERNAL_ACTIVITY,
                )
            )

            viewModel.stateFlow.test {
                assertThat(awaitItem().viewEffect).isNull()

                viewModel.handleOnNewIntent(successIntent())
                assertThat(awaitItem().webAuthFlowStatus).isEqualTo(AuthFlowStatus.NONE)

                val state = awaitItem()
                val result = (state.viewEffect as FinishWithResult).result as Completed
                val bankAccount = result.financialConnectionsSession?.paymentAccount as BankAccount
                assertThat(bankAccount.usesMicrodeposits).isFalse()
            }
        }
    }

    @Test
    fun `init - when repository returns sync response, stores in state`() {
        runTest {
            // Given
            whenever(browserManager.canOpenHttpsUrl()).thenReturn(true)
            whenever(getOrFetchSync(any(), any())).thenReturn(syncResponse)

            // When
            val viewModel = createViewModel(defaultInitialState)

            // Then
            assertThat(viewModel.stateFlow.value.manifest).isEqualTo(syncResponse.manifest)
        }
    }

    private fun successIntent(
        url: String = ApiKeyFixtures.SUCCESS_URL
    ): Intent = Intent().apply { data = Uri.parse(url) }

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
            )
        )
    )

    private fun createViewModel(
        initialState: FinancialConnectionsSheetState,
        integrityRequestManager: IntegrityRequestManager = TestIntegrityRequestManager()
    ): FinancialConnectionsSheetViewModel {
        return FinancialConnectionsSheetViewModel(
            applicationId = "com.example.app",
            initialState = initialState,
            getOrFetchSync = getOrFetchSync,
            fetchFinancialConnectionsSession = fetchFinancialConnectionsSession,
            fetchFinancialConnectionsSessionForToken = fetchFinancialConnectionsSessionForToken,
            eventReporter = eventReporter,
            nativeRouter = nativeRouter,
            analyticsTracker = analyticsTracker,
            browserManager = browserManager,
            savedStateHandle = SavedStateHandle(),
            nativeAuthFlowCoordinator = mock(),
            integrityRequestManager = integrityRequestManager,
            integrityVerdictManager = mock(),
            logger = Logger.noop()
        )
    }
}
