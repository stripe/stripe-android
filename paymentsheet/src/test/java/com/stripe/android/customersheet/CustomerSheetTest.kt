package com.stripe.android.customersheet

import android.app.Application
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.times
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import com.stripe.android.customersheet.util.CustomerSheetHacks
import com.stripe.android.customersheet.utils.FakeCustomerSessionProvider
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class CustomerSheetTest {
    @get:Rule
    val intentsTestRule = IntentsRule()

    @Before
    fun setup() {
        val appContext = ApplicationProvider.getApplicationContext<Application>()

        listOf(TestActivity::class.java, CustomerSheetHostActivity::class.java).forEach { activityClass ->
            val activityInfo = ActivityInfo().apply {
                name = activityClass.name
                packageName = appContext.packageName
                theme = R.style.StripePaymentSheetDefaultTheme
            }

            shadowOf(appContext.packageManager).addOrUpdateActivity(activityInfo)
        }
    }

    @Test
    fun `Creating two CustomerSheet instances in the same activity throws`() = runTestActivityTest {
        CustomerSheet.create(
            activity = activity,
            customerAdapter = FakeCustomerAdapter(),
            callback = {},
        )

        val error = assertFailsWith<IllegalStateException> {
            CustomerSheet.create(
                activity = activity,
                customerSessionProvider = FakeCustomerSessionProvider(),
                callback = {},
            )
        }

        assertThat(error.message).isEqualTo("Cannot have more than one active CustomerSheet instance!")
        completeTest()
    }

    @Test
    fun `Recreating the host activity initializes a single CustomerSheet without crashing`() {
        ActivityScenario.launch(CustomerSheetHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.customerSheet.configure(CUSTOM_CONFIGURATION)
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                activity.customerSheet.present()
            }

            intended(
                allOf(
                    hasComponent(CustomerSheetActivity::class.java.name),
                    hasExtra(
                        "args",
                        CustomerSheetContract.Args(
                            integrationType = CustomerSheetIntegration.Type.CustomerAdapter,
                            configuration = CUSTOM_CONFIGURATION,
                            statusBarColor = 0,
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `When presenting without configuring, should return error`() = runTestActivityTest {
        val customerSheet = CustomerSheet.create(
            activity = activity,
            customerAdapter = FakeCustomerAdapter(),
            callback = { result ->
                val failedResult = result.asFailed()

                assertThat(failedResult.exception).isInstanceOf(IllegalStateException::class.java)
                assertThat(failedResult.exception.message).isEqualTo(
                    "Must call `configure` first before attempting to present `CustomerSheet`!"
                )

                completeTest()
            },
        )

        customerSheet.present()
    }

    @Test
    fun `When presenting, should launch 'CustomerSheetActivity'`() = runTestActivityTest {
        val customerSheet = CustomerSheet.create(
            activity = activity,
            customerAdapter = FakeCustomerAdapter(),
            callback = {},
        )

        val configuration = CustomerSheet.Configuration.builder(merchantDisplayName = "Merchant, Inc.")
            .googlePayEnabled(googlePayEnabled = true)
            .preferredNetworks(preferredNetworks = listOf(CardBrand.CartesBancaires))
            .build()

        customerSheet.configure(
            configuration = configuration,
        )

        customerSheet.present()

        intended(hasComponent(CustomerSheetActivity::class.java.name))
        intended(
            hasExtra(
                "args",
                CustomerSheetContract.Args(
                    integrationType = CustomerSheetIntegration.Type.CustomerAdapter,
                    configuration = configuration,
                    statusBarColor = 0
                )
            )
        )

        completeTest()
    }

    @Test
    fun `When retrieving a payment option without configuring, should return error`() = runPaymentOptionTest(
        configuration = null,
    ) { result ->
        val failedResult = result.asFailed()

        assertThat(failedResult.exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.exception.message).isEqualTo(
            "Must call `configure` first before attempting to fetch the saved payment option!"
        )
    }

    @Test
    fun `When retrieving a payment option, should return expected payment option from adapter`() =
        runPaymentOptionTest(
            paymentOption = CustomerAdapter.Result.success(CustomerAdapter.PaymentOption.fromId("pm_1")),
            paymentMethods = CustomerAdapter.Result.success(listOf(CARD_PAYMENT_METHOD.copy("pm_1"))),
            configuration = CustomerSheet.Configuration.builder(merchantDisplayName = "Merchant, Inc.")
                .build(),
        ) { result ->
            val selectedResult = result.asSelected()

            val paymentMethodSelection = selectedResult.selection.asPaymentMethodSelection()

            assertThat(paymentMethodSelection.paymentMethod)
                .isEqualTo(CARD_PAYMENT_METHOD.copy(id = "pm_1"))
            assertThat(paymentMethodSelection.paymentOption.paymentMethodType)
                .isEqualTo("card")
            assertThat(paymentMethodSelection.paymentOption.label)
                .isEqualTo("\u2066···· 4242\u2069")
        }

    @Test
    fun `When retrieving a payment option, should fail if payment option retrieval fails`() =
        runPaymentOptionTest(
            paymentOption = CustomerAdapter.Result.failure(
                cause = IllegalStateException("Failed to retrieve!"),
                displayMessage = null,
            ),
            configuration = CustomerSheet.Configuration.builder(merchantDisplayName = "Merchant, Inc.")
                .build(),
        ) { result ->
            val failedResult = result.asFailed()

            assertThat(failedResult.exception).isInstanceOf(IllegalStateException::class.java)
            assertThat(failedResult.exception.message).isEqualTo("Failed to retrieve!")
        }

    @Test
    fun `When retrieving a payment option, should null selection if payment methods retrieval fails`() =
        runPaymentOptionTest(
            paymentOption = CustomerAdapter.Result.success(CustomerAdapter.PaymentOption.fromId("pm_1")),
            paymentMethods = CustomerAdapter.Result.failure(
                cause = IllegalStateException("Failed to retrieve!"),
                displayMessage = null,
            ),
            configuration = CustomerSheet.Configuration.builder(merchantDisplayName = "Merchant, Inc.")
                .build(),
        ) { result ->
            val selectedResult = result.asSelected()

            assertThat(selectedResult.selection).isNull()
        }

    @Test
    fun `When retrieving a payment option, should return null selection if option no longer in payment methods`() =
        runPaymentOptionTest(
            paymentOption = CustomerAdapter.Result.success(CustomerAdapter.PaymentOption.fromId("pm_1")),
            paymentMethods = CustomerAdapter.Result.success(listOf()),
            configuration = CustomerSheet.Configuration.builder(merchantDisplayName = "Merchant, Inc.")
                .build(),
        ) { result ->
            val selectedResult = result.asSelected()

            assertThat(selectedResult.selection).isNull()
        }

    @Test
    fun `When Google payment option, should return option is config enables Google Pay`() = runPaymentOptionTest(
        paymentOption = CustomerAdapter.Result.success(CustomerAdapter.PaymentOption.GooglePay),
        configuration = CustomerSheet.Configuration.builder(merchantDisplayName = "Merchant, Inc.")
            .googlePayEnabled(googlePayEnabled = true)
            .build(),
    ) { result ->
        val selectedResult = result.asSelected()

        assertThat(selectedResult.selection).isInstanceOf(PaymentOptionSelection.GooglePay::class.java)
    }

    @Test
    fun `When Google payment option, should not return option is config disables Google Pay`() = runPaymentOptionTest(
        paymentOption = CustomerAdapter.Result.success(CustomerAdapter.PaymentOption.GooglePay),
        configuration = CustomerSheet.Configuration.builder(merchantDisplayName = "Merchant, Inc.")
            .googlePayEnabled(googlePayEnabled = false)
            .build(),
    ) { result ->
        val selectedResult = result.asSelected()

        assertThat(selectedResult.selection).isNull()
    }

    @Test
    fun `When Google payment option, should not return option is config has not Google Pay config`() =
        runPaymentOptionTest(
            paymentOption = CustomerAdapter.Result.success(CustomerAdapter.PaymentOption.GooglePay),
            configuration = CustomerSheet.Configuration.builder(merchantDisplayName = "Merchant, Inc.")
                .build(),
        ) { result ->
            val selectedResult = result.asSelected()

            assertThat(selectedResult.selection).isNull()
        }

    @Test
    fun `On configure, should persist on config changes`() = runTestActivityTest {
        val customerSheet = CustomerSheet.create(
            activity = activity,
            customerAdapter = FakeCustomerAdapter(),
            callback = {},
        )

        customerSheet.configure(
            configuration = CustomerSheet.Configuration
                .builder(merchantDisplayName = "Merchant, Inc.")
                .build()
        )

        customerSheet.present()

        activityScenario.recreate()

        activityScenario.onActivity { recreatedActivity ->
            val recreatedCustomerSheet = CustomerSheet.create(
                activity = recreatedActivity,
                customerAdapter = FakeCustomerAdapter(),
                callback = {},
            )

            recreatedCustomerSheet.present()
        }

        intended(hasComponent(CustomerSheetActivity::class.java.name), times(2))

        completeTest()
    }

    @Test
    fun `retrieving saved payment method passes configured appearance to factory`() = runFactoryScenario {
        val call = retrieveFactoryCall()

        assertThat(call.selection).isEqualTo(PaymentSelection.Saved(CARD_PAYMENT_METHOD))
        assertThat(call.canUseGooglePay).isFalse()
        assertThat(call.appearance).isEqualTo(CUSTOM_CONFIGURATION.appearance)
    }

    @Test
    fun `retrieving Google Pay passes configured appearance to factory`() = runFactoryScenario(
        paymentOption = CustomerAdapter.PaymentOption.GooglePay,
        configuration = CUSTOM_CONFIGURATION.newBuilder().googlePayEnabled(true).build(),
    ) {
        val call = retrieveFactoryCall()

        assertThat(call.selection).isEqualTo(PaymentSelection.GooglePay)
        assertThat(call.canUseGooglePay).isTrue()
        assertThat(call.appearance).isEqualTo(CUSTOM_CONFIGURATION.appearance)
    }

    @Test
    fun `retrieving payment option passes default appearance to factory`() = runFactoryScenario(
        configuration = CustomerSheet.Configuration(merchantDisplayName = "Merchant, Inc."),
    ) {
        val call = retrieveFactoryCall()

        assertThat(call.selection).isEqualTo(PaymentSelection.Saved(CARD_PAYMENT_METHOD))
        assertThat(call.appearance).isEqualTo(PaymentSheet.Appearance())
    }

    @Test
    fun `retrieving null selection passes configured appearance to factory`() = runFactoryScenario(
        paymentOption = null,
    ) {
        val call = retrieveFactoryCall()

        assertThat(call.selection).isNull()
        assertThat(call.appearance).isEqualTo(CUSTOM_CONFIGURATION.appearance)
    }

    @Test
    fun `retrieving after reconfiguration passes updated appearance to factory`() = runFactoryScenario {
        assertThat(retrieveFactoryCall().appearance).isEqualTo(CUSTOM_CONFIGURATION.appearance)
        val updatedAppearance = PaymentSheet.Appearance(
            shapes = PaymentSheet.Shapes(cornerRadiusDp = 20f, borderStrokeWidthDp = 1f),
        )

        customerSheet.configure(CUSTOM_CONFIGURATION.newBuilder().appearance(updatedAppearance).build())

        assertThat(retrieveFactoryCall().appearance).isEqualTo(updatedAppearance)
    }

    private fun runFactoryScenario(
        paymentOption: CustomerAdapter.PaymentOption? = CustomerAdapter.PaymentOption.fromId(CARD_PAYMENT_METHOD.id!!),
        configuration: CustomerSheet.Configuration = CUSTOM_CONFIGURATION,
        test: suspend FactoryScenario.() -> Unit,
    ) = runTestActivityTest {
        val factory = FakePaymentOptionSelectionFactory()
        CustomerSheetHacks.initialize(
            application = activity.application,
            lifecycleOwner = activity,
            integration = CustomerSheetIntegration.Adapter(
                FakeCustomerAdapter(selectedPaymentOption = CustomerAdapter.Result.success(paymentOption)),
            ),
        )
        val customerSheet = CustomerSheet(
            application = activity.application,
            lifecycleOwner = activity,
            activityResultRegistryOwner = activity,
            viewModelStoreOwner = activity,
            integrationType = CustomerSheetIntegration.Type.CustomerAdapter,
            paymentOptionSelectionFactory = factory,
            callback = {},
            statusBarColor = { null },
        )
        customerSheet.configure(configuration)

        runBlocking {
            FactoryScenario(customerSheet, factory).test()
        }

        factory.ensureAllEventsConsumed()
        completeTest()
    }

    private fun runPaymentOptionTest(
        configuration: CustomerSheet.Configuration?,
        paymentOption: CustomerAdapter.Result<CustomerAdapter.PaymentOption?> =
            CustomerAdapter.Result.success(null),
        paymentMethods: CustomerAdapter.Result<List<PaymentMethod>> =
            CustomerAdapter.Result.success(listOf()),
        test: (result: CustomerSheetResult) -> Unit,
    ) {
        runTestActivityTest {
            val customerSheet = CustomerSheet.create(
                activity = activity,
                customerAdapter = FakeCustomerAdapter(
                    selectedPaymentOption = paymentOption,
                    paymentMethods = paymentMethods,
                ),
                callback = {},
            )

            configuration?.let {
                customerSheet.configure(configuration)
            }

            runBlocking {
                test(customerSheet.retrievePaymentOptionSelection())

                completeTest()
            }
        }
    }

    private fun runTestActivityTest(
        test: Scenario.() -> Unit,
    ) {
        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            val countDownLatch = CountDownLatch(1)

            scenario.onActivity {
                Scenario(
                    activityScenario = scenario,
                    activity = it,
                    completeTest = countDownLatch::countDown,
                ).test()
            }

            assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
        }
    }

    private fun PaymentOptionSelection?.asPaymentMethodSelection(): PaymentOptionSelection.PaymentMethod {
        return this as PaymentOptionSelection.PaymentMethod
    }

    private fun CustomerSheetResult.asSelected(): CustomerSheetResult.Selected {
        return this as CustomerSheetResult.Selected
    }

    private fun CustomerSheetResult.asFailed(): CustomerSheetResult.Failed {
        return this as CustomerSheetResult.Failed
    }

    private class Scenario(
        val activityScenario: ActivityScenario<TestActivity>,
        val activity: TestActivity,
        val completeTest: () -> Unit,
    )

    private class TestActivity : AppCompatActivity()

    private class CustomerSheetHostActivity : AppCompatActivity() {
        lateinit var customerSheet: CustomerSheet
            private set

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            customerSheet = CustomerSheet.create(
                activity = this,
                customerAdapter = FakeCustomerAdapter(),
                callback = {},
            )
        }
    }

    private class FactoryScenario(
        val customerSheet: CustomerSheet,
        val factory: FakePaymentOptionSelectionFactory,
    ) {
        suspend fun retrieveFactoryCall(): FakePaymentOptionSelectionFactory.CreateCall {
            customerSheet.retrievePaymentOptionSelection()
            return factory.createCalls.awaitItem()
        }
    }

    private companion object {
        val CUSTOM_CONFIGURATION = CustomerSheet.Configuration(
            merchantDisplayName = "Merchant, Inc.",
            appearance = PaymentSheet.Appearance(
                colorsLight = PaymentSheet.Colors.Builder.light().component(Color.BLACK).build(),
            ),
        )
    }
}
