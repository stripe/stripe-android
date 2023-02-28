package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.SavedSelection
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
    private lateinit var activity: ComponentActivity

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        val activityScenario = activityScenarioFactory.createAddPaymentMethodActivity()
        activityScenario.moveToState(Lifecycle.State.CREATED)
        activityScenario.onActivity {
            activity = it
        }
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful configure() should fire analytics event`() = runTest {
        val configurationHandler = createConfigurationHandler()
        configurationHandler.configure(
            PaymentSheet.InitializationMode.PaymentIntent(PaymentSheetFixtures.CLIENT_SECRET),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }
        verify(eventReporter)
            .onInit(PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY)
    }

    @Test
    fun `configure() with invalid paymentIntent`() = runTest {
        var result = Pair<Boolean, Throwable?>(true, null)
        val configurationHandler = createConfigurationHandler()
        configurationHandler.configure(
            PaymentSheet.InitializationMode.PaymentIntent(" "),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { success, error ->
            result = success to error
        }

        assertThat(result.first).isFalse()
        assertThat(result.second?.message)
            .isEqualTo("The PaymentIntent client_secret cannot be an empty string.")
    }

    @Test
    fun `configure() with invalid merchant`() = runTest {
        var result = Pair<Boolean, Throwable?>(true, null)
        val configurationHandler = createConfigurationHandler()
        configurationHandler.configure(
            PaymentSheet.InitializationMode.PaymentIntent(PaymentSheetFixtures.CLIENT_SECRET),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(merchantDisplayName = "")
        ) { success, error ->
            result = success to error
        }

        assertThat(result.first).isFalse()
        assertThat(result.second?.message)
            .isEqualTo("When a Configuration is passed to PaymentSheet, the Merchant display name cannot be an empty string.")
    }

    @Test
    fun `configure() with invalid customer id`() = runTest {
        var result = Pair<Boolean, Throwable?>(true, null)
        val configurationHandler = createConfigurationHandler()
        configurationHandler.configure(
            PaymentSheet.InitializationMode.PaymentIntent(PaymentSheetFixtures.CLIENT_SECRET),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                customer = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.customer?.copy(
                    id = " "
                )
            )
        ) { success, error ->
            result = success to error
        }

        assertThat(result.first).isFalse()
        assertThat(result.second?.message)
            .isEqualTo("When a CustomerConfiguration is passed to PaymentSheet, the Customer ID cannot be an empty string.")
    }

    @Test
    fun `configure() with invalid customer ephemeral key`() = runTest {
        var result = Pair<Boolean, Throwable?>(true, null)
        val configurationHandler = createConfigurationHandler()
        configurationHandler.configure(
            PaymentSheet.InitializationMode.PaymentIntent(PaymentSheetFixtures.CLIENT_SECRET),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                customer = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.customer?.copy(
                    ephemeralKeySecret = " "
                )
            )
        ) { success, error ->
            result = success to error
        }

        assertThat(result.first).isFalse()
        assertThat(result.second?.message)
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
            savedSelection = SavedSelection.None,
            linkState = LinkState(
                configuration = mock(),
                loginState = LinkState.LoginState.LoggedIn,
            ),
        )
    }

    private fun createConfigurationHandler(
        paymentSheetLoader: PaymentSheetLoader = defaultPaymentSheetLoader()
    ): FlowControllerConfigurationHandler {
        val viewModel = ViewModelProvider(activity)[FlowControllerViewModel::class.java]
        return FlowControllerConfigurationHandler(
            paymentSheetLoader = paymentSheetLoader,
            uiContext = testDispatcher,
            eventReporter = eventReporter,
            viewModel = viewModel,
            lpmResourceRepository = mock(),
            addressResourceRepository = mock(),
        )
    }
}
