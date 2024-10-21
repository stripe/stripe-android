package com.stripe.android.customersheet

import android.content.Context
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.customersheet.analytics.CustomerSheetEventReporter
import com.stripe.android.customersheet.utils.CustomerSheetTestHelper.createViewModel
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class CustomerSheetActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val contract = CustomerSheetContract()
    private val intent = contract.createIntent(
        context = context,
        input = CustomerSheetContract.Args(
            integrationType = CustomerSheetIntegration.Type.CustomerAdapter,
            configuration = CustomerSheet.Configuration(
                merchantDisplayName = "Example",
                googlePayEnabled = true,
            ),
            statusBarColor = null,
        ),
    )
    private val page = CustomerSheetPage(composeTestRule)

    @Before
    fun before() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    }

    @Test
    fun `Finish with cancel on back press`() {
        runActivityScenario {
            composeTestRule.waitForIdle()
            pressBack()
            composeTestRule.waitForIdle()
            assertThat(
                InternalCustomerSheetResult.fromIntent(scenario.getResult().resultData)
            ).isEqualTo(
                InternalCustomerSheetResult.Canceled(null)
            )
        }
    }

    @Test
    fun `Finish without result emits null result and contract parses error`() {
        runActivityScenario {
            activity.finish()
            assertThat(
                InternalCustomerSheetResult.fromIntent(scenario.getResult().resultData)
            ).isEqualTo(
                null
            )
            val result = contract.parseResult(
                scenario.getResult().resultCode,
                scenario.getResult().resultData,
            )
            assertThat(result).isInstanceOf<InternalCustomerSheetResult.Error>()
        }
    }

    @Test
    fun `Finish with cancel and payment selection on back press`() {
        runActivityScenario(
            paymentMethods = listOf(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            ),
            savedPaymentSelection = PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
        ) {
            composeTestRule.waitForIdle()
            pressBack()
            composeTestRule.waitForIdle()
            assertThat(
                InternalCustomerSheetResult.fromIntent(scenario.getResult().resultData)
            ).isEqualTo(
                InternalCustomerSheetResult.Canceled(
                    paymentSelection = PaymentSelection.Saved(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                )
            )
        }
    }

    @Test
    fun `Verify bottom sheet expands on start with default title`() {
        runActivityScenario {
            page.waitForText("Manage your payment methods")
        }
    }

    @Test
    fun `When savedPaymentMethods is not empty, payment method is visible`() {
        runActivityScenario(
            paymentMethods = listOf(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            ),
        ) {
            page.waitForText("···· 4242")
        }
    }

    @Test
    fun `When isGooglePayEnabled is true, google pay is visible`() {
        runActivityScenario(
            paymentMethods = listOf(),
            isGooglePayAvailable = true,
        ) {
            page.waitForText("google pay")
        }
    }

    @Test
    fun `When payment selection is different from original, primary button is visible`() {
        runActivityScenario(
            paymentMethods = listOf(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            ),
        ) {
            page.clickPaymentOptionItem("Google Pay")
            page.waitForText("Confirm")
        }
    }

    @Test
    fun `When adding a payment method, title and primary button label is for adding payment method`() {
        runActivityScenario(
            paymentMethods = listOf(),
            isGooglePayAvailable = false,
        ) {
            page.waitForText("Save a new payment method")
            page.waitForTextExactly("Save")
        }
    }

    @Test
    fun `When payment method available, edit mode is available`() {
        runActivityScenario(
            paymentMethods = List(3) {
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            },
        ) {
            page.waitForText("edit")
        }
    }

    @Test
    fun `When edit is pressed, payment methods enters edit mode`() {
        runActivityScenario(
            paymentMethods = List(3) {
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            },
        ) {
            page.clickOnText("edit")
            page.waitForText("done")
        }
    }

    @Test
    fun `When add payment method screen is shown, should display form elements`() {
        val eventReporter: CustomerSheetEventReporter = mock()

        runActivityScenario(
            paymentMethods = listOf(),
            isGooglePayAvailable = false,
            eventReporter = eventReporter,
        ) {
            page.waitForText("Card number")
        }
    }

    @Test
    fun `When card number is completed, should execute event`() {
        val eventReporter: CustomerSheetEventReporter = mock()

        runActivityScenario(
            paymentMethods = listOf(),
            isGooglePayAvailable = false,
            eventReporter = eventReporter,
        ) {
            page.waitForText("Card number")
            page.inputText("Card number", "4242424242424242")

            verify(eventReporter).onCardNumberCompleted()
        }
    }

    private fun activityScenario(
        paymentMethods: List<PaymentMethod>,
        isGooglePayAvailable: Boolean = true,
        savedPaymentSelection: PaymentSelection?,
        eventReporter: CustomerSheetEventReporter = mock(),
    ): InjectableActivityScenario<CustomerSheetActivity> {
        val viewModel = createViewModel(
            savedPaymentSelection = savedPaymentSelection,
            eventReporter = eventReporter,
            customerPaymentMethods = paymentMethods,
            isGooglePayAvailable = isGooglePayAvailable,
        )

        return injectableActivityScenario {
            injectActivity {
                this.viewModelFactoryProducer = { viewModelFactoryFor(viewModel) }
            }
        }
    }

    private fun runActivityScenario(
        paymentMethods: List<PaymentMethod> = listOf(),
        isGooglePayAvailable: Boolean = true,
        savedPaymentSelection: PaymentSelection? = null,
        eventReporter: CustomerSheetEventReporter = mock(),
        testBlock: CustomerSheetTestData.() -> Unit,
    ) {
        activityScenario(
            paymentMethods = paymentMethods,
            isGooglePayAvailable = isGooglePayAvailable,
            savedPaymentSelection = savedPaymentSelection,
            eventReporter = eventReporter,
        )
            .launchForResult(intent)
            .use { injectableActivityScenario ->
                injectableActivityScenario.onActivity { activity ->
                    with(
                        CustomerSheetTestDataImpl(
                            scenario = injectableActivityScenario,
                            activity = activity
                        )
                    ) {
                        testBlock()
                    }
                }
            }
    }

    private class CustomerSheetTestDataImpl(
        override val scenario: InjectableActivityScenario<CustomerSheetActivity>,
        override val activity: CustomerSheetActivity
    ) : CustomerSheetTestData

    interface CustomerSheetTestData {
        val scenario: InjectableActivityScenario<CustomerSheetActivity>
        val activity: CustomerSheetActivity
    }
}
