package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.toElementsSessionParams
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.utils.FakePaymentSheetLoader
import com.stripe.android.view.ActivityScenarioFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@RunWith(RobolectricTestRunner::class)
class FlowControllerConfigurationHandlerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
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
        val configurationHandler = createConfigurationHandler()

        val exception = configurationHandler.configure(
            PaymentSheet.InitializationMode.PaymentIntent(PaymentSheetFixtures.CLIENT_SECRET),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        assertThat(exception).isNull()
        assertThat(viewModel.initializationMode).isNotNull()
        assertThat(viewModel.previousElementsSessionParams).isNotNull()
        assertThat(viewModel.paymentSelection).isEqualTo(PaymentSelection.Link)
        assertThat(viewModel.state).isNotNull()
        verify(eventReporter)
            .onInit(PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY)
    }

    @Test
    fun `configure() should not re-run initial config during second config`() = runTest {
        val elementsSessionParams = createElementsSessionParams()
        val configurationHandler = createConfigurationHandler()

        // Signaling we previously loaded elements session here.
        viewModel.previousElementsSessionParams = elementsSessionParams
        viewModel.paymentSelection = PaymentSelection.GooglePay

        val exception = configurationHandler.configure(
            PaymentSheet.InitializationMode.PaymentIntent(PaymentSheetFixtures.CLIENT_SECRET),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        assertThat(exception).isNull()
        assertThat(viewModel.previousElementsSessionParams).isSameInstanceAs(elementsSessionParams)
        assertThat(viewModel.paymentSelection).isEqualTo(PaymentSelection.GooglePay)

        // We're running ONLY the second config run, so we don't expect any interactions.
        verify(eventReporter, never())
            .onInit(PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY)
    }

    @Test
    fun `configure() should re-run CHANGED config during second config`() = runTest {
        val elementsSessionParams = createElementsSessionParams()
        val differentElementsSessionParams = createElementsSessionParams(PaymentSheetFixtures.DIFFERENT_CLIENT_SECRET)
        val configurationHandler = createConfigurationHandler()

        // Signaling we previously loaded elements session here.
        viewModel.previousElementsSessionParams = elementsSessionParams
        viewModel.paymentSelection = PaymentSelection.GooglePay

        val exception = configurationHandler.configure(
            PaymentSheet.InitializationMode.PaymentIntent(PaymentSheetFixtures.DIFFERENT_CLIENT_SECRET),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        assertThat(exception).isNull()
        assertThat(viewModel.initializationMode).isNotNull()
        assertThat(viewModel.previousElementsSessionParams).isNotSameInstanceAs(
            differentElementsSessionParams
        )
        assertThat(viewModel.paymentSelection).isEqualTo(PaymentSelection.Link)

        // We're running a new config, so we DO expect an interaction.
        verify(eventReporter)
            .onInit(PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY)
    }

    @Test
    fun `configure() with invalid paymentIntent`() = runTest {
        val configurationHandler = createConfigurationHandler()

        val exception = configurationHandler.configure(
            PaymentSheet.InitializationMode.PaymentIntent(" "),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        assertThat(exception?.message)
            .isEqualTo("The PaymentIntent client_secret cannot be an empty string.")
    }

    @Test
    fun `configure() with invalid merchant`() = runTest {
        val configurationHandler = createConfigurationHandler()

        val exception = configurationHandler.configure(
            PaymentSheet.InitializationMode.PaymentIntent(PaymentSheetFixtures.CLIENT_SECRET),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(merchantDisplayName = "")
        )

        assertThat(exception?.message)
            .isEqualTo("When a Configuration is passed to PaymentSheet, the Merchant display name cannot be an empty string.")
    }

    @Test
    fun `configure() with invalid customer id`() = runTest {
        val configurationHandler = createConfigurationHandler()

        val exception = configurationHandler.configure(
            PaymentSheet.InitializationMode.PaymentIntent(PaymentSheetFixtures.CLIENT_SECRET),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                customer = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.customer?.copy(
                    id = " "
                )
            )
        )

        assertThat(exception?.message)
            .isEqualTo("When a CustomerConfiguration is passed to PaymentSheet, the Customer ID cannot be an empty string.")
    }

    @Test
    fun `configure() with invalid customer ephemeral key`() = runTest {
        val configurationHandler = createConfigurationHandler()

        val exception = configurationHandler.configure(
            PaymentSheet.InitializationMode.PaymentIntent(PaymentSheetFixtures.CLIENT_SECRET),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                customer = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.customer?.copy(
                    ephemeralKeySecret = " "
                )
            )
        )
        assertThat(exception?.message)
            .isEqualTo("When a CustomerConfiguration is passed to PaymentSheet, the ephemeralKeySecret cannot be an empty string.")
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

    private fun createElementsSessionParams(
        clientSecret: String = PaymentSheetFixtures.CLIENT_SECRET,
        configuration: PaymentSheet.Configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
    ): ElementsSessionParams {
        val initializationMode = PaymentSheet.InitializationMode.PaymentIntent(clientSecret)
        return initializationMode.toElementsSessionParams(configuration)
    }

    private fun createConfigurationHandler(
        paymentSheetLoader: PaymentSheetLoader = defaultPaymentSheetLoader()
    ): FlowControllerConfigurationHandler {
        return FlowControllerConfigurationHandler(
            paymentSheetLoader = paymentSheetLoader,
            uiContext = testDispatcher,
            eventReporter = eventReporter,
            viewModel = viewModel,
        )
    }
}
