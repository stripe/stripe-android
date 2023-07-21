package com.stripe.android.payments.paymentlauncher

import android.app.Application
import android.graphics.Color
import androidx.activity.result.ActivityResultCaller
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.StripePaymentController.Companion.EXPAND_PAYMENT_METHOD
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.payments.SetupIntentFlowResultProcessor
import com.stripe.android.payments.core.authentication.PaymentAuthenticator
import com.stripe.android.payments.core.authentication.PaymentAuthenticatorRegistry
import com.stripe.android.payments.core.injection.PaymentLauncherViewModelSubcomponent
import com.stripe.android.testing.fakeCreationExtras
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class PaymentLauncherViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    internal class TestFragment : Fragment()

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
    private val analyticsRequestFactory = mock<PaymentAnalyticsRequestFactory>()
    private val uiContext = UnconfinedTestDispatcher()
    private val activityResultCaller = mock<ActivityResultCaller>()
    private val lifecycleOwner = TestLifecycleOwner()
    private val savedStateHandle = mock<SavedStateHandle>()

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
    private val succeededSetupResult =
        SetupIntentResult(setupIntent, StripeIntentResult.Outcome.SUCCEEDED)

    private fun createViewModel(
        isPaymentIntent: Boolean = true,
        isInstantApp: Boolean = false
    ) =
        PaymentLauncherViewModel(
            isPaymentIntent,
            stripeApiRepository,
            authenticatorRegistry,
            defaultReturnUrl,
            { apiRequestOptions },
            threeDs1IntentReturnUrlMap,
            { paymentIntentFlowResultProcessor },
            { setupIntentFlowResultProcessor },
            analyticsRequestExecutor,
            analyticsRequestFactory,
            uiContext,
            savedStateHandle,
            isInstantApp
        ).apply {
            register(activityResultCaller, lifecycleOwner)
        }

    @Before
    fun setUpMocks() = runTest {
        reset(paymentIntent)
        reset(setupIntent)

        whenever(
            stripeApiRepository.confirmPaymentIntent(any(), any(), any())
        ).thenReturn(Result.success(paymentIntent))

        whenever(
            stripeApiRepository.confirmSetupIntent(any(), any(), any())
        ).thenReturn(Result.success(setupIntent))

        whenever(authenticatorRegistry.getAuthenticator(eq(paymentIntent)))
            .thenReturn(piAuthenticator)

        whenever(authenticatorRegistry.getAuthenticator(eq(setupIntent)))
            .thenReturn(siAuthenticator)

        whenever(
            stripeApiRepository.retrieveStripeIntent(
                clientSecret = eq(CLIENT_SECRET),
                options = eq(apiRequestOptions),
                expandFields = any(),
            )
        ).thenReturn(Result.success(stripeIntent))

        whenever(authenticatorRegistry.getAuthenticator(eq(stripeIntent)))
            .thenReturn(stripeIntentAuthenticator)
    }

    @Test
    fun `verify confirm PaymentIntent without returnUrl invokes StripeRepository and calls correct authenticator`() =
        runTest {
            whenever(paymentIntent.requiresAction()).thenReturn(true)

            createViewModel().confirmStripeIntent(confirmPaymentIntentParams, authHost)

            verify(analyticsRequestFactory).createRequest(
                PaymentAnalyticsEvent.ConfirmReturnUrlNull
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
        runTest {
            whenever(paymentIntent.requiresAction()).thenReturn(true)

            createViewModel().confirmStripeIntent(
                confirmPaymentIntentParams.also {
                    it.returnUrl = RETURN_URL
                },
                authHost
            )

            verify(savedStateHandle).set(PaymentLauncherViewModel.KEY_HAS_STARTED, true)
            verify(analyticsRequestFactory).createRequest(
                PaymentAnalyticsEvent.ConfirmReturnUrlCustom
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
    fun `verify confirm PaymentIntent when no action is required does not invoke authenticator`() =
        runTest {
            whenever(paymentIntent.requiresAction()).thenReturn(false)

            val viewModel = createViewModel()
            viewModel.confirmStripeIntent(
                confirmPaymentIntentParams.also {
                    it.returnUrl = RETURN_URL
                },
                authHost
            )

            verify(savedStateHandle).set(PaymentLauncherViewModel.KEY_HAS_STARTED, true)
            verify(analyticsRequestFactory).createRequest(
                PaymentAnalyticsEvent.ConfirmReturnUrlCustom
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
            verify(piAuthenticator, never()).authenticate(
                eq(authHost),
                eq(paymentIntent),
                eq(apiRequestOptions)
            )

            assertThat(viewModel.paymentLauncherResult.value)
                .isEqualTo(PaymentResult.Completed)
        }

    @Test
    fun `verify confirm SetupIntent without returnUrl invokes StripeRepository and calls correct authenticator`() =
        runTest {
            whenever(setupIntent.requiresAction()).thenReturn(true)

            createViewModel().confirmStripeIntent(confirmSetupIntentParams, authHost)

            verify(savedStateHandle).set(PaymentLauncherViewModel.KEY_HAS_STARTED, true)
            verify(analyticsRequestFactory).createRequest(
                PaymentAnalyticsEvent.ConfirmReturnUrlNull
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
        runTest {
            whenever(setupIntent.requiresAction()).thenReturn(true)

            createViewModel().confirmStripeIntent(
                confirmSetupIntentParams.also {
                    it.returnUrl = RETURN_URL
                },
                authHost
            )

            verify(savedStateHandle).set(PaymentLauncherViewModel.KEY_HAS_STARTED, true)
            verify(analyticsRequestFactory).createRequest(
                PaymentAnalyticsEvent.ConfirmReturnUrlCustom
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
    fun `verify instantApp confirm PaymentIntent without returnUrl gets null returnUrl`() =
        runTest {
            createViewModel(isInstantApp = true).confirmStripeIntent(confirmPaymentIntentParams, authHost)

            verify(analyticsRequestFactory).createRequest(
                PaymentAnalyticsEvent.ConfirmReturnUrlNull
            )
            verify(stripeApiRepository).confirmPaymentIntent(
                argWhere {
                    it.returnUrl == null
                },
                any(),
                any()
            )
        }

    @Test
    fun `verify when stripeApiRepository fails then confirmPaymentIntent will post Failed result`() =
        runTest {
            whenever(stripeApiRepository.confirmPaymentIntent(any(), any(), any()))
                .thenReturn(Result.failure(APIConnectionException()))
            val viewModel = createViewModel()
            viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)

            assertThat(viewModel.paymentLauncherResult.value)
                .isInstanceOf(PaymentResult.Failed::class.java)
        }

    @Test
    fun `verify when stripeApiRepository fails then confirmSetupIntent will post Failed result`() =
        runTest {
            whenever(stripeApiRepository.confirmSetupIntent(any(), any(), any()))
                .thenReturn(Result.failure(APIConnectionException()))

            val viewModel = createViewModel()
            viewModel.confirmStripeIntent(confirmSetupIntentParams, authHost)

            assertThat(viewModel.paymentLauncherResult.value)
                .isInstanceOf(PaymentResult.Failed::class.java)
        }

    @Test
    fun `verify next action is handled correctly`() =
        runTest {
            createViewModel().handleNextActionForStripeIntent(CLIENT_SECRET, authHost)

            verify(savedStateHandle).set(PaymentLauncherViewModel.KEY_HAS_STARTED, true)
            verify(stripeIntentAuthenticator).authenticate(
                eq(authHost),
                eq(stripeIntent),
                eq(apiRequestOptions)
            )
        }

    @Test
    fun `verify when stripeApiRepository fails then handleNextAction will post Failed result`() =
        runTest {
            whenever(
                stripeApiRepository.retrieveStripeIntent(
                    eq(CLIENT_SECRET),
                    eq(apiRequestOptions),
                    any()
                )
            ).thenReturn(Result.failure(APIConnectionException()))

            val viewModel = createViewModel()
            viewModel.handleNextActionForStripeIntent(CLIENT_SECRET, authHost)

            assertThat(viewModel.paymentLauncherResult.value)
                .isInstanceOf(PaymentResult.Failed::class.java)
        }

    @Test
    fun `verify paymentIntentProcessor is chosen correctly`() =
        runTest {
            val viewModel = createViewModel(isPaymentIntent = true)
            val paymentFlowResult = mock<PaymentFlowResult.Unvalidated>()
            whenever(paymentIntentFlowResultProcessor.processResult(eq(paymentFlowResult)))
                .thenReturn(Result.success(succeededPaymentResult))
            viewModel.onPaymentFlowResult(paymentFlowResult)

            verify(paymentIntentFlowResultProcessor).processResult(paymentFlowResult)
            verifyNoMoreInteractions(setupIntentFlowResultProcessor)
        }

    @Test
    fun `verify setupIntentProcessor is chosen correctly`() =
        runTest {
            val viewModel = createViewModel(isPaymentIntent = false)
            val paymentFlowResult = mock<PaymentFlowResult.Unvalidated>()
            whenever(setupIntentFlowResultProcessor.processResult(eq(paymentFlowResult)))
                .thenReturn(Result.success(succeededSetupResult))
            viewModel.onPaymentFlowResult(paymentFlowResult)

            verify(setupIntentFlowResultProcessor).processResult(paymentFlowResult)
            verifyNoMoreInteractions(paymentIntentFlowResultProcessor)
        }

    @Test
    fun `verify success paymentIntentFlowResult is processed correctly`() =
        runTest {
            val viewModel = createViewModel()
            val paymentFlowResult = mock<PaymentFlowResult.Unvalidated>()
            whenever(paymentIntentFlowResultProcessor.processResult(eq(paymentFlowResult)))
                .thenReturn(Result.success(succeededPaymentResult))

            viewModel.onPaymentFlowResult(paymentFlowResult)

            assertThat(viewModel.paymentLauncherResult.value)
                .isEqualTo(PaymentResult.Completed)
        }

    @Test
    fun `verify failed paymentIntentFlowResult is processed correctly`() =
        runTest {
            val viewModel = createViewModel()
            val paymentFlowResult = mock<PaymentFlowResult.Unvalidated>()
            whenever(paymentIntentFlowResultProcessor.processResult(eq(paymentFlowResult)))
                .thenReturn(Result.success(failedPaymentResult))

            viewModel.onPaymentFlowResult(paymentFlowResult)

            assertThat(viewModel.paymentLauncherResult.value)
                .isInstanceOf(PaymentResult.Failed::class.java)
        }

    @Test
    fun `verify canceled paymentIntentFlowResult is processed correctly`() =
        runTest {
            val viewModel = createViewModel()
            val paymentFlowResult = mock<PaymentFlowResult.Unvalidated>()
            whenever(paymentIntentFlowResultProcessor.processResult(eq(paymentFlowResult)))
                .thenReturn(Result.success(canceledPaymentResult))

            viewModel.onPaymentFlowResult(paymentFlowResult)

            assertThat(viewModel.paymentLauncherResult.value)
                .isEqualTo(PaymentResult.Canceled)
        }

    @Test
    fun `verify timedOut paymentIntentFlowResult is processed correctly`() =
        runTest {
            val viewModel = createViewModel()
            val paymentFlowResult = mock<PaymentFlowResult.Unvalidated>()
            whenever(paymentIntentFlowResultProcessor.processResult(eq(paymentFlowResult)))
                .thenReturn(Result.success(timedOutPaymentResult))

            viewModel.onPaymentFlowResult(paymentFlowResult)

            assertThat(viewModel.paymentLauncherResult.value)
                .isInstanceOf(PaymentResult.Failed::class.java)
        }

    @Test
    fun `verify unknown paymentIntentFlowResult is processed correctly`() =
        runTest {
            val viewModel = createViewModel()
            val paymentFlowResult = mock<PaymentFlowResult.Unvalidated>()
            whenever(paymentIntentFlowResultProcessor.processResult(eq(paymentFlowResult)))
                .thenReturn(Result.success(unknownPaymentResult))

            viewModel.onPaymentFlowResult(paymentFlowResult)

            assertThat(viewModel.paymentLauncherResult.value)
                .isInstanceOf(PaymentResult.Failed::class.java)
        }

    @Test
    fun `Invalidates launcher when lifecycle owner is destroyed`() = runTest {
        createViewModel()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        verify(authenticatorRegistry).onLauncherInvalidated()
    }

    @Test
    fun `Factory gets initialized by Injector when Injector is available`() {
        val mockBuilder = mock<PaymentLauncherViewModelSubcomponent.Builder>()
        val mockSubComponent = mock<PaymentLauncherViewModelSubcomponent>()
        val vmToBeReturned: PaymentLauncherViewModel = mock()

        whenever(mockBuilder.build()).thenReturn(mockSubComponent)
        whenever(mockBuilder.isPaymentIntent(any())).thenReturn(mockBuilder)
        whenever(mockBuilder.savedStateHandle(any())).thenReturn(mockBuilder)
        whenever(mockSubComponent.viewModel).thenReturn(vmToBeReturned)

        val injector = object : Injector {
            override fun inject(injectable: Injectable<*>) {
                val factory = injectable as PaymentLauncherViewModel.Factory
                factory.subComponentBuilderProvider = Provider { mockBuilder }
            }
        }

        val injectorKey = WeakMapInjectorRegistry.nextKey("testKey")
        WeakMapInjectorRegistry.register(injector, injectorKey)

        val factory = PaymentLauncherViewModel.Factory {
            PaymentLauncherContract.Args.IntentConfirmationArgs(
                injectorKey,
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                TEST_STRIPE_ACCOUNT_ID,
                false,
                PRODUCT_USAGE,
                mock<ConfirmPaymentIntentParams>(),
                statusBarColor = Color.RED,
            )
        }

        val fragmentScenario = launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
            TestFragment()
        }

        fragmentScenario.onFragment { fragment ->
            val factorySpy = spy(factory)
            val createdViewModel = factorySpy.create(
                modelClass = PaymentLauncherViewModel::class.java,
                extras = fragment.fakeCreationExtras(),
            )

            verify(factorySpy, times(0)).fallbackInitialize(any())
            assertThat(createdViewModel).isEqualTo(vmToBeReturned)
        }

        WeakMapInjectorRegistry.clear()
    }

    @Test
    fun `Factory gets initialized with fallback when no Injector is available`() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val factory = PaymentLauncherViewModel.Factory {
            PaymentLauncherContract.Args.IntentConfirmationArgs(
                DUMMY_INJECTOR_KEY,
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                TEST_STRIPE_ACCOUNT_ID,
                false,
                PRODUCT_USAGE,
                mock<ConfirmPaymentIntentParams>(),
                statusBarColor = Color.RED,
            )
        }

        val fragmentScenario = launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
            TestFragment()
        }

        fragmentScenario.onFragment { fragment ->
            val factorySpy = spy(factory)

            assertNotNull(
                factorySpy.create(
                    modelClass = PaymentLauncherViewModel::class.java,
                    extras = fragment.fakeCreationExtras(),
                )
            )

            verify(factorySpy).fallbackInitialize(
                argWhere {
                    it.application == application
                }
            )
        }
    }

    companion object {
        const val CLIENT_SECRET = "clientSecret"
        const val PM_ID = "12345"
        const val RETURN_URL = "return://to.me"
        const val TEST_STRIPE_ACCOUNT_ID = "accountId"
        val PRODUCT_USAGE = setOf("TestProductUsage")
    }
}
