package com.stripe.android.payments.paymentlauncher

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.StripeIntentResult
import com.stripe.android.analytics.FakeDurationProvider
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.payments.Clock
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.payments.SetupIntentFlowResultProcessor
import com.stripe.android.payments.paymentlauncher.PaymentLauncherViewModelTestFakes.FakeAuthActivityStarterHost
import com.stripe.android.payments.paymentlauncher.PaymentLauncherViewModelTestFakes.FakeNextActionHandler
import com.stripe.android.payments.paymentlauncher.PaymentLauncherViewModelTestFakes.FakeNextActionHandlerRegistry
import com.stripe.android.payments.paymentlauncher.PaymentLauncherViewModelTestFakes.FakeStripeRepository
import com.stripe.android.testing.FakePollingAnalyticsEventReporter
import com.stripe.android.testing.ViewModelStoreTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class PaymentLauncherViewModelAnalyticsTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @get:Rule
    val networkRule = NetworkRule(
        hostsToTrack = listOf(AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )

    private val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
        packageManager = null,
        packageInfo = null,
        packageName = "com.stripe.test",
        publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
        networkTypeProvider = { null },
    )

    @Test
    fun `verify confirm finished analytics includes duration parameter`() =
        runScenario {
            val intent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
            repository.confirmPaymentIntentResult = Result.success(intent)
            expectEvent(
                PaymentAnalyticsEvent.PaymentLauncherConfirmStarted,
                analyticsPayloadField("intent_id", requireNotNull(PaymentIntentFixtures.PI_SUCCEEDED.id)),
                analyticsPayloadField("payment_method_type", "card"),
            )
            expectEvent(PaymentAnalyticsEvent.ConfirmReturnUrlNull)

            viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)
            assertThat(repository.confirmPaymentIntentCalls.awaitItem().params.clientSecret).isEqualTo(CLIENT_SECRET)
            assertThat(nextActionHandlerRegistry.getNextActionHandlerCalls.awaitItem()).isEqualTo(intent)
            assertThat(nextActionHandlerRegistry.handler.nextActionCalls.awaitItem()).isEqualTo(
                FakeNextActionHandler.NextActionCall(authHost, intent, API_REQUEST_OPTIONS)
            )
            awaitIdle()
            networkRule.validate()

            expectEvent(
                PaymentAnalyticsEvent.PaymentLauncherConfirmFinished,
                analyticsPayloadField("duration", "1"),
            )
            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.SUCCEEDED))
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
        }

    @Test
    fun `verify confirm finished analytics includes succeeded status for completed result`() =
        runScenario {
            expectEvent(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted)
            expectEvent(PaymentAnalyticsEvent.ConfirmReturnUrlNull)
            expectEvent(
                PaymentAnalyticsEvent.PaymentLauncherConfirmFinished,
                analyticsPayloadField("status", "succeeded"),
            )

            viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)
            assertThat(repository.confirmPaymentIntentCalls.awaitItem().params.clientSecret).isEqualTo(CLIENT_SECRET)
        }

    @Test
    fun `verify confirm finished analytics includes failed status for failed result`() =
        runScenario {
            repository.confirmPaymentIntentResult = Result.failure(APIConnectionException())
            expectEvent(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted)
            expectEvent(PaymentAnalyticsEvent.ConfirmReturnUrlNull)
            expectEvent(
                PaymentAnalyticsEvent.PaymentLauncherConfirmFinished,
                analyticsPayloadField("status", "failed"),
            )

            viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)
            assertThat(repository.confirmPaymentIntentCalls.awaitItem().params.clientSecret).isEqualTo(CLIENT_SECRET)
        }

    @Test
    fun `verify SetupIntent confirmation failure sends failed confirmation analytics`() =
        runScenario(isPaymentIntent = false) {
            repository.confirmSetupIntentResult = Result.failure(APIConnectionException())
            expectEvent(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted)
            expectEvent(PaymentAnalyticsEvent.ConfirmReturnUrlNull)
            expectEvent(
                PaymentAnalyticsEvent.PaymentLauncherConfirmFinished,
                analyticsPayloadField("status", "failed"),
            )

            viewModel.confirmStripeIntent(confirmSetupIntentParams, authHost)
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
    fun `verify confirm finished analytics includes canceled status for canceled result`() =
        runScenario {
            repository.retrievePaymentIntentResult = Result.success(
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(paymentMethod = null, nextActionData = null)
            )
            expectEvent(
                PaymentAnalyticsEvent.PaymentLauncherConfirmFinished,
                analyticsPayloadField("status", "canceled"),
            )

            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.CANCELED))
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
        }

    @Test
    fun `verify next action finished analytics includes duration parameter`() =
        runScenario {
            val intent = repository.retrieveStripeIntentResult.getOrThrow()
            expectEvent(
                PaymentAnalyticsEvent.PaymentLauncherNextActionStarted,
                analyticsPayloadField("intent_id", requireNotNull(PaymentIntentFixtures.PI_SUCCEEDED.id)),
            )

            viewModel.handleNextActionForStripeIntent(CLIENT_SECRET, authHost)
            assertThat(repository.retrieveStripeIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, emptyList())
            )
            assertThat(nextActionHandlerRegistry.getNextActionHandlerCalls.awaitItem()).isEqualTo(intent)
            assertThat(nextActionHandlerRegistry.handler.nextActionCalls.awaitItem()).isEqualTo(
                FakeNextActionHandler.NextActionCall(authHost, intent, API_REQUEST_OPTIONS)
            )
            awaitIdle()
            networkRule.validate()

            expectEvent(
                PaymentAnalyticsEvent.PaymentLauncherNextActionFinished,
                analyticsPayloadField("duration", "1"),
            )
            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.SUCCEEDED))
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
        }

    @Test
    fun `verify next action retrieval failure sends failed next action analytics`() =
        runScenario {
            repository.retrieveStripeIntentResult = Result.failure(APIConnectionException())
            expectEvent(PaymentAnalyticsEvent.PaymentLauncherNextActionStarted)
            expectEvent(
                PaymentAnalyticsEvent.PaymentLauncherNextActionFinished,
                analyticsPayloadField("status", "failed"),
            )

            viewModel.handleNextActionForStripeIntent(CLIENT_SECRET, authHost)
            assertThat(repository.retrieveStripeIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, emptyList())
            )
        }

    @Test
    fun `verify custom confirmation return URL sends custom return URL analytics`() =
        runScenario(isPaymentIntent = false) {
            val intent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT
            repository.confirmSetupIntentResult = Result.success(intent)
            confirmSetupIntentParams.returnUrl = RETURN_URL
            expectEvent(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted)
            expectEvent(PaymentAnalyticsEvent.ConfirmReturnUrlCustom)

            viewModel.confirmStripeIntent(confirmSetupIntentParams, authHost)
            assertThat(repository.confirmSetupIntentCalls.awaitItem().params.returnUrl).isEqualTo(RETURN_URL)
            assertThat(nextActionHandlerRegistry.getNextActionHandlerCalls.awaitItem()).isEqualTo(intent)
            assertThat(nextActionHandlerRegistry.handler.nextActionCalls.awaitItem()).isEqualTo(
                FakeNextActionHandler.NextActionCall(authHost, intent, API_REQUEST_OPTIONS)
            )
        }

    @Test
    fun `verify default confirmation return URL sends default return URL analytics`() =
        runScenario {
            confirmPaymentIntentParams.returnUrl = defaultReturnUrl.value
            expectEvent(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted)
            expectEvent(PaymentAnalyticsEvent.ConfirmReturnUrlDefault)
            expectEvent(PaymentAnalyticsEvent.PaymentLauncherConfirmFinished)

            viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)
            assertThat(repository.confirmPaymentIntentCalls.awaitItem().params.returnUrl)
                .isEqualTo(defaultReturnUrl.value)
        }

    @Test
    fun `verify pending SetupIntent confirmation sends started analytics without finished analytics`() =
        runScenario(isPaymentIntent = false) {
            val intent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT
            repository.confirmSetupIntentResult = Result.success(intent)
            expectEvent(
                PaymentAnalyticsEvent.PaymentLauncherConfirmStarted,
                analyticsPayloadField("intent_id", requireNotNull(SetupIntentFixtures.SI_SUCCEEDED.id)),
                analyticsPayloadField("payment_method_type", "card"),
            )
            expectEvent(PaymentAnalyticsEvent.ConfirmReturnUrlNull)

            viewModel.confirmStripeIntent(confirmSetupIntentParams, authHost)
            assertThat(repository.confirmSetupIntentCalls.awaitItem().params.clientSecret)
                .isEqualTo(SETUP_CLIENT_SECRET)
            assertThat(nextActionHandlerRegistry.getNextActionHandlerCalls.awaitItem()).isEqualTo(intent)
            assertThat(nextActionHandlerRegistry.handler.nextActionCalls.awaitItem()).isEqualTo(
                FakeNextActionHandler.NextActionCall(authHost, intent, API_REQUEST_OPTIONS)
            )
        }

    @Test
    fun `verify next action with supplied intent sends finished analytics without started analytics`() =
        runScenario {
            val intent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
            viewModel.handleNextActionForStripeIntent(intent, authHost)
            assertThat(nextActionHandlerRegistry.getNextActionHandlerCalls.awaitItem()).isEqualTo(intent)
            assertThat(nextActionHandlerRegistry.handler.nextActionCalls.awaitItem()).isEqualTo(
                FakeNextActionHandler.NextActionCall(authHost, intent, API_REQUEST_OPTIONS)
            )
            awaitIdle()
            networkRule.validate()

            expectEvent(
                PaymentAnalyticsEvent.PaymentLauncherNextActionFinished,
                analyticsPayloadField("status", "succeeded"),
            )
            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.SUCCEEDED))
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
        }

    @Test
    fun `verify only one finished event is sent when result is already set`() =
        runScenario {
            expectEvent(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted)
            expectEvent(PaymentAnalyticsEvent.ConfirmReturnUrlNull)
            expectEvent(
                PaymentAnalyticsEvent.PaymentLauncherConfirmFinished,
                analyticsPayloadField("status", "succeeded"),
            )

            viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)
            assertThat(repository.confirmPaymentIntentCalls.awaitItem().params.clientSecret).isEqualTo(CLIENT_SECRET)
            awaitIdle()
            networkRule.validate()

            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.SUCCEEDED))
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
        }

    @Test
    fun `verify guard blocks finished analytics for different result types`() =
        runScenario {
            expectEvent(PaymentAnalyticsEvent.PaymentLauncherConfirmStarted)
            expectEvent(PaymentAnalyticsEvent.ConfirmReturnUrlNull)
            expectEvent(
                PaymentAnalyticsEvent.PaymentLauncherConfirmFinished,
                analyticsPayloadField("status", "succeeded"),
            )

            viewModel.confirmStripeIntent(confirmPaymentIntentParams, authHost)
            assertThat(repository.confirmPaymentIntentCalls.awaitItem().params.clientSecret).isEqualTo(CLIENT_SECRET)
            awaitIdle()
            networkRule.validate()

            repository.retrievePaymentIntentResult = Result.success(
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(paymentMethod = null, nextActionData = null)
            )
            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.FAILED))
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
        }

    @Test
    fun `verify guard sends only one finished analytics event for next action flows`() =
        runScenario {
            val intent = repository.retrieveStripeIntentResult.getOrThrow()
            expectEvent(PaymentAnalyticsEvent.PaymentLauncherNextActionStarted)
            viewModel.handleNextActionForStripeIntent(CLIENT_SECRET, authHost)
            assertThat(repository.retrieveStripeIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, emptyList())
            )
            assertThat(nextActionHandlerRegistry.getNextActionHandlerCalls.awaitItem()).isEqualTo(intent)
            assertThat(nextActionHandlerRegistry.handler.nextActionCalls.awaitItem()).isEqualTo(
                FakeNextActionHandler.NextActionCall(authHost, intent, API_REQUEST_OPTIONS)
            )
            awaitIdle()
            networkRule.validate()

            expectEvent(
                PaymentAnalyticsEvent.PaymentLauncherNextActionFinished,
                analyticsPayloadField("status", "succeeded"),
            )
            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.SUCCEEDED))
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
            awaitIdle()
            networkRule.validate()

            viewModel.onPaymentFlowResult(paymentFlowResult(StripeIntentResult.Outcome.SUCCEEDED))
            assertThat(repository.retrievePaymentIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(CLIENT_SECRET, API_REQUEST_OPTIONS, EXPAND_PAYMENT_METHOD)
            )
        }

    @Test
    fun `invalid PaymentIntent client secret is omitted from confirmation analytics`() =
        runScenario {
            val clientSecret = "person@example.com"
            val error = IllegalArgumentException("Invalid PaymentIntent client secret.")
            repository.confirmPaymentIntentResult = Result.failure(error)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()
                expectEvent(
                    PaymentAnalyticsEvent.PaymentLauncherConfirmStarted,
                    RequestMatcher { !it.queryParams.containsKey("intent_id") },
                )
                expectEvent(PaymentAnalyticsEvent.ConfirmReturnUrlNull)
                expectEvent(
                    PaymentAnalyticsEvent.PaymentLauncherConfirmFinished,
                    RequestMatcher { !it.queryParams.containsKey("intent_id") },
                    analyticsPayloadField("status", "failed"),
                )

                viewModel.confirmStripeIntent(confirmPaymentIntentParams.copy(clientSecret = clientSecret), authHost)
                val result = awaitItem() as InternalPaymentResult.Failed
                assertThat(result.throwable).isSameInstanceAs(error)
            }
            assertThat(repository.confirmPaymentIntentCalls.awaitItem().params.clientSecret).isEqualTo(clientSecret)
        }

    @Test
    fun `invalid SetupIntent client secret is omitted from confirmation analytics`() =
        runScenario(isPaymentIntent = false) {
            val clientSecret = "person@example.com_secret_invalid"
            val error = IllegalArgumentException("Invalid SetupIntent client secret.")
            repository.confirmSetupIntentResult = Result.failure(error)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()
                expectEvent(
                    PaymentAnalyticsEvent.PaymentLauncherConfirmStarted,
                    RequestMatcher { !it.queryParams.containsKey("intent_id") },
                )
                expectEvent(PaymentAnalyticsEvent.ConfirmReturnUrlNull)
                expectEvent(
                    PaymentAnalyticsEvent.PaymentLauncherConfirmFinished,
                    RequestMatcher { !it.queryParams.containsKey("intent_id") },
                    analyticsPayloadField("status", "failed"),
                )

                viewModel.confirmStripeIntent(confirmSetupIntentParams.copy(clientSecret = clientSecret), authHost)
                val result = awaitItem() as InternalPaymentResult.Failed
                assertThat(result.throwable).isSameInstanceAs(error)
            }
            assertThat(repository.confirmSetupIntentCalls.awaitItem().params.clientSecret).isEqualTo(clientSecret)
        }

    @Test
    fun `valid scoped PaymentIntent client secret includes its intent ID in confirmation analytics`() =
        runScenario {
            val clientSecret = "pi_example_scoped_secret_example"
            val intent = PaymentIntentFixtures.PI_SUCCEEDED.copy(clientSecret = clientSecret)
            repository.confirmPaymentIntentResult = Result.success(intent)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()
                expectEvent(
                    PaymentAnalyticsEvent.PaymentLauncherConfirmStarted,
                    analyticsPayloadField("intent_id", "pi_example"),
                )
                expectEvent(PaymentAnalyticsEvent.ConfirmReturnUrlNull)
                expectEvent(
                    PaymentAnalyticsEvent.PaymentLauncherConfirmFinished,
                    analyticsPayloadField("intent_id", "pi_example"),
                )

                viewModel.confirmStripeIntent(confirmPaymentIntentParams.copy(clientSecret = clientSecret), authHost)
                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Completed(intent))
            }
            assertThat(repository.confirmPaymentIntentCalls.awaitItem().params.clientSecret).isEqualTo(clientSecret)
        }

    @Test
    fun `valid SetupIntent client secret includes its intent ID in confirmation analytics`() =
        runScenario(isPaymentIntent = false) {
            val clientSecret = "seti_example_secret_example"
            val intent = SetupIntentFixtures.SI_SUCCEEDED.copy(clientSecret = clientSecret)
            repository.confirmSetupIntentResult = Result.success(intent)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()
                expectEvent(
                    PaymentAnalyticsEvent.PaymentLauncherConfirmStarted,
                    analyticsPayloadField("intent_id", "seti_example"),
                )
                expectEvent(PaymentAnalyticsEvent.ConfirmReturnUrlNull)
                expectEvent(
                    PaymentAnalyticsEvent.PaymentLauncherConfirmFinished,
                    analyticsPayloadField("intent_id", "seti_example"),
                )

                viewModel.confirmStripeIntent(confirmSetupIntentParams.copy(clientSecret = clientSecret), authHost)
                assertThat(awaitItem()).isEqualTo(InternalPaymentResult.Completed(intent))
            }
            assertThat(repository.confirmSetupIntentCalls.awaitItem().params.clientSecret).isEqualTo(clientSecret)
        }

    @Test
    fun `invalid client secret is omitted from next action analytics`() =
        runScenario {
            val clientSecret = "person@example.com"
            val error = IllegalArgumentException("Invalid client secret.")
            repository.retrieveStripeIntentResult = Result.failure(error)

            viewModel.internalPaymentResult.test {
                assertThat(awaitItem()).isNull()
                expectEvent(
                    PaymentAnalyticsEvent.PaymentLauncherNextActionStarted,
                    RequestMatcher { !it.queryParams.containsKey("intent_id") },
                )
                expectEvent(
                    PaymentAnalyticsEvent.PaymentLauncherNextActionFinished,
                    RequestMatcher { !it.queryParams.containsKey("intent_id") },
                )

                viewModel.handleNextActionForStripeIntent(clientSecret, authHost)
                val result = awaitItem() as InternalPaymentResult.Failed
                assertThat(result.throwable).isSameInstanceAs(error)
            }
            assertThat(repository.retrieveStripeIntentCalls.awaitItem()).isEqualTo(
                FakeStripeRepository.RetrieveIntentCall(clientSecret, API_REQUEST_OPTIONS, emptyList())
            )
        }

    private fun expectEvent(event: PaymentAnalyticsEvent, vararg matchers: RequestMatcher) {
        networkRule.enqueue(
            host("q.stripe.com"),
            method("GET"),
            analyticsPayloadField("event", event.toString()),
            analyticsPayloadField("publishable_key", ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            *matchers,
            applyDefaultAuthorization = false,
        ) { response ->
            response.status = "HTTP/1.1 200 OK"
        }
    }

    private fun runScenario(
        isPaymentIntent: Boolean = true,
        isInstantApp: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val repository = FakeStripeRepository()
        val nextActionHandlerRegistry = FakeNextActionHandlerRegistry()
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

        coroutineScope {
            val viewModel = PaymentLauncherViewModel(
                isPaymentIntent = isPaymentIntent,
                stripeApiRepository = repository,
                nextActionHandlerRegistry = nextActionHandlerRegistry,
                defaultReturnUrl = defaultReturnUrl,
                apiRequestOptionsProvider = { API_REQUEST_OPTIONS },
                lazyPaymentIntentFlowResultProcessor = { paymentIntentFlowResultProcessor },
                lazySetupIntentFlowResultProcessor = { setupIntentFlowResultProcessor },
                analyticsRequestExecutor = DefaultAnalyticsRequestExecutor(
                    stripeNetworkClient = DefaultStripeNetworkClient(workContext = Dispatchers.IO),
                    workContext = coroutineContext + Dispatchers.IO,
                    logger = Logger.noop(),
                ),
                paymentAnalyticsRequestFactory = analyticsRequestFactory,
                uiContext = dispatcher,
                savedStateHandle = savedStateHandle,
                isInstantApp = isInstantApp,
                durationProvider = FakeDurationProvider(),
            ).also { viewModelStoreRule.track(it) }

            Scenario(
                viewModel = viewModel,
                repository = repository,
                nextActionHandlerRegistry = nextActionHandlerRegistry,
                authHost = authHost,
                defaultReturnUrl = defaultReturnUrl,
                analyticsScope = this,
            ).apply {
                block()
                awaitIdle()
            }
        }

        repository.ensureAllEventsConsumed()
        nextActionHandlerRegistry.ensureAllEventsConsumed()
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
            API_CONFIGURATION
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
        val authHost: FakeAuthActivityStarterHost,
        val defaultReturnUrl: DefaultReturnUrl,
        val analyticsScope: CoroutineScope,
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

        suspend fun awaitIdle() {
            viewModel.viewModelScope.coroutineContext.job.children.toList().joinAll()
            analyticsScope.coroutineContext.job.children.toList().joinAll()
        }
    }

    private companion object {
        val CLIENT_SECRET = requireNotNull(PaymentIntentFixtures.PI_SUCCEEDED.clientSecret)
        val SETUP_CLIENT_SECRET = requireNotNull(SetupIntentFixtures.SI_SUCCEEDED.clientSecret)
        const val PM_ID = "pm_12345"
        const val RETURN_URL = "return://to.me"
        const val TEST_STRIPE_ACCOUNT_ID = "acct_123"
        val EXPAND_PAYMENT_METHOD = listOf("payment_method")
        val API_CONFIGURATION = ApiConfiguration.State(
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
        )
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
