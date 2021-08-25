package com.stripe.android.payments.paymentlauncher

import androidx.activity.result.ActivityResultCaller
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.StripePaymentController.Companion.EXPAND_PAYMENT_METHOD
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.AnalyticsEvent
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.payments.SetupIntentFlowResultProcessor
import com.stripe.android.payments.core.authentication.PaymentAuthenticator
import com.stripe.android.payments.core.authentication.PaymentAuthenticatorRegistry
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PaymentLauncherViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val stripeApiRepository = mock<StripeApiRepository>()
    private val authenticatorRegistry = mock<PaymentAuthenticatorRegistry>()
    private val defaultReturnUrl =
        DefaultReturnUrl.create(ApplicationProvider.getApplicationContext())
    private val apiRequestOptions = mock<ApiRequest.Options>()
    private val authHost = mock<AuthActivityStarterHost>()
    private val threeDs1IntentReturnUrlMap = mock<MutableMap<String, String>>()
    private val paymentIntentFlowResultProcessor = mock<PaymentIntentFlowResultProcessor>()
    private val setupIntentFlowResultProcessor = mock<SetupIntentFlowResultProcessor>()

    private val analyticsRequestExecutor = mock<DefaultAnalyticsRequestExecutor>()
    private val analyticsRequestFactory = mock<AnalyticsRequestFactory>()
    private val uiContext = TestCoroutineDispatcher()
    private val activityResultCaller = mock<ActivityResultCaller>()

    private val viewModel = PaymentLauncherViewModel(
        stripeApiRepository,
        authenticatorRegistry,
        defaultReturnUrl,
        apiRequestOptions,
        threeDs1IntentReturnUrlMap,
        { paymentIntentFlowResultProcessor },
        { setupIntentFlowResultProcessor },
        analyticsRequestExecutor,
        analyticsRequestFactory,
        uiContext,
        authHost,
        activityResultCaller
    )

    private val confirmPaymentIntentParams = ConfirmPaymentIntentParams(
        clientSecret = CLIENT_SECRET,
        paymentMethodId = PM_ID
    )
    private val confirmSetupIntentParams = ConfirmSetupIntentParams(
        clientSecret = CLIENT_SECRET,
        paymentMethodId = PM_ID
    )
    private val paymentIntent = mock<PaymentIntent>()
    private val piAuthenticator = mock<PaymentAuthenticator<PaymentIntent>>()
    private val setupIntent = mock<SetupIntent>()
    private val siAuthenticator = mock<PaymentAuthenticator<SetupIntent>>()
    private val stripeIntent = mock<StripeIntent>()
    private val stripeIntentAuthenticator = mock<PaymentAuthenticator<StripeIntent>>()
    private val succeededPaymentResult =
        PaymentIntentResult(paymentIntent, StripeIntentResult.Outcome.SUCCEEDED)
    private val failedPaymentResult =
        PaymentIntentResult(paymentIntent, StripeIntentResult.Outcome.FAILED)
    private val canceledPaymentResult =
        PaymentIntentResult(paymentIntent, StripeIntentResult.Outcome.CANCELED)
    private val timedOutPaymentResult =
        PaymentIntentResult(paymentIntent, StripeIntentResult.Outcome.TIMEDOUT)
    private val unknownPaymentResult =
        PaymentIntentResult(paymentIntent, StripeIntentResult.Outcome.UNKNOWN)

    @Before
    fun setUpMocks() = runBlockingTest {
        whenever(
            stripeApiRepository.confirmPaymentIntent(any(), any(), any())
        ).thenReturn(paymentIntent)

        whenever(
            stripeApiRepository.confirmSetupIntent(any(), any(), any())
        ).thenReturn(setupIntent)

        whenever(authenticatorRegistry.getAuthenticator(eq(paymentIntent)))
            .thenReturn(piAuthenticator)

        whenever(authenticatorRegistry.getAuthenticator(eq(setupIntent)))
            .thenReturn(siAuthenticator)

        whenever(
            stripeApiRepository.retrieveStripeIntent(
                eq(CLIENT_SECRET),
                eq(apiRequestOptions),
                any()
            )
        )
            .thenReturn(stripeIntent)

        whenever(authenticatorRegistry.getAuthenticator(eq(stripeIntent)))
            .thenReturn(stripeIntentAuthenticator)
    }

    @Test
    fun `verify confirm PaymentIntent without returnUrl invokes StripeRepository and calls correct authenticator`() =
        runBlockingTest {
            viewModel.confirmStripeIntent(confirmPaymentIntentParams)

            verify(analyticsRequestFactory).createRequest(
                AnalyticsEvent.ConfirmReturnUrlNull
            )
            verify(stripeApiRepository).confirmPaymentIntent(
                argWhere {
                    it.returnUrl == defaultReturnUrl.value &&
                        it.paymentMethodId == PM_ID &&
                        it.clientSecret == CLIENT_SECRET &&
                        it.shouldUseStripeSdk()
                },
                eq(apiRequestOptions),
                eq(EXPAND_PAYMENT_METHOD)
            )
            verify(piAuthenticator).authenticate(
                eq(authHost),
                eq(paymentIntent),
                eq(apiRequestOptions)
            )
        }

    @Test
    fun `verify confirm PaymentIntent with returnUrl invokes StripeRepository and calls correct authenticator`() =
        runBlockingTest {
            viewModel.confirmStripeIntent(
                confirmPaymentIntentParams.also {
                    it.returnUrl = RETURN_URL
                }
            )

            verify(analyticsRequestFactory).createRequest(
                AnalyticsEvent.ConfirmReturnUrlCustom
            )
            verify(stripeApiRepository).confirmPaymentIntent(
                argWhere {
                    it.returnUrl == RETURN_URL &&
                        it.paymentMethodId == PM_ID &&
                        it.clientSecret == CLIENT_SECRET &&
                        it.shouldUseStripeSdk()
                },
                eq(apiRequestOptions),
                eq(EXPAND_PAYMENT_METHOD)
            )
            verify(piAuthenticator).authenticate(
                eq(authHost),
                eq(paymentIntent),
                eq(apiRequestOptions)
            )
        }

    @Test
    fun `verify confirm SetupIntent without returnUrl invokes StripeRepository and calls correct authenticator`() =
        runBlockingTest {
            viewModel.confirmStripeIntent(confirmSetupIntentParams)

            verify(analyticsRequestFactory).createRequest(
                AnalyticsEvent.ConfirmReturnUrlNull
            )
            verify(stripeApiRepository).confirmSetupIntent(
                argWhere {
                    it.returnUrl == defaultReturnUrl.value &&
                        it.paymentMethodId == PM_ID &&
                        it.clientSecret == CLIENT_SECRET &&
                        it.shouldUseStripeSdk()
                },
                eq(apiRequestOptions),
                eq(EXPAND_PAYMENT_METHOD)
            )
            verify(siAuthenticator).authenticate(
                eq(authHost),
                eq(setupIntent),
                eq(apiRequestOptions)
            )
        }

    @Test
    fun `verify confirm SetupIntent with returnUrl invokes StripeRepository and calls correct authenticator`() =
        runBlockingTest {
            viewModel.confirmStripeIntent(
                confirmSetupIntentParams.also {
                    it.returnUrl = RETURN_URL
                }
            )

            verify(analyticsRequestFactory).createRequest(
                AnalyticsEvent.ConfirmReturnUrlCustom
            )
            verify(stripeApiRepository).confirmSetupIntent(
                argWhere {
                    it.returnUrl == RETURN_URL &&
                        it.paymentMethodId == PM_ID &&
                        it.clientSecret == CLIENT_SECRET &&
                        it.shouldUseStripeSdk()
                },
                eq(apiRequestOptions),
                eq(EXPAND_PAYMENT_METHOD)
            )
            verify(siAuthenticator).authenticate(
                eq(authHost),
                eq(setupIntent),
                eq(apiRequestOptions)
            )
        }

    @Test
    fun `verify when stripeApiRepository fails then confirmPaymentIntent will post Failed result`() =
        runBlockingTest {
            whenever(stripeApiRepository.confirmPaymentIntent(any(), any(), any()))
                .thenReturn(null)

            viewModel.confirmStripeIntent(confirmPaymentIntentParams)

            assertThat(viewModel.paymentLauncherResult.value)
                .isInstanceOf(PaymentResult.Failed::class.java)
        }

    @Test
    fun `verify when stripeApiRepository fails then confirmSetupIntent will post Failed result`() =
        runBlockingTest {
            whenever(stripeApiRepository.confirmSetupIntent(any(), any(), any()))
                .thenReturn(null)

            viewModel.confirmStripeIntent(confirmSetupIntentParams)

            assertThat(viewModel.paymentLauncherResult.value)
                .isInstanceOf(PaymentResult.Failed::class.java)
        }

    @Test
    fun `verify next action is handled correctly`() =
        runBlockingTest {
            viewModel.handleNextActionForStripeIntent(CLIENT_SECRET)

            verify(stripeIntentAuthenticator).authenticate(
                eq(authHost),
                eq(stripeIntent),
                eq(apiRequestOptions)
            )
        }

    @Test
    fun `verify when stripeApiRepository fails then handleNextAction will post Failed result`() =
        runBlockingTest {
            whenever(
                stripeApiRepository.retrieveStripeIntent(
                    eq(CLIENT_SECRET),
                    eq(apiRequestOptions),
                    any()
                )
            ).thenReturn(null)

            viewModel.handleNextActionForStripeIntent(CLIENT_SECRET)

            assertThat(viewModel.paymentLauncherResult.value)
                .isInstanceOf(PaymentResult.Failed::class.java)
        }

    @Test
    fun `verify success paymentIntentFlowResult is processed correctly`() =
        runBlockingTest {
            viewModel.stripeIntent = paymentIntent
            val paymentFlowResult = mock<PaymentFlowResult.Unvalidated>()
            whenever(paymentIntentFlowResultProcessor.processResult(eq(paymentFlowResult)))
                .thenReturn(succeededPaymentResult)

            viewModel.onPaymentFlowResult(paymentFlowResult)

            assertThat(viewModel.paymentLauncherResult.value)
                .isEqualTo(PaymentResult.Completed)
        }

    @Test
    fun `verify failed paymentIntentFlowResult is processed correctly`() =
        runBlockingTest {
            viewModel.stripeIntent = paymentIntent
            val paymentFlowResult = mock<PaymentFlowResult.Unvalidated>()
            whenever(paymentIntentFlowResultProcessor.processResult(eq(paymentFlowResult)))
                .thenReturn(failedPaymentResult)

            viewModel.onPaymentFlowResult(paymentFlowResult)

            assertThat(viewModel.paymentLauncherResult.value)
                .isInstanceOf(PaymentResult.Failed::class.java)
        }

    @Test
    fun `verify canceled paymentIntentFlowResult is processed correctly`() =
        runBlockingTest {
            viewModel.stripeIntent = paymentIntent
            val paymentFlowResult = mock<PaymentFlowResult.Unvalidated>()
            whenever(paymentIntentFlowResultProcessor.processResult(eq(paymentFlowResult)))
                .thenReturn(canceledPaymentResult)

            viewModel.onPaymentFlowResult(paymentFlowResult)

            assertThat(viewModel.paymentLauncherResult.value)
                .isEqualTo(PaymentResult.Canceled)
        }

    @Test
    fun `verify timedOut paymentIntentFlowResult is processed correctly`() =
        runBlockingTest {
            viewModel.stripeIntent = paymentIntent
            val paymentFlowResult = mock<PaymentFlowResult.Unvalidated>()
            whenever(paymentIntentFlowResultProcessor.processResult(eq(paymentFlowResult)))
                .thenReturn(timedOutPaymentResult)

            viewModel.onPaymentFlowResult(paymentFlowResult)

            assertThat(viewModel.paymentLauncherResult.value)
                .isInstanceOf(PaymentResult.Failed::class.java)
        }

    @Test
    fun `verify unknown paymentIntentFlowResult is processed correctly`() =
        runBlockingTest {
            viewModel.stripeIntent = paymentIntent
            val paymentFlowResult = mock<PaymentFlowResult.Unvalidated>()
            whenever(paymentIntentFlowResultProcessor.processResult(eq(paymentFlowResult)))
                .thenReturn(unknownPaymentResult)

            viewModel.onPaymentFlowResult(paymentFlowResult)

            assertThat(viewModel.paymentLauncherResult.value)
                .isInstanceOf(PaymentResult.Failed::class.java)
        }

    companion object {
        const val CLIENT_SECRET = "clientSecret"
        const val PM_ID = "12345"
        const val RETURN_URL = "return://to.me"
    }
}
