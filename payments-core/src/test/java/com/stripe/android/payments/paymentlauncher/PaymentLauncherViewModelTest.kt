package com.stripe.android.payments.paymentlauncher

import android.app.Application
import android.graphics.Color
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.StripeIntentResult
import com.stripe.android.analytics.FakeDurationProvider
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.Clock
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.payments.SetupIntentFlowResultProcessor
import com.stripe.android.payments.paymentlauncher.PaymentLauncherViewModelTestFakes.FakeAnalyticsRequestExecutor
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

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)

                expectNoEvents()
            }
            assertThat(savedStateHandle.get<Boolean>(PaymentLauncherViewModel.KEY_HAS_STARTED)).isTrue()
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlNull.toString())
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

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)

                expectNoEvents()
            }
            assertThat(savedStateHandle.get<Boolean>(PaymentLauncherViewModel.KEY_HAS_STARTED)).isTrue()
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlCustom.toString())
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

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)

                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))
            }
            assertThat(savedStateHandle.get<Boolean>(PaymentLauncherViewModel.KEY_HAS_STARTED)).isTrue()
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlCustom.toString())
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
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
        }

    @Test
    fun `verify confirm SetupIntent without returnUrl invokes StripeRepository and calls correct authenticator`() =
        runScenario {
            val intent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT
            repository.confirmSetupIntentResult = Result.success(intent)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.confirmStripeIntent(confirmSetupIntentParams, authHost)

                expectNoEvents()
            }
            assertThat(savedStateHandle.get<Boolean>(PaymentLauncherViewModel.KEY_HAS_STARTED)).isTrue()
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlNull.toString())
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
        runScenario {
            val intent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT
            repository.confirmSetupIntentResult = Result.success(intent)
            confirmSetupIntentParams.returnUrl = RETURN_URL

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.confirmStripeIntent(confirmSetupIntentParams, authHost)

                expectNoEvents()
            }
            assertThat(savedStateHandle.get<Boolean>(PaymentLauncherViewModel.KEY_HAS_STARTED)).isTrue()
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlCustom.toString())
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
            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)

                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))
            }
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlNull.toString())
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
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
        }

    @Test
    fun `verify when stripeApiRepository fails then confirmPaymentIntent will post Failed result`() =
        runScenario {
            val error = APIConnectionException()
            repository.confirmPaymentIntentResult = Result.failure(error)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)

                val result = awaitItem() as InternalPaymentResult.Failed
                assertThat(result.throwable).isSameInstanceAs(error)
            }
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlNull.toString())
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
            val finished = analyticsRequestExecutor.requests.awaitItem()
            assertThat(finished.params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
            assertThat(finished.params).containsEntry("status", "failed")
        }

    @Test
    fun `verify when stripeApiRepository fails then confirmSetupIntent will post Failed result`() =
        runScenario {
            val error = APIConnectionException()
            repository.confirmSetupIntentResult = Result.failure(error)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.confirmStripeIntent(confirmSetupIntentParams, authHost)

                val result = awaitItem() as InternalPaymentResult.Failed
                assertThat(result.throwable).isSameInstanceAs(error)
            }
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlNull.toString())
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
            val finished = analyticsRequestExecutor.requests.awaitItem()
            assertThat(finished.params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
            assertThat(finished.params).containsEntry("status", "failed")
        }

    @Test
    fun `verify next action is handled correctly`() =
        runScenario {
            val intent = repository.retrieveStripeIntentResult.getOrThrow()
            viewModel.handleNextActionForStripeIntent(CLIENT_SECRET, authHost)

            assertThat(savedStateHandle.get<Boolean>(PaymentLauncherViewModel.KEY_HAS_STARTED)).isTrue()
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherNextActionStarted.toString())
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

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.handleNextActionForStripeIntent(CLIENT_SECRET, authHost)

                val result = awaitItem() as InternalPaymentResult.Failed
                assertThat(result.throwable).isSameInstanceAs(error)
            }
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherNextActionStarted.toString())
            assertThat(repository.retrieveStripeIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, emptyList())
            )
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherNextActionFinished.toString())
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
            val caller = DummyActivityResultCaller.noOp()
            viewModel.register(caller, authHost.lifecycleOwner)
            val registration = nextActionHandlerRegistry.registrationCalls.awaitItem()
            assertThat(registration.caller).isSameInstanceAs(caller)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                registration.callback.onActivityResult(
                    PaymentFlowResult.Unvalidated(
                        clientSecret = CLIENT_SECRET,
                        flowOutcome = StripeIntentResult.Outcome.SUCCEEDED,
                        stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
                    )
                )

                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))
            }
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
        }

    @Test
    fun `verify setupIntentProcessor is chosen correctly`() =
        runScenario(isPaymentIntent = false) {
            val caller = DummyActivityResultCaller.noOp()
            viewModel.register(caller, authHost.lifecycleOwner)
            val registration = nextActionHandlerRegistry.registrationCalls.awaitItem()
            assertThat(registration.caller).isSameInstanceAs(caller)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                registration.callback.onActivityResult(
                    PaymentFlowResult.Unvalidated(
                        clientSecret = SETUP_CLIENT_SECRET,
                        flowOutcome = StripeIntentResult.Outcome.SUCCEEDED,
                        stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
                    )
                )

                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Completed(SetupIntentFixtures.SI_SUCCEEDED))
            }
            assertThat(repository.retrieveSetupIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(SETUP_CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
        }

    @Test
    fun `verify success paymentIntentFlowResult is processed correctly`() =
        runScenario {
            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.SUCCEEDED))

                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))
            }
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
        }

    @Test
    fun `verify failed paymentIntentFlowResult is processed correctly`() =
        runScenario {
            val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethod = null,
                nextActionData = null,
            )
            repository.retrievePaymentIntentResult = Result.success(intent)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.FAILED))

                assertThat(awaitItem()).isInstanceOf(InternalPaymentResult.Failed::class.java)
            }
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
        }

    @Test
    fun `verify canceled paymentIntentFlowResult is processed correctly`() =
        runScenario {
            val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethod = null,
                nextActionData = null,
            )
            repository.retrievePaymentIntentResult = Result.success(intent)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.CANCELED))

                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Canceled)
            }
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
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
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherNextActionStarted.toString())
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

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.TIMEDOUT))

                assertThat(awaitItem()).isInstanceOf(InternalPaymentResult.Failed::class.java)
            }
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
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

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.UNKNOWN))

                assertThat(awaitItem()).isInstanceOf(InternalPaymentResult.Failed::class.java)
            }
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
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
    fun `invalid PaymentIntent client secret is omitted from confirmation analytics`() =
        runScenario {
            val clientSecret = "person@example.com"
            val error = IllegalArgumentException("Invalid PaymentIntent client secret.")
            repository.confirmPaymentIntentResult = Result.failure(error)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()
                viewModel.confirmStripeIntent(confirmPaymentIntentParams.copy(clientSecret = clientSecret), authHost)
                val result = awaitItem() as InternalPaymentResult.Failed
                assertThat(result.throwable).isSameInstanceAs(error)
            }
            assertThat(repository.confirmPaymentIntentCalls.awaitItem().params.clientSecret).isEqualTo(clientSecret)
            val started = analyticsRequestExecutor.requests.awaitItem().params
            assertThat(started).containsEntry("event", PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
            assertThat(started).doesNotContainKey("intent_id")
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlNull.toString())
            val finished = analyticsRequestExecutor.requests.awaitItem().params
            assertThat(finished).containsEntry("event", PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
            assertThat(finished).doesNotContainKey("intent_id")
            assertThat(finished).containsEntry("status", "failed")
        }

    @Test
    fun `invalid SetupIntent client secret is omitted from confirmation analytics`() =
        runScenario(isPaymentIntent = false) {
            val clientSecret = "person@example.com_secret_invalid"
            val error = IllegalArgumentException("Invalid SetupIntent client secret.")
            repository.confirmSetupIntentResult = Result.failure(error)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()
                viewModel.confirmStripeIntent(confirmSetupIntentParams.copy(clientSecret = clientSecret), authHost)
                val result = awaitItem() as InternalPaymentResult.Failed
                assertThat(result.throwable).isSameInstanceAs(error)
            }
            assertThat(repository.confirmSetupIntentCalls.awaitItem().params.clientSecret).isEqualTo(clientSecret)
            val started = analyticsRequestExecutor.requests.awaitItem().params
            assertThat(started).containsEntry("event", PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
            assertThat(started).doesNotContainKey("intent_id")
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlNull.toString())
            val finished = analyticsRequestExecutor.requests.awaitItem().params
            assertThat(finished).containsEntry("event", PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
            assertThat(finished).doesNotContainKey("intent_id")
            assertThat(finished).containsEntry("status", "failed")
        }

    @Test
    fun `valid scoped PaymentIntent client secret includes its intent ID in confirmation analytics`() =
        runScenario {
            val clientSecret = "pi_example_scoped_secret_example"
            val intent = PaymentIntentFixtures.PI_SUCCEEDED.copy(clientSecret = clientSecret)
            repository.confirmPaymentIntentResult = Result.success(intent)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()
                viewModel.confirmStripeIntent(confirmPaymentIntentParams.copy(clientSecret = clientSecret), authHost)
                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Completed(intent))
            }
            assertThat(repository.confirmPaymentIntentCalls.awaitItem().params.clientSecret).isEqualTo(clientSecret)
            val started = analyticsRequestExecutor.requests.awaitItem().params
            assertThat(started).containsEntry("event", PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
            assertThat(started).containsEntry("intent_id", "pi_example")
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlNull.toString())
            val finished = analyticsRequestExecutor.requests.awaitItem().params
            assertThat(finished).containsEntry("event", PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
            assertThat(finished).containsEntry("intent_id", "pi_example")
        }

    @Test
    fun `valid SetupIntent client secret includes its intent ID in confirmation analytics`() =
        runScenario(isPaymentIntent = false) {
            val clientSecret = "seti_example_secret_example"
            val intent = SetupIntentFixtures.SI_SUCCEEDED.copy(clientSecret = clientSecret)
            repository.confirmSetupIntentResult = Result.success(intent)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()
                viewModel.confirmStripeIntent(confirmSetupIntentParams.copy(clientSecret = clientSecret), authHost)
                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Completed(intent))
            }
            assertThat(repository.confirmSetupIntentCalls.awaitItem().params.clientSecret).isEqualTo(clientSecret)
            val started = analyticsRequestExecutor.requests.awaitItem().params
            assertThat(started).containsEntry("event", PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
            assertThat(started).containsEntry("intent_id", "seti_example")
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlNull.toString())
            val finished = analyticsRequestExecutor.requests.awaitItem().params
            assertThat(finished).containsEntry("event", PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
            assertThat(finished).containsEntry("intent_id", "seti_example")
        }

    @Test
    fun `invalid client secret is omitted from next action analytics`() =
        runScenario {
            val clientSecret = "person@example.com"
            val error = IllegalArgumentException("Invalid client secret.")
            repository.retrieveStripeIntentResult = Result.failure(error)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()
                viewModel.handleNextActionForStripeIntent(clientSecret, authHost)
                val result = awaitItem() as InternalPaymentResult.Failed
                assertThat(result.throwable).isSameInstanceAs(error)
            }
            assertThat(repository.retrieveStripeIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(clientSecret, API_REQUEST_OPTIONS, emptyList())
            )
            val started = analyticsRequestExecutor.requests.awaitItem().params
            assertThat(started["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherNextActionStarted.toString())
            assertThat(started).doesNotContainKey("intent_id")
            val finished = analyticsRequestExecutor.requests.awaitItem().params
            assertThat(finished["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherNextActionFinished.toString())
            assertThat(finished).doesNotContainKey("intent_id")
        }

    @Test
    fun `verify confirm finished analytics includes duration parameter`() =
        runScenario {
            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)

                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))
            }
            val started = analyticsRequestExecutor.requests.awaitItem()
            assertThat(started.params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
            assertThat(started.params).containsEntry("publishable_key", ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlNull.toString())
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
            val finished = analyticsRequestExecutor.requests.awaitItem()
            assertThat(finished.params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
            assertThat(finished.params).containsEntry("duration", 1L)
            assertThat(finished.params).containsEntry("publishable_key", ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        }

    @Test
    fun `verify confirm finished analytics includes succeeded status for completed result`() =
        runScenario {
            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)

                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))
            }
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlNull.toString())
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
            val finished = analyticsRequestExecutor.requests.awaitItem()
            assertThat(finished.params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
            assertThat(finished.params).containsEntry("status", "succeeded")
        }

    @Test
    fun `verify confirm finished analytics includes failed status for failed result`() =
        runScenario {
            val error = APIConnectionException()
            repository.confirmPaymentIntentResult = Result.failure(error)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)

                assertThat((awaitItem() as InternalPaymentResult.Failed).throwable).isSameInstanceAs(error)
            }
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
            assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlNull.toString())
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
            val finished = analyticsRequestExecutor.requests.awaitItem()
            assertThat(finished.params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
            assertThat(finished.params).containsEntry("status", "failed")
        }

    @Test
    fun `verify confirm finished analytics includes canceled status for canceled result`() =
        runScenario {
            val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethod = null,
                nextActionData = null,
            )
            repository.retrievePaymentIntentResult = Result.success(intent)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.CANCELED))

                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Canceled)
            }
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
            val finished = analyticsRequestExecutor.requests.awaitItem()
            assertThat(finished.params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
            assertThat(finished.params).containsEntry("status", "canceled")
        }

    @Test
    fun `verify next action finished analytics includes duration parameter`() =
        runScenario {
            val intent = repository.retrieveStripeIntentResult.getOrThrow()
            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()

                viewModel.handleNextActionForStripeIntent(CLIENT_SECRET, authHost)
                viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.SUCCEEDED))

                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))
            }
            val started = analyticsRequestExecutor.requests.awaitItem()
            assertThat(started.params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherNextActionStarted.toString())
            assertThat(started.params).containsEntry("publishable_key", ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
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
            val finished = analyticsRequestExecutor.requests.awaitItem()
            assertThat(finished.params["event"])
                .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherNextActionFinished.toString())
            assertThat(finished.params).containsEntry("duration", 1L)
            assertThat(finished.params).containsEntry("publishable_key", ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        }

    @Test
    fun `verify only one finished event is sent when result is already set`() =
        runScenario {
            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()
                viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)
                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

                assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                    .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
                assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                    .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlNull.toString())
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
                val finished = analyticsRequestExecutor.requests.awaitItem()
                assertThat(finished.params["event"])
                    .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
                assertThat(finished.params).containsEntry("status", "succeeded")

                viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.SUCCEEDED))
                assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                    FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
                )
                expectNoEvents()
                analyticsRequestExecutor.requests.expectNoEvents()
            }
        }

    @Test
    fun `verify guard blocks different result types`() =
        runScenario {
            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()
                viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)
                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

                assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                    .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted.toString())
                assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                    .isEqualTo(PaymentAnalyticsEvent.ConfirmReturnUrlNull.toString())
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
                val finished = analyticsRequestExecutor.requests.awaitItem()
                assertThat(finished.params["event"])
                    .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished.toString())
                assertThat(finished.params).containsEntry("status", "succeeded")

                repository.retrievePaymentIntentResult = Result.success(
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(paymentMethod = null, nextActionData = null)
                )
                viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.FAILED))
                assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                    FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
                )
                expectNoEvents()
                analyticsRequestExecutor.requests.expectNoEvents()
            }
        }

    @Test
    fun `verify guard works for next action flows`() =
        runScenario {
            val intent = repository.retrieveStripeIntentResult.getOrThrow()
            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()
                viewModel.handleNextActionForStripeIntent(CLIENT_SECRET, authHost)
                viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.SUCCEEDED))
                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED))

                assertThat(analyticsRequestExecutor.requests.awaitItem().params["event"])
                    .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherNextActionStarted.toString())
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
                val finished = analyticsRequestExecutor.requests.awaitItem()
                assertThat(finished.params["event"])
                    .isEqualTo(PaymentAnalyticsEvent.PaymentLauncherNextActionFinished.toString())
                assertThat(finished.params).containsEntry("status", "succeeded")

                viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.SUCCEEDED))
                assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                    FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
                )
                expectNoEvents()
                analyticsRequestExecutor.requests.expectNoEvents()
            }
        }

    private fun runScenario(
        isPaymentIntent: Boolean = true,
        isInstantApp: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val repository = FakeStripeRepository()
        val nextActionHandlerRegistry = FakeNextActionHandlerRegistry()
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
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
            analyticsRequestExecutor = analyticsRequestExecutor,
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                packageManager = null,
                packageInfo = null,
                packageName = "com.stripe.test",
                publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
                networkTypeProvider = { null },
            ),
            uiContext = dispatcher,
            savedStateHandle = savedStateHandle,
            isInstantApp = isInstantApp,
            durationProvider = durationProvider,
        ).also { viewModelStoreRule.track(it) }

        Scenario(
            viewModel = viewModel,
            repository = repository,
            nextActionHandlerRegistry = nextActionHandlerRegistry,
            analyticsRequestExecutor = analyticsRequestExecutor,
            authHost = authHost,
            savedStateHandle = savedStateHandle,
            defaultReturnUrl = defaultReturnUrl,
        ).apply { block() }

        repository.ensureAllEventsConsumed()
        nextActionHandlerRegistry.ensureAllEventsConsumed()
        analyticsRequestExecutor.ensureAllEventsConsumed()
        pollingAnalyticsEventReporter.ensureAllEventsConsumed()
        authHost.ensureAllEventsConsumed()
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
        val repository: FakeStripeRepository,
        val nextActionHandlerRegistry: FakeNextActionHandlerRegistry,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
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
