package com.stripe.android.payments.paymentlauncher

import android.app.Application
import android.graphics.Color
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.StripeIntentResult
import com.stripe.android.analytics.FakeDurationProvider
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.Clock
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.payments.SetupIntentFlowResultProcessor
import com.stripe.android.payments.paymentlauncher.PaymentLauncherViewModelTestFakes.FakeAuthActivityStarterHost
import com.stripe.android.payments.paymentlauncher.PaymentLauncherViewModelTestFakes.FakeNextActionHandler
import com.stripe.android.payments.paymentlauncher.PaymentLauncherViewModelTestFakes.FakeNextActionHandlerRegistry
import com.stripe.android.payments.paymentlauncher.PaymentLauncherViewModelTestFakes.FakeStripeRepository
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.FakePollingAnalyticsEventReporter
import com.stripe.android.testing.ViewModelStoreTestRule
import com.stripe.android.testing.fakeCreationExtras
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@Suppress("LargeClass")
class PaymentLauncherViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    internal class TestFragment : Fragment()

    @Test
    fun `verify confirm PaymentIntent without returnUrl invokes StripeRepository and calls correct authenticator`() =
        runScenario {
            val intent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
            repository.confirmPaymentIntentResult = Result.success(intent)

            viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)

            assertThat(savedStateHandle.get<Boolean>(PaymentLauncherViewModel.KEY_HAS_STARTED)).isTrue()
            assertThat(repository.confirmPaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.ConfirmPaymentIntentCall(
                    params = ConfirmPaymentIntentParams(
                        clientSecret = CLIENT_SECRET,
                        paymentMethodId = PM_ID,
                        returnUrl = defaultReturnUrl.value,
                        useStripeSdk = true,
                        paymentMethodCode = "card",
                    ),
                    options = API_REQUEST_OPTIONS,
                    expandFields = EXPAND_PAYMENT_METHOD,
                )
            )
            assertThat(nextActionHandlerRegistry.getNextActionHandlerCalls.awaitItem()).isEqualTo(intent)
            assertThat(nextActionHandlerRegistry.handler.nextActionCalls.awaitItem()).isEqualTo(
                FakeNextActionHandler.NextActionCall(authHost, intent, API_REQUEST_OPTIONS)
            )
        }

    @Test
    fun `verify confirm PaymentIntent with returnUrl invokes StripeRepository and calls correct authenticator`() =
        runScenario {
            val intent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
            repository.confirmPaymentIntentResult = Result.success(intent)
            confirmPaymentIntentParams.returnUrl = RETURN_URL

            viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)

            assertThat(savedStateHandle.get<Boolean>(PaymentLauncherViewModel.KEY_HAS_STARTED)).isTrue()
            assertThat(repository.confirmPaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.ConfirmPaymentIntentCall(
                    params = ConfirmPaymentIntentParams(
                        clientSecret = CLIENT_SECRET,
                        paymentMethodId = PM_ID,
                        returnUrl = RETURN_URL,
                        useStripeSdk = true,
                        paymentMethodCode = "card",
                    ),
                    options = API_REQUEST_OPTIONS,
                    expandFields = EXPAND_PAYMENT_METHOD,
                )
            )
            assertThat(nextActionHandlerRegistry.getNextActionHandlerCalls.awaitItem()).isEqualTo(intent)
            assertThat(nextActionHandlerRegistry.handler.nextActionCalls.awaitItem()).isEqualTo(
                FakeNextActionHandler.NextActionCall(authHost, intent, API_REQUEST_OPTIONS)
            )
        }

    @Test
    fun `verify confirm PaymentIntent when no action is required does not invoke authenticator`() =
        runScenario {
            confirmPaymentIntentParams.returnUrl = RETURN_URL

            viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)

            assertThat(paymentResults.awaitItem()).isEqualTo(
                InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED)
            )
            assertThat(savedStateHandle.get<Boolean>(PaymentLauncherViewModel.KEY_HAS_STARTED)).isTrue()
            assertThat(repository.confirmPaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.ConfirmPaymentIntentCall(
                    params = ConfirmPaymentIntentParams(
                        clientSecret = CLIENT_SECRET,
                        paymentMethodId = PM_ID,
                        returnUrl = RETURN_URL,
                        useStripeSdk = true,
                        paymentMethodCode = "card",
                    ),
                    options = API_REQUEST_OPTIONS,
                    expandFields = EXPAND_PAYMENT_METHOD,
                )
            )
            nextActionHandlerRegistry.getNextActionHandlerCalls.expectNoEvents()
            nextActionHandlerRegistry.handler.nextActionCalls.expectNoEvents()
        }

    @Test
    fun `verify confirm SetupIntent without returnUrl invokes StripeRepository and calls correct authenticator`() =
        runScenario(isPaymentIntent = false) {
            val intent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT
            repository.confirmSetupIntentResult = Result.success(intent)

            viewModel.confirmStripeIntent(confirmSetupIntentParams, authHost)

            assertThat(savedStateHandle.get<Boolean>(PaymentLauncherViewModel.KEY_HAS_STARTED)).isTrue()
            assertThat(repository.confirmSetupIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.ConfirmSetupIntentCall(
                    params = ConfirmSetupIntentParams(
                        clientSecret = SETUP_CLIENT_SECRET,
                        paymentMethodId = PM_ID,
                        returnUrl = defaultReturnUrl.value,
                        useStripeSdk = true,
                        paymentMethodCode = "card",
                    ),
                    options = API_REQUEST_OPTIONS,
                    expandFields = EXPAND_PAYMENT_METHOD,
                )
            )
            assertThat(nextActionHandlerRegistry.getNextActionHandlerCalls.awaitItem()).isEqualTo(intent)
            assertThat(nextActionHandlerRegistry.handler.nextActionCalls.awaitItem()).isEqualTo(
                FakeNextActionHandler.NextActionCall(authHost, intent, API_REQUEST_OPTIONS)
            )
        }

    @Test
    fun `verify confirm SetupIntent with returnUrl invokes StripeRepository and calls correct authenticator`() =
        runScenario(isPaymentIntent = false) {
            val intent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT
            repository.confirmSetupIntentResult = Result.success(intent)
            confirmSetupIntentParams.returnUrl = RETURN_URL

            viewModel.confirmStripeIntent(confirmSetupIntentParams, authHost)

            assertThat(savedStateHandle.get<Boolean>(PaymentLauncherViewModel.KEY_HAS_STARTED)).isTrue()
            assertThat(repository.confirmSetupIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.ConfirmSetupIntentCall(
                    params = ConfirmSetupIntentParams(
                        clientSecret = SETUP_CLIENT_SECRET,
                        paymentMethodId = PM_ID,
                        returnUrl = RETURN_URL,
                        useStripeSdk = true,
                        paymentMethodCode = "card",
                    ),
                    options = API_REQUEST_OPTIONS,
                    expandFields = EXPAND_PAYMENT_METHOD,
                )
            )
            assertThat(nextActionHandlerRegistry.getNextActionHandlerCalls.awaitItem()).isEqualTo(intent)
            assertThat(nextActionHandlerRegistry.handler.nextActionCalls.awaitItem()).isEqualTo(
                FakeNextActionHandler.NextActionCall(authHost, intent, API_REQUEST_OPTIONS)
            )
        }

    @Test
    fun `verify instantApp confirm PaymentIntent without returnUrl gets null returnUrl`() =
        runScenario(isInstantApp = true) {
            viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)

            assertThat(paymentResults.awaitItem()).isEqualTo(
                InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED)
            )
            assertThat(repository.confirmPaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.ConfirmPaymentIntentCall(
                    params = ConfirmPaymentIntentParams(
                        clientSecret = CLIENT_SECRET,
                        paymentMethodId = PM_ID,
                        returnUrl = null,
                        useStripeSdk = true,
                        paymentMethodCode = "card",
                    ),
                    options = API_REQUEST_OPTIONS,
                    expandFields = EXPAND_PAYMENT_METHOD,
                )
            )
        }

    @Test
    fun `verify when stripeApiRepository fails then confirmPaymentIntent will post Failed result`() =
        runScenario {
            val error = APIConnectionException()
            repository.confirmPaymentIntentResult = Result.failure(error)

            viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)

            val result = paymentResults.awaitItem() as InternalPaymentResult.Failed
            assertThat(result.throwable).isSameInstanceAs(error)
            assertThat(repository.confirmPaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.ConfirmPaymentIntentCall(
                    params = ConfirmPaymentIntentParams(
                        clientSecret = CLIENT_SECRET,
                        paymentMethodId = PM_ID,
                        returnUrl = defaultReturnUrl.value,
                        useStripeSdk = true,
                        paymentMethodCode = "card",
                    ),
                    options = API_REQUEST_OPTIONS,
                    expandFields = EXPAND_PAYMENT_METHOD,
                )
            )
        }

    @Test
    fun `verify when stripeApiRepository fails then confirmSetupIntent will post Failed result`() =
        runScenario(isPaymentIntent = false) {
            val error = APIConnectionException()
            repository.confirmSetupIntentResult = Result.failure(error)

            viewModel.confirmStripeIntent(confirmSetupIntentParams, authHost)

            val result = paymentResults.awaitItem() as InternalPaymentResult.Failed
            assertThat(result.throwable).isSameInstanceAs(error)
            assertThat(repository.confirmSetupIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.ConfirmSetupIntentCall(
                    params = ConfirmSetupIntentParams(
                        clientSecret = SETUP_CLIENT_SECRET,
                        paymentMethodId = PM_ID,
                        returnUrl = defaultReturnUrl.value,
                        useStripeSdk = true,
                        paymentMethodCode = "card",
                    ),
                    options = API_REQUEST_OPTIONS,
                    expandFields = EXPAND_PAYMENT_METHOD,
                )
            )
        }

    @Test
    fun `verify next action is handled correctly`() =
        runScenario {
            val intent = repository.retrieveStripeIntentResult.getOrThrow()
            viewModel.handleNextActionForStripeIntent(CLIENT_SECRET, authHost)

            assertThat(savedStateHandle.get<Boolean>(PaymentLauncherViewModel.KEY_HAS_STARTED)).isTrue()
            assertThat(repository.retrieveStripeIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, emptyList())
            )
            assertThat(nextActionHandlerRegistry.getNextActionHandlerCalls.awaitItem()).isEqualTo(intent)
            assertThat(nextActionHandlerRegistry.handler.nextActionCalls.awaitItem()).isEqualTo(
                FakeNextActionHandler.NextActionCall(authHost, intent, API_REQUEST_OPTIONS)
            )
        }

    @Test
    fun `verify when stripeApiRepository fails then handleNextAction will post Failed result`() =
        runScenario {
            val error = APIConnectionException()
            repository.retrieveStripeIntentResult = Result.failure(error)

            viewModel.handleNextActionForStripeIntent(CLIENT_SECRET, authHost)

            val result = paymentResults.awaitItem() as InternalPaymentResult.Failed
            assertThat(result.throwable).isSameInstanceAs(error)
            assertThat(repository.retrieveStripeIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, emptyList())
            )
        }

    @Test
    fun `verify next action with PaymentIntent object is handled correctly without fetching`() =
        runScenario {
            val intent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
            viewModel.handleNextActionForStripeIntent(intent, authHost)

            assertThat(savedStateHandle.get<Boolean>(PaymentLauncherViewModel.KEY_HAS_STARTED)).isTrue()
            assertThat(nextActionHandlerRegistry.getNextActionHandlerCalls.awaitItem()).isEqualTo(intent)
            assertThat(nextActionHandlerRegistry.handler.nextActionCalls.awaitItem()).isEqualTo(
                FakeNextActionHandler.NextActionCall(authHost, intent, API_REQUEST_OPTIONS)
            )
            repository.retrieveStripeIntentCalls.expectNoEvents()
        }

    @Test
    fun `verify next action with SetupIntent object is handled correctly without fetching`() =
        runScenario {
            val intent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT
            viewModel.handleNextActionForStripeIntent(intent, authHost)

            assertThat(savedStateHandle.get<Boolean>(PaymentLauncherViewModel.KEY_HAS_STARTED)).isTrue()
            assertThat(nextActionHandlerRegistry.getNextActionHandlerCalls.awaitItem()).isEqualTo(intent)
            assertThat(nextActionHandlerRegistry.handler.nextActionCalls.awaitItem()).isEqualTo(
                FakeNextActionHandler.NextActionCall(authHost, intent, API_REQUEST_OPTIONS)
            )
            repository.retrieveStripeIntentCalls.expectNoEvents()
        }

    @Test
    fun `verify paymentIntentProcessor is chosen correctly`() =
        runScenario(isPaymentIntent = true) {
            val callbackAccount = "acct_callback"
            val caller = DummyActivityResultCaller.noOp()
            viewModel.register(caller, authHost.lifecycleOwner)
            val registration = nextActionHandlerRegistry.registrationCalls.awaitItem()
            assertThat(registration.caller).isSameInstanceAs(caller)

            registration.callback.onActivityResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = CLIENT_SECRET,
                    flowOutcome = StripeIntentResult.Outcome.SUCCEEDED,
                    stripeAccountId = callbackAccount,
                )
            )

            assertThat(paymentResults.awaitItem()).isEqualTo(
                InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED)
            )
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(
                    clientSecret = CLIENT_SECRET,
                    options = ApiRequest.Options(
                        apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                        stripeAccount = callbackAccount,
                    ),
                    expandFields = EXPAND_PAYMENT_METHOD,
                )
            )
        }

    @Test
    fun `verify setupIntentProcessor is chosen correctly`() =
        runScenario(isPaymentIntent = false) {
            val callbackAccount = "acct_callback"
            val caller = DummyActivityResultCaller.noOp()
            viewModel.register(caller, authHost.lifecycleOwner)
            val registration = nextActionHandlerRegistry.registrationCalls.awaitItem()
            assertThat(registration.caller).isSameInstanceAs(caller)

            registration.callback.onActivityResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = SETUP_CLIENT_SECRET,
                    flowOutcome = StripeIntentResult.Outcome.SUCCEEDED,
                    stripeAccountId = callbackAccount,
                )
            )

            assertThat(paymentResults.awaitItem()).isEqualTo(
                InternalPaymentResult.Completed(SetupIntentFixtures.SI_SUCCEEDED)
            )
            assertThat(repository.retrieveSetupIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(
                    clientSecret = SETUP_CLIENT_SECRET,
                    options = ApiRequest.Options(
                        apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                        stripeAccount = callbackAccount,
                    ),
                    expandFields = EXPAND_PAYMENT_METHOD,
                )
            )
        }

    @Test
    fun `verify PaymentIntent callback exception is returned without retrieving intent`() =
        runScenario(isPaymentIntent = true) {
            val error = APIConnectionException()
            val caller = DummyActivityResultCaller.noOp()
            viewModel.register(caller, authHost.lifecycleOwner)
            val registration = nextActionHandlerRegistry.registrationCalls.awaitItem()
            assertThat(registration.caller).isSameInstanceAs(caller)

            registration.callback.onActivityResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = CLIENT_SECRET,
                    flowOutcome = StripeIntentResult.Outcome.FAILED,
                    exception = error,
                    stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
                )
            )

            val result = paymentResults.awaitItem() as InternalPaymentResult.Failed
            assertThat(result.throwable).isSameInstanceAs(error)
            repository.retrievePaymentIntentCalls.expectNoEvents()
            repository.retrieveSetupIntentCalls.expectNoEvents()
            repository.retrieveStripeIntentCalls.expectNoEvents()
        }

    @Test
    fun `verify SetupIntent callback exception is returned without retrieving intent`() =
        runScenario(isPaymentIntent = false) {
            val error = APIConnectionException()
            val caller = DummyActivityResultCaller.noOp()
            viewModel.register(caller, authHost.lifecycleOwner)
            val registration = nextActionHandlerRegistry.registrationCalls.awaitItem()
            assertThat(registration.caller).isSameInstanceAs(caller)

            registration.callback.onActivityResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = SETUP_CLIENT_SECRET,
                    flowOutcome = StripeIntentResult.Outcome.FAILED,
                    exception = error,
                    stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
                )
            )

            val result = paymentResults.awaitItem() as InternalPaymentResult.Failed
            assertThat(result.throwable).isSameInstanceAs(error)
            repository.retrievePaymentIntentCalls.expectNoEvents()
            repository.retrieveSetupIntentCalls.expectNoEvents()
            repository.retrieveStripeIntentCalls.expectNoEvents()
        }

    @Test
    fun `verify success paymentIntentFlowResult is processed correctly`() =
        runScenario {
            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.SUCCEEDED))

            assertThat(paymentResults.awaitItem()).isEqualTo(
                InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED)
            )
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
        }

    @Test
    fun `verify failed paymentIntentFlowResult is processed correctly`() =
        runScenario {
            val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethod = null,
                nextActionData = null,
            )
            repository.retrievePaymentIntentResult = Result.success(intent)

            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.FAILED))

            assertThat(paymentResults.awaitItem()).isInstanceOf(InternalPaymentResult.Failed::class.java)
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
        }

    @Test
    fun `verify canceled paymentIntentFlowResult is processed correctly`() =
        runScenario {
            val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethod = null,
                nextActionData = null,
            )
            repository.retrievePaymentIntentResult = Result.success(intent)

            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.CANCELED))

            assertThat(paymentResults.awaitItem()).isEqualTo(InternalPaymentResult.Canceled)
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
        }

    @Test
    fun `verify redacted intent is properly handled when handling next action`() =
        runScenario {
            val redactedIntent = requireNotNull(
                PaymentIntent.fromJson(PaymentIntentFixtures.REDACTED_PAYMENT_INTENT_JSON)
            )
            repository.retrieveStripeIntentResult = Result.success(redactedIntent)
            val unredactedIntent = redactedIntent.withUnredactedClientSecret(CLIENT_SECRET)

            viewModel.handleNextActionForStripeIntent(CLIENT_SECRET, authHost)

            assertThat(savedStateHandle.get<Boolean>(PaymentLauncherViewModel.KEY_HAS_STARTED)).isTrue()
            assertThat(repository.retrieveStripeIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, emptyList())
            )
            assertThat(nextActionHandlerRegistry.getNextActionHandlerCalls.awaitItem()).isEqualTo(unredactedIntent)
            assertThat(nextActionHandlerRegistry.handler.nextActionCalls.awaitItem()).isEqualTo(
                FakeNextActionHandler.NextActionCall(authHost, unredactedIntent, API_REQUEST_OPTIONS)
            )
        }

    @Test
    fun `verify timedOut paymentIntentFlowResult is processed correctly`() =
        runScenario {
            val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethod = null,
                nextActionData = null,
            )
            repository.retrievePaymentIntentResult = Result.success(intent)

            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.TIMEDOUT))

            assertThat(paymentResults.awaitItem()).isInstanceOf(InternalPaymentResult.Failed::class.java)
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
        }

    @Test
    fun `verify unknown paymentIntentFlowResult is processed correctly`() =
        runScenario {
            val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                status = null,
                paymentMethod = null,
                nextActionData = null,
            )
            repository.retrievePaymentIntentResult = Result.success(intent)

            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.UNKNOWN))

            assertThat(paymentResults.awaitItem()).isInstanceOf(InternalPaymentResult.Failed::class.java)
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
        }

    @Test
    fun `Invalidates launcher when lifecycle owner is destroyed`() =
        runScenario {
            val caller = DummyActivityResultCaller.noOp()
            viewModel.register(caller, authHost.lifecycleOwner)
            assertThat(nextActionHandlerRegistry.registrationCalls.awaitItem().caller).isSameInstanceAs(caller)

            authHost.lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

            assertThat(nextActionHandlerRegistry.invalidationCalls.awaitItem()).isEqualTo(Unit)
        }

    @Test
    fun `Factory gets initialized`() =
        runScenario {
            val factory = PaymentLauncherViewModel.Factory {
                PaymentLauncherContract.Args.IntentConfirmationArgs(
                    apiConfiguration = API_CONFIGURATION,
                    enableLogging = false,
                    productUsage = PRODUCT_USAGE,
                    includePaymentSheetNextHandlers = false,
                    confirmStripeIntentParams = confirmPaymentIntentParams,
                    statusBarColor = Color.RED,
                )
            }

            val fragmentScenario = launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
                TestFragment()
            }
            try {
                fragmentScenario.onFragment { fragment ->
                    val createdViewModel = factory.create(
                        modelClass = PaymentLauncherViewModel::class.java,
                        extras = fragment.fakeCreationExtras(),
                    )
                    assertThat(createdViewModel).isNotNull()
                    viewModelStoreRule.track(createdViewModel)
                }
            } finally {
                fragmentScenario.close()
            }
        }

    @Test
    fun `verify only first terminal result is emitted`() =
        runScenario {
            viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)
            assertThat(paymentResults.awaitItem()).isEqualTo(
                InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED)
            )

            assertThat(repository.confirmPaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.ConfirmPaymentIntentCall(
                    params = ConfirmPaymentIntentParams(
                        clientSecret = CLIENT_SECRET,
                        paymentMethodId = PM_ID,
                        returnUrl = defaultReturnUrl.value,
                        useStripeSdk = true,
                        paymentMethodCode = "card",
                    ),
                    options = API_REQUEST_OPTIONS,
                    expandFields = EXPAND_PAYMENT_METHOD,
                )
            )

            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.SUCCEEDED))
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
        }

    @Test
    fun `verify guard blocks different result types`() =
        runScenario {
            viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)
            assertThat(paymentResults.awaitItem()).isEqualTo(
                InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED)
            )

            assertThat(repository.confirmPaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.ConfirmPaymentIntentCall(
                    params = ConfirmPaymentIntentParams(
                        clientSecret = CLIENT_SECRET,
                        paymentMethodId = PM_ID,
                        returnUrl = defaultReturnUrl.value,
                        useStripeSdk = true,
                        paymentMethodCode = "card",
                    ),
                    options = API_REQUEST_OPTIONS,
                    expandFields = EXPAND_PAYMENT_METHOD,
                )
            )

            repository.retrievePaymentIntentResult = Result.success(
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(paymentMethod = null, nextActionData = null)
            )
            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.FAILED))
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
        }

    @Test
    fun `verify guard works for next action flows`() =
        runScenario {
            val intent = repository.retrieveStripeIntentResult.getOrThrow()
            viewModel.handleNextActionForStripeIntent(CLIENT_SECRET, authHost)
            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.SUCCEEDED))
            assertThat(paymentResults.awaitItem()).isEqualTo(
                InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED)
            )

            assertThat(repository.retrieveStripeIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, emptyList())
            )
            assertThat(nextActionHandlerRegistry.getNextActionHandlerCalls.awaitItem()).isEqualTo(intent)
            assertThat(nextActionHandlerRegistry.handler.nextActionCalls.awaitItem()).isEqualTo(
                FakeNextActionHandler.NextActionCall(authHost, intent, API_REQUEST_OPTIONS)
            )
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )

            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.SUCCEEDED))
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
        }

    private fun runScenario(
        isPaymentIntent: Boolean = true,
        isInstantApp: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val repository = FakeStripeRepository()
        val nextActionHandlerRegistry = FakeNextActionHandlerRegistry()
        val durationProvider = FakeDurationProvider()
        val pollingAnalyticsEventReporter = FakePollingAnalyticsEventReporter()
        val authHost = FakeAuthActivityStarterHost()
        val savedStateHandle = SavedStateHandle()
        val defaultReturnUrl = DefaultReturnUrl.create(authHost.application)
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val (paymentIntentFlowResultProcessor, setupIntentFlowResultProcessor) = createResultProcessors(
            repository = repository,
            pollingAnalyticsEventReporter = pollingAnalyticsEventReporter,
            dispatcher = dispatcher,
        )
        val viewModel = PaymentLauncherViewModel(
            isPaymentIntent = isPaymentIntent,
            stripeApiRepository = repository,
            nextActionHandlerRegistry = nextActionHandlerRegistry,
            defaultReturnUrl = defaultReturnUrl,
            apiRequestOptionsProvider = { API_REQUEST_OPTIONS },
            lazyPaymentIntentFlowResultProcessor = { paymentIntentFlowResultProcessor },
            lazySetupIntentFlowResultProcessor = { setupIntentFlowResultProcessor },
            analyticsRequestExecutor = AnalyticsRequestExecutor {},
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                packageManager = null,
                packageInfo = null,
                packageName = "com.stripe.test",
                networkTypeProvider = { null },
                defaultProductUsageTokens = emptySet(),
            ),
            uiContext = dispatcher,
            savedStateHandle = savedStateHandle,
            isInstantApp = isInstantApp,
            durationProvider = durationProvider,
        ).also { viewModelStoreRule.track(it) }

        viewModel.internalPaymentResult.test {
            assertThat(awaitItem()).isNull()

            Scenario(
                viewModel = viewModel,
                paymentResults = this@test,
                repository = repository,
                nextActionHandlerRegistry = nextActionHandlerRegistry,
                authHost = authHost,
                savedStateHandle = savedStateHandle,
                defaultReturnUrl = defaultReturnUrl,
            ).apply { block() }

            repository.ensureAllEventsConsumed()
            nextActionHandlerRegistry.ensureAllEventsConsumed()
            pollingAnalyticsEventReporter.ensureAllEventsConsumed()
            authHost.ensureAllEventsConsumed()
        }
    }

    private fun createResultProcessors(
        repository: FakeStripeRepository,
        pollingAnalyticsEventReporter: FakePollingAnalyticsEventReporter,
        dispatcher: TestDispatcher,
    ): Pair<PaymentIntentFlowResultProcessor, SetupIntentFlowResultProcessor> {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val apiConfigProvider: Provider<ApiConfiguration.State> = Provider {
            ApiConfiguration.State(
                publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
            )
        }
        val clock = Clock { dispatcher.scheduler.currentTime }
        return PaymentIntentFlowResultProcessor(
            context,
            apiConfigProvider,
            repository,
            Logger.noop(),
            dispatcher,
            pollingAnalyticsEventReporter,
            clock,
        ) to SetupIntentFlowResultProcessor(
            context,
            apiConfigProvider,
            repository,
            Logger.noop(),
            dispatcher,
            pollingAnalyticsEventReporter,
            clock,
        )
    }

    private data class Scenario(
        val viewModel: PaymentLauncherViewModel,
        val paymentResults: ReceiveTurbine<InternalPaymentResult?>,
        val repository: FakeStripeRepository,
        val nextActionHandlerRegistry: FakeNextActionHandlerRegistry,
        val authHost: FakeAuthActivityStarterHost,
        val savedStateHandle: SavedStateHandle,
        val defaultReturnUrl: DefaultReturnUrl,
    ) {
        val confirmPaymentIntentParams = ConfirmPaymentIntentParams(
            clientSecret = CLIENT_SECRET,
            paymentMethodId = PM_ID,
            paymentMethodCode = "card",
        )
        val confirmSetupIntentParams = ConfirmSetupIntentParams(
            clientSecret = SETUP_CLIENT_SECRET,
            paymentMethodId = PM_ID,
            paymentMethodCode = "card",
        )
    }

    private companion object {
        val CLIENT_SECRET = requireNotNull(PaymentIntentFixtures.PI_SUCCEEDED.clientSecret)
        val SETUP_CLIENT_SECRET = requireNotNull(SetupIntentFixtures.SI_SUCCEEDED.clientSecret)
        const val PM_ID = "pm_12345"
        const val RETURN_URL = "return://to.me"
        const val TEST_STRIPE_ACCOUNT_ID = "acct_123"
        val PRODUCT_USAGE = setOf("TestProductUsage")
        val API_CONFIGURATION = ApiConfiguration.State(
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
        )
        val EXPAND_PAYMENT_METHOD = listOf("payment_method")
        val API_REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccount = TEST_STRIPE_ACCOUNT_ID,
        )

        fun paymentFlowResult(@StripeIntentResult.Outcome outcome: Int) = PaymentFlowResult.Unvalidated(
            clientSecret = CLIENT_SECRET,
            flowOutcome = outcome,
            stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
        )
    }
}
