package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.utils.FakePaymentSheetLoader
import com.stripe.android.view.ActivityScenarioFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class FlowControllerConfigurationHandlerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val eventReporter = mock<EventReporter>()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityScenarioFactory = ActivityScenarioFactory(context)
    private lateinit var viewModel: FlowControllerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        val activityScenario = activityScenarioFactory.createAddPaymentMethodActivity()
        activityScenario.moveToState(Lifecycle.State.CREATED)
        activityScenario.onActivity { activity ->
            viewModel = ViewModelProvider(activity)[FlowControllerViewModel::class.java]
        }
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `configure() should run initial configuration`() = runTest {
        val configureErrors = Turbine<Throwable?>()
        val configurationHandler = createConfigurationHandler()

        configurationHandler.configure(
            PaymentSheet.InitializationMode.PaymentIntent(PaymentSheetFixtures.CLIENT_SECRET),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, exception ->
            configureErrors.add(exception)
        }

        assertThat(configureErrors.awaitItem()).isNull()
        assertThat(viewModel.previousConfigureRequest).isNotNull()
        assertThat(viewModel.paymentSelection).isEqualTo(PaymentSelection.Link)
        assertThat(viewModel.state).isNotNull()
        verify(eventReporter)
            .onInit(PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY)
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

        configurationHandler.configure(
            initializationMode = initializationMode,
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, exception ->
            configureErrors.add(exception)
        }

        assertThat(configureErrors.awaitItem()).isNull()
        assertThat(viewModel.previousConfigureRequest).isSameInstanceAs(configureRequest)
        assertThat(viewModel.paymentSelection).isEqualTo(PaymentSelection.GooglePay)

        // We're running ONLY the second config run, so we don't expect any interactions.
        verify(eventReporter, never())
            .onInit(PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY)
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
            initializationMode = newConfigureRequest.initializationMode,
            configuration = newConfigureRequest.configuration,
        ) { _, exception ->
            configureErrors.add(exception)
        }

        assertThat(configureErrors.awaitItem()).isNull()
        assertThat(viewModel.previousConfigureRequest).isEqualTo(newConfigureRequest)
        assertThat(viewModel.paymentSelection).isEqualTo(PaymentSelection.Link)

        // We're running a new config, so we DO expect an interaction.
        verify(eventReporter).onInit(PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY)
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
            initializationMode = newConfigureRequest.initializationMode,
            configuration = newConfigureRequest.configuration,
        ) { _, exception ->
            configureErrors.add(exception)
        }

        assertThat(configureErrors.awaitItem()).isNull()
        assertThat(viewModel.previousConfigureRequest).isEqualTo(newConfigureRequest)
        assertThat(viewModel.paymentSelection).isEqualTo(PaymentSelection.Link)

        // We're running a new config, so we DO expect an interaction.
        verify(eventReporter).onInit(PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY)
    }

    @Test
    fun `configure() with invalid paymentIntent`() = runTest {
        val configureErrors = Turbine<Throwable?>()
        val configurationHandler = createConfigurationHandler()

        configurationHandler.configure(
            PaymentSheet.InitializationMode.PaymentIntent(" "),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
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
            PaymentSheet.InitializationMode.PaymentIntent(PaymentSheetFixtures.CLIENT_SECRET),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(merchantDisplayName = "")
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
            PaymentSheet.InitializationMode.PaymentIntent(PaymentSheetFixtures.CLIENT_SECRET),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                customer = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.customer?.copy(
                    id = " "
                )
            )
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
            PaymentSheet.InitializationMode.PaymentIntent(PaymentSheetFixtures.CLIENT_SECRET),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                customer = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.customer?.copy(
                    ephemeralKeySecret = " "
                )
            )
        ) { _, error ->
            configureErrors.add(error)
        }

        assertThat(configureErrors.awaitItem()?.message)
            .isEqualTo("When a CustomerConfiguration is passed to PaymentSheet, the ephemeralKeySecret cannot be an empty string.")
    }

    @Test
    fun `configure() when scope is cancelled before completion should not call onInit lambda`() =
        runTest {
            var onInitCallbacks = 0

            val configurationHandler = createConfigurationHandler(
                FakePaymentSheetLoader(
                    customerPaymentMethods = emptyList(),
                    delay = 2.seconds,
                )
            )
            testScope.launch {
                configurationHandler.configure(
                    PaymentSheet.InitializationMode.PaymentIntent(PaymentSheetFixtures.CLIENT_SECRET),
                    null,
                ) { _, _ ->
                    onInitCallbacks++
                }
            }

            testScope.advanceTimeBy(500L)
            testScope.cancel()

            assertThat(onInitCallbacks).isEqualTo(0)
        }

    private fun defaultPaymentSheetLoader(): PaymentSheetLoader {
        return FakePaymentSheetLoader(
            customerPaymentMethods = emptyList(),
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentSelection = PaymentSelection.Link,
            linkState = LinkState(
                configuration = mock(),
                loginState = LinkState.LoginState.LoggedIn,
            ),
        )
    }

    private fun createInitializationMode(
        clientSecret: String = PaymentSheetFixtures.CLIENT_SECRET,
    ): PaymentSheet.InitializationMode {
        return PaymentSheet.InitializationMode.PaymentIntent(clientSecret)
    }

    private fun createConfigurationHandler(
        paymentSheetLoader: PaymentSheetLoader = defaultPaymentSheetLoader()
    ): FlowControllerConfigurationHandler {
        return FlowControllerConfigurationHandler(
            paymentSheetLoader = paymentSheetLoader,
            uiContext = testDispatcher,
            eventReporter = eventReporter,
            viewModel = viewModel,
            paymentSelectionUpdater = { _, newState -> newState.paymentSelection },
        )
    }
}
