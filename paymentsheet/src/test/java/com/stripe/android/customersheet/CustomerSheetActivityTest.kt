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
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.analytics.CustomerSheetEventReporter
import com.stripe.android.customersheet.utils.CustomerSheetTestHelper.createViewModel
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.TestUiDefinitionFactoryArgumentsFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
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
import java.util.Stack

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
@OptIn(ExperimentalCustomerSheetApi::class)
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
            viewState = createSelectPaymentMethodViewState(
                paymentSelection = PaymentSelection.Saved(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                )
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
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(
                title = null
            ),
        ) {
            page.waitForText("Manage your payment methods")
        }
    }

    @Test
    fun `When savedPaymentMethods is not empty, payment method is visible`() {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(
                savedPaymentMethods = listOf(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                )
            ),
        ) {
            page.waitForText("····4242")
        }
    }

    @Test
    fun `When isGooglePayEnabled is true, google pay is visible`() {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(
                isGooglePayEnabled = true
            ),
        ) {
            page.waitForText("google pay")
        }
    }

    @Test
    fun `When payment selection is different from original, primary button is visible`() {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(
                isGooglePayEnabled = true,
                paymentSelection = null,
                primaryButtonVisible = false,
                savedPaymentMethods = listOf(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                ),
            ),
        ) {
            page.clickPaymentOptionItem("Google Pay")
            page.waitForText("Confirm")
        }
    }

    @Test
    fun `When adding a payment method, title and primary button label is for adding payment method`() {
        runActivityScenario(
            viewState = createAddPaymentMethodViewState(),
        ) {
            page.waitForText("Save a new payment method")
            page.waitForTextExactly("Save")
        }
    }

    @Test
    fun `When payment method available, edit mode is available`() {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(
                isGooglePayEnabled = true,
                isEditing = false,
                savedPaymentMethods = List(3) {
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                }
            ),
        ) {
            page.waitForText("edit")
        }
    }

    @Test
    fun `When edit is pressed, payment methods enters edit mode`() {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(
                isGooglePayEnabled = true,
                isEditing = true,
                savedPaymentMethods = List(3) {
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                }
            ),
        ) {
            page.waitForText("done")
        }
    }

    @Test
    fun `When add payment method screen is shown, should display form elements`() {
        val eventReporter: CustomerSheetEventReporter = mock()

        runActivityScenario(
            viewState = createAddPaymentMethodViewState(),
            eventReporter = eventReporter,
        ) {
            page.waitForText("Card number")
        }
    }

    @Test
    fun `When card number is completed, should execute event`() {
        val eventReporter: CustomerSheetEventReporter = mock()

        runActivityScenario(
            viewState = createAddPaymentMethodViewState(),
            eventReporter = eventReporter,
        ) {
            page.waitForText("Card number")
            page.inputText("Card number", "4242424242424242")

            verify(eventReporter).onCardNumberCompleted()
        }
    }

    private fun activityScenario(
        viewState: CustomerSheetViewState,
        savedPaymentSelection: PaymentSelection?,
        eventReporter: CustomerSheetEventReporter = mock(),
    ): InjectableActivityScenario<CustomerSheetActivity> {
        val viewModel = createViewModel(
            initialBackStack = Stack<CustomerSheetViewState>().apply {
                push(viewState)
            },
            savedPaymentSelection = savedPaymentSelection,
            eventReporter = eventReporter
        )

        return injectableActivityScenario {
            injectActivity {
                this.viewModelFactoryProducer = { viewModelFactoryFor(viewModel) }
            }
        }
    }

    private fun runActivityScenario(
        viewState: CustomerSheetViewState = CustomerSheetViewState.Loading(
            isLiveMode = false,
        ),
        savedPaymentSelection: PaymentSelection? = null,
        eventReporter: CustomerSheetEventReporter = mock(),
        testBlock: CustomerSheetTestData.() -> Unit,
    ) {
        activityScenario(
            viewState = viewState,
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

    private fun createSelectPaymentMethodViewState(
        title: String? = null,
        savedPaymentMethods: List<PaymentMethod> = listOf(),
        paymentSelection: PaymentSelection? = null,
        isLiveMode: Boolean = false,
        isProcessing: Boolean = false,
        isEditing: Boolean = false,
        isGooglePayEnabled: Boolean = false,
        primaryButtonVisible: Boolean = true,
        primaryButtonLabel: String? = null,
    ): CustomerSheetViewState.SelectPaymentMethod {
        return CustomerSheetViewState.SelectPaymentMethod(
            title = title,
            savedPaymentMethods = savedPaymentMethods,
            paymentSelection = paymentSelection,
            isLiveMode = isLiveMode,
            isProcessing = isProcessing,
            isEditing = isEditing,
            isGooglePayEnabled = isGooglePayEnabled,
            primaryButtonVisible = primaryButtonVisible,
            primaryButtonLabel = primaryButtonLabel,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            allowsRemovalOfLastSavedPaymentMethod = true,
            canRemovePaymentMethods = true,
        )
    }

    private fun createAddPaymentMethodViewState(
        paymentMethodCode: PaymentMethodCode = PaymentMethod.Type.Card.code,
        isLiveMode: Boolean = false,
        enabled: Boolean = true,
        isProcessing: Boolean = false,
    ): CustomerSheetViewState.AddPaymentMethod {
        val card = LpmRepositoryTestHelpers.card
        val cardFormElements = PaymentMethodMetadataFactory.create().formElementsForCode(
            code = "card",
            uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create()
        )!!
        return CustomerSheetViewState.AddPaymentMethod(
            paymentMethodCode = paymentMethodCode,
            supportedPaymentMethods = listOf(card),
            formFieldValues = null,
            formElements = cardFormElements,
            formArguments = FormArguments(
                paymentMethodCode = PaymentMethod.Type.Card.code,
                cbcEligibility = CardBrandChoiceEligibility.Ineligible,
                merchantName = ""
            ),
            usBankAccountFormArguments = mock(),
            enabled = enabled,
            isLiveMode = isLiveMode,
            isProcessing = isProcessing,
            isFirstPaymentMethod = false,
            primaryButtonLabel = "Save".resolvableString,
            primaryButtonEnabled = false,
            customPrimaryButtonUiState = null,
            bankAccountResult = null,
            draftPaymentSelection = null,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            errorReporter = FakeErrorReporter(),
        )
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
