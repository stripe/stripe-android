package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.SessionTestRule
import com.stripe.android.utils.FakePaymentElementLoader
import com.stripe.android.utils.PaymentElementCallbackTestRule
import com.stripe.android.utils.RelayingPaymentElementLoader
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class FlowControllerConfigurationHandlerTest {

    @get:Rule
    val paymentElementCallbackTestRule = PaymentElementCallbackTestRule()

    @get:Rule
    val sessionRule = SessionTestRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val eventReporter = mock<EventReporter>()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var viewModel: FlowControllerViewModel

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @Before
    fun setup() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        viewModel = FlowControllerViewModel(
            application = ApplicationProvider.getApplicationContext(),
            handle = SavedStateHandle(),
            paymentElementCallbackIdentifier = "FlowControllerTestIdentifier",
            statusBarColor = null,
        )
    }

    @Test
    fun `configure() should run initial configuration`() = runTest {
        val configureErrors = Turbine<Throwable?>()
        val configurationHandler = createConfigurationHandler()

        val beforeSessionId = AnalyticsRequestFactory.sessionId
        configurationHandler.configure(
            scope = this,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
            ),
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            initializedViaCompose = false,
        ) { _, exception ->
            configureErrors.add(exception)
        }

        assertThat(configureErrors.awaitItem()).isNull()
        assertThat(viewModel.previousConfigureRequest).isNotNull()
        assertThat(configurationHandler.isConfigured).isTrue()
        assertThat(viewModel.paymentSelection).isEqualTo(PaymentSelection.Link())
        assertThat(viewModel.state).isNotNull()
        verify(eventReporter).onInit(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            isDeferred = false,
        )
        // Configure should regenerate the analytics sessionId.
        assertThat(beforeSessionId).isNotEqualTo(AnalyticsRequestFactory.sessionId)
    }

    @Test
    fun `configure() should not re-run initial config during second config`() = runTest {
        val configureErrors = Turbine<Throwable?>()
        val configurationHandler = createConfigurationHandler()

        val initializationMode = createInitializationMode()
        val configureRequest = FlowControllerConfigurationHandler.ConfigureRequest(
            initializationMode = initializationMode,
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
        )

        // Signaling we previously loaded elements session here.
        viewModel.previousConfigureRequest = configureRequest
        viewModel.paymentSelection = PaymentSelection.GooglePay

        val beforeSessionId = AnalyticsRequestFactory.sessionId
        configurationHandler.configure(
            scope = this,
            initializationMode = initializationMode,
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            initializedViaCompose = false,
        ) { _, exception ->
            configureErrors.add(exception)
        }

        assertThat(configureErrors.awaitItem()).isNull()
        assertThat(viewModel.previousConfigureRequest).isSameInstanceAs(configureRequest)
        assertThat(configurationHandler.isConfigured).isTrue()
        assertThat(viewModel.paymentSelection).isEqualTo(PaymentSelection.GooglePay)

        // We're running ONLY the second config run, so we don't expect any interactions.
        verify(eventReporter, never()).onInit(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            isDeferred = false,
        )

        // Configure should not regenerate the analytics sessionId when using the same configuration.
        assertThat(beforeSessionId).isEqualTo(AnalyticsRequestFactory.sessionId)
    }

    @Test
    fun `configure() should re-run CHANGED config if the initialization mode changed`() = runTest {
        val configureErrors = Turbine<Throwable?>()
        val configurationHandler = createConfigurationHandler()

        // Signaling we previously loaded elements session here.
        viewModel.previousConfigureRequest = FlowControllerConfigurationHandler.ConfigureRequest(
            initializationMode = createInitializationMode(),
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
        )
        viewModel.paymentSelection = PaymentSelection.GooglePay

        val newConfigureRequest = FlowControllerConfigurationHandler.ConfigureRequest(
            initializationMode = createInitializationMode(PaymentSheetFixtures.DIFFERENT_CLIENT_SECRET),
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
        )

        configurationHandler.configure(
            scope = this,
            initializationMode = newConfigureRequest.initializationMode,
            configuration = newConfigureRequest.configuration,
            initializedViaCompose = false,
        ) { _, exception ->
            configureErrors.add(exception)
        }

        assertThat(configureErrors.awaitItem()).isNull()
        assertThat(viewModel.previousConfigureRequest).isEqualTo(newConfigureRequest)
        assertThat(configurationHandler.isConfigured).isTrue()
        assertThat(viewModel.paymentSelection).isEqualTo(PaymentSelection.Link())

        // We're running a new config, so we DO expect an interaction.
        verify(eventReporter).onInit(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            isDeferred = false,
        )
    }

    @Test
    fun `configure() should re-run CHANGED config if the payment sheet config changed`() = runTest {
        val configureErrors = Turbine<Throwable?>()
        val configurationHandler = createConfigurationHandler()
        val initializationMode = createInitializationMode()

        // Signaling we previously loaded elements session here.
        viewModel.previousConfigureRequest = FlowControllerConfigurationHandler.ConfigureRequest(
            initializationMode = initializationMode,
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER,
        )
        viewModel.paymentSelection = PaymentSelection.GooglePay

        val newConfigureRequest = FlowControllerConfigurationHandler.ConfigureRequest(
            initializationMode = initializationMode,
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
        )

        configurationHandler.configure(
            scope = this,
            initializationMode = newConfigureRequest.initializationMode,
            configuration = newConfigureRequest.configuration,
            initializedViaCompose = false,
        ) { _, exception ->
            configureErrors.add(exception)
        }

        assertThat(configureErrors.awaitItem()).isNull()
        assertThat(viewModel.previousConfigureRequest).isEqualTo(newConfigureRequest)
        assertThat(configurationHandler.isConfigured).isTrue()
        assertThat(viewModel.paymentSelection).isEqualTo(PaymentSelection.Link())

        // We're running a new config, so we DO expect an interaction.
        verify(eventReporter).onInit(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            isDeferred = false,
        )
    }

    @Test
    fun `configure() with invalid paymentIntent`() = runTest {
        val configureErrors = Turbine<Throwable?>()
        val configurationHandler = createConfigurationHandler()

        configurationHandler.configure(
            scope = this,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(" "),
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            initializedViaCompose = false,
        ) { _, error ->
            configureErrors.add(error)
        }

        assertThat(configureErrors.awaitItem()?.message)
            .isEqualTo("The PaymentIntent client_secret cannot be an empty string.")
    }

    @Test
    fun `configure() with invalid merchant`() = runTest {
        val configureErrors = Turbine<Throwable?>()
        val configurationHandler = createConfigurationHandler()

        configurationHandler.configure(
            scope = this,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
            ),
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                merchantDisplayName = "",
            ),
            initializedViaCompose = false,
        ) { _, error ->
            configureErrors.add(error)
        }

        assertThat(configureErrors.awaitItem()?.message)
            .isEqualTo("When a Configuration is passed to PaymentSheet, the Merchant display name cannot be an empty string.")
    }

    @Test
    fun `configure() with invalid customer id`() = runTest {
        val configureErrors = Turbine<Throwable?>()
        val configurationHandler = createConfigurationHandler()

        configurationHandler.configure(
            scope = this,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
            ),
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                customer = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.customer?.copy(
                    id = " "
                )
            ),
            initializedViaCompose = false,
        ) { _, error ->
            configureErrors.add(error)
        }

        assertThat(configureErrors.awaitItem()?.message)
            .isEqualTo("When a CustomerConfiguration is passed to PaymentSheet, the Customer ID cannot be an empty string.")
    }

    @Test
    fun `configure() with invalid customer ephemeral key`() = runTest {
        val configureErrors = Turbine<Throwable?>()
        val configurationHandler = createConfigurationHandler()

        configurationHandler.configure(
            scope = this,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
            ),
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                customer = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.customer?.copy(
                    ephemeralKeySecret = " "
                )
            ),
            initializedViaCompose = false,
        ) { _, error ->
            configureErrors.add(error)
        }

        assertThat(configureErrors.awaitItem()?.message)
            .isEqualTo("Conflicting ephemeralKeySecrets between CustomerConfiguration and CustomerConfiguration.customerAccessType")
    }

    @Test
    fun `configure() when scope is cancelled before completion should not call onInit lambda`() =
        runTest {
            var onInitCallbacks = 0

            val configurationHandler = createConfigurationHandler(
                FakePaymentElementLoader(
                    customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
                    delay = 2.seconds,
                )
            )
            testScope.launch {
                configurationHandler.configure(
                    scope = this,
                    initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                        clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
                    ),
                    configuration = PaymentSheet.Configuration("Some name"),
                    initializedViaCompose = false,
                ) { _, _ ->
                    onInitCallbacks++
                }
            }

            testScope.advanceTimeBy(500L)
            testScope.cancel()

            assertThat(onInitCallbacks).isEqualTo(0)
        }

    @Test
    fun `Cancels current configure job if new call to configure comes in`() = runTest {
        val loader = RelayingPaymentElementLoader()
        val configurationHandler = createConfigurationHandler(paymentElementLoader = loader)

        val amounts = (0 until 10).map { index ->
            1_000L + index * 200L
        }

        val resultTurbine = Turbine<Long>()

        for (amount in amounts) {
            configurationHandler.configure(
                scope = this,
                initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = amount,
                            currency = "cad",
                        )
                    )
                ),
                configuration = PaymentSheet.Configuration("Some name"),
                initializedViaCompose = false,
                callback = { _, _ ->
                    resultTurbine.add(amount)
                },
            )
        }

        loader.enqueueSuccess()

        val completedCall = resultTurbine.awaitItem()
        assertThat(completedCall).isEqualTo(amounts.last())

        resultTurbine.expectNoEvents()
    }

    @Test
    fun `Cancels current configure job if coroutine scope is canceled`() = runTest {
        val loader = RelayingPaymentElementLoader()
        val configurationHandler = createConfigurationHandler(paymentElementLoader = loader)

        val resultTurbine = Turbine<Unit>()

        configurationHandler.configure(
            scope = testScope,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
            ),
            configuration = PaymentSheet.Configuration("Some name"),
            initializedViaCompose = false,
            callback = { _, _ ->
                resultTurbine.add(Unit)
            },
        )

        testScope.cancel()
        loader.enqueueSuccess()

        resultTurbine.expectNoEvents()
    }

    @Test
    fun `Records configuration failure correctly in view model`() = runTest {
        val resultTurbine = Turbine<Throwable?>()
        val configurationHandler = createConfigurationHandler(
            paymentElementLoader = FakePaymentElementLoader(shouldFail = true),
        )

        configurationHandler.configure(
            scope = this,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
            ),
            configuration = PaymentSheet.Configuration("Some name"),
            initializedViaCompose = false,
        ) { _, exception ->
            resultTurbine.add(exception)
        }

        assertThat(resultTurbine.awaitItem()).isNotNull()
        assertThat(configurationHandler.isConfigured).isFalse()
    }

    @Test
    fun `Sends correct analytics event when using normal intent`() = runTest {
        val configureTurbine = Turbine<Throwable?>()
        val configurationHandler = createConfigurationHandler()

        configurationHandler.configure(
            scope = this,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("pi_123_sk_456"),
            configuration = PaymentSheet.Configuration("Some name"),
            initializedViaCompose = false,
        ) { _, _ ->
            configureTurbine.close()
        }

        configureTurbine.awaitComplete()

        verify(eventReporter).onInit(
            configuration = any(),
            isDeferred = eq(false),
        )
    }

    @Test
    fun `Sends correct analytics event when using deferred intent with client-side confirmation`() = runTest {
        val configureTurbine = Turbine<Throwable?>()
        val configurationHandler = createConfigurationHandler()

        configurationHandler.configure(
            scope = this,
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                ),
            ),
            configuration = PaymentSheet.Configuration("Some name"),
            initializedViaCompose = false,
        ) { _, _ ->
            configureTurbine.close()
        }

        configureTurbine.awaitComplete()

        verify(eventReporter).onInit(
            configuration = any(),
            isDeferred = eq(true),
        )
    }

    @Test
    fun `Sends correct analytics event when using deferred intent with server-side confirmation`() = runTest {
        val configureTurbine = Turbine<Throwable?>()
        val configurationHandler = createConfigurationHandler()

        configurationHandler.configure(
            scope = this,
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                ),
            ),
            configuration = PaymentSheet.Configuration("Some name"),
            initializedViaCompose = false,
        ) { _, _ ->
            configureTurbine.close()
        }

        configureTurbine.awaitComplete()

        verify(eventReporter).onInit(
            configuration = any(),
            isDeferred = eq(true),
        )
    }

    private fun defaultPaymentSheetLoader(): PaymentElementLoader {
        return FakePaymentElementLoader(
            customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Link(),
            linkState = LinkState(
                configuration = mock(),
                loginState = LinkState.LoginState.LoggedIn,
                signupMode = null,
            ),
        )
    }

    private fun createInitializationMode(
        clientSecret: String = PaymentSheetFixtures.CLIENT_SECRET,
    ): PaymentElementLoader.InitializationMode {
        return PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret)
    }

    private fun createConfigurationHandler(
        paymentElementLoader: PaymentElementLoader = defaultPaymentSheetLoader()
    ): FlowControllerConfigurationHandler {
        return FlowControllerConfigurationHandler(
            paymentElementLoader = paymentElementLoader,
            uiContext = testDispatcher,
            eventReporter = eventReporter,
            viewModel = viewModel,
            paymentSelectionUpdater = { _, _, newState, _ -> newState.paymentSelection },
        )
    }
}
