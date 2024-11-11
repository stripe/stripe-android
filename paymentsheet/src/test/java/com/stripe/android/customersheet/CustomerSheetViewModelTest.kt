package com.stripe.android.customersheet

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.CustomerSheetViewState.AddPaymentMethod
import com.stripe.android.customersheet.CustomerSheetViewState.SelectPaymentMethod
import com.stripe.android.customersheet.analytics.CustomerSheetEventReporter
import com.stripe.android.customersheet.data.CustomerSheetDataResult
import com.stripe.android.customersheet.data.FakeCustomerSheetIntentDataSource
import com.stripe.android.customersheet.data.FakeCustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.FakeCustomerSheetSavedSelectionDataSource
import com.stripe.android.customersheet.injection.CustomerSheetViewModelModule
import com.stripe.android.customersheet.utils.CustomerSheetTestHelper.createModifiableEditPaymentMethodViewInteractorFactory
import com.stripe.android.customersheet.utils.CustomerSheetTestHelper.createViewModel
import com.stripe.android.customersheet.utils.FakeCustomerSheetLoader
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.model.PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
import com.stripe.android.model.PaymentMethodFixtures.US_BANK_ACCOUNT
import com.stripe.android.model.PaymentMethodFixtures.US_BANK_ACCOUNT_VERIFIED
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewAction
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewAction.OnBrandChoiceChanged
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewAction.OnRemoveConfirmed
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewAction.OnRemovePressed
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewAction.OnUpdatePressed
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.CardDetailsSectionElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.utils.BankFormScreenStateFactory
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.coroutineContext
import kotlin.test.assertFailsWith
import com.stripe.android.ui.core.R as UiCoreR

@RunWith(RobolectricTestRunner::class)
class CustomerSheetViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher(TestCoroutineScheduler())

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @Test
    fun `isLiveMode is true when publishable key is live`() {
        var isLiveMode = CustomerSheetViewModelModule.isLiveMode {
            PaymentConfiguration(
                publishableKey = "pk_test_123"
            )
        }

        assertThat(isLiveMode()).isFalse()

        isLiveMode = CustomerSheetViewModelModule.isLiveMode {
            PaymentConfiguration(
                publishableKey = "pk_live_123"
            )
        }

        assertThat(isLiveMode()).isTrue()

        isLiveMode = CustomerSheetViewModelModule.isLiveMode {
            PaymentConfiguration(
                publishableKey = "pk_test_51HvTI7Lu5o3livep6t5AgBSkMvWoTtA0nyA7pVYDqpfLkRtWun7qZTYCOHCReprfLM464yaBeF72UFfB7cY9WG4a00ZnDtiC2C"
            )
        }

        assertThat(isLiveMode()).isFalse()
    }

    @Test
    fun `init emits CustomerSheetViewState#AddPaymentMethod when no payment methods available`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerSheetLoader = FakeCustomerSheetLoader(
                isGooglePayAvailable = false,
                customerPaymentMethods = listOf()
            ),
        )
        viewModel.viewState.test {
            val state = awaitItem()

            assertThat(state).isInstanceOf<AddPaymentMethod>()
            assertThat(state.topBarState {}.showEditMenu).isFalse()
        }
    }

    @Test
    fun `init emits CustomerSheetViewState#SelectPaymentMethod when only google pay available`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = true
        )
        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()
        }
    }

    @Test
    fun `init emits CustomerSheetViewState#SelectPaymentMethod when payment methods available`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher
        )
        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()
        }
    }

    @Test
    fun `on init, sends expected 'onInit' event to event reporter`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        createViewModel(
            workContext = testDispatcher,
            eventReporter = eventReporter,
            integrationType = CustomerSheetIntegration.Type.CustomerAdapter,
            configuration = CustomerSheetFixtures.MINIMUM_CONFIG,
        )

        verify(eventReporter).onInit(
            configuration = CustomerSheetFixtures.MINIMUM_CONFIG,
            integrationType = CustomerSheetIntegration.Type.CustomerAdapter,
        )
    }

    @Test
    fun `on init with customer session, sends expected 'onInit' event to event reporter`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        createViewModel(
            workContext = testDispatcher,
            eventReporter = eventReporter,
            integrationType = CustomerSheetIntegration.Type.CustomerSession,
            configuration = CustomerSheetFixtures.MINIMUM_CONFIG,
        )

        verify(eventReporter).onInit(
            configuration = CustomerSheetFixtures.MINIMUM_CONFIG,
            integrationType = CustomerSheetIntegration.Type.CustomerSession,
        )
    }

    @Test
    fun `CustomerSheetViewAction#OnBackPressed emits canceled result`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
        )
        viewModel.result.test {
            assertThat(awaitItem()).isEqualTo(null)
            viewModel.handleViewAction(CustomerSheetViewAction.OnBackPressed)
            assertThat(awaitItem()).isEqualTo(InternalCustomerSheetResult.Canceled(null))
        }
    }

    @Test
    fun `When payment methods loaded, CustomerSheetViewState is populated`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerSheetLoader = FakeCustomerSheetLoader(
                isGooglePayAvailable = false,
                customerPaymentMethods = listOf(CARD_PAYMENT_METHOD),
                paymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD),
            ),
        )
        viewModel.viewState.test {
            val selectState = awaitViewState<SelectPaymentMethod>()

            assertThat(selectState.savedPaymentMethods).isEqualTo(
                listOf(
                    CARD_PAYMENT_METHOD,
                )
            )

            assertThat(selectState.paymentSelection).isEqualTo(
                PaymentSelection.Saved(
                    paymentMethod = CARD_PAYMENT_METHOD,
                )
            )

            assertThat(selectState.primaryButtonLabel)
                .isEqualTo(R.string.stripe_paymentsheet_confirm.resolvableString)
        }
    }

    @Test
    fun `When payment methods cannot be loaded, sheet closes`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerSheetLoader = FakeCustomerSheetLoader(
                shouldFail = true,
            ),
        )
        viewModel.result.test {
            assertThat((awaitItem() as InternalCustomerSheetResult.Error).exception.message)
                .isEqualTo("failed to load")
        }
    }

    @Test
    fun `When the selected payment method cannot be loaded, sheet closes`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerSheetLoader = FakeCustomerSheetLoader(
                shouldFail = true,
            ),
        )
        viewModel.result.test {
            assertThat((awaitItem() as InternalCustomerSheetResult.Error).exception.message)
                .isEqualTo("failed to load")
        }
    }

    @Test
    fun `When the Google Pay is selected payment method, paymentSelection is GooglePay`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerSheetLoader = FakeCustomerSheetLoader(
                isGooglePayAvailable = true,
                paymentSelection = PaymentSelection.GooglePay,
            ),
        )
        viewModel.viewState.test {
            val viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.paymentSelection)
                .isEqualTo(PaymentSelection.GooglePay)
            assertThat(viewState.errorMessage)
                .isEqualTo(null)
        }
    }

    @Test
    fun `When the payment method is selected payment method, paymentSelection is payment method`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerSheetLoader = FakeCustomerSheetLoader(
                customerPaymentMethods = listOf(CARD_PAYMENT_METHOD),
                paymentSelection = PaymentSelection.Saved(
                    CARD_PAYMENT_METHOD
                ),
            ),
        )
        viewModel.viewState.test {
            val viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.paymentSelection)
                .isInstanceOf<PaymentSelection.Saved>()
            assertThat(viewState.errorMessage)
                .isEqualTo(null)
        }
    }

    @Test
    fun `providePaymentMethodName provides payment method name given code`() {
        val viewModel = createViewModel(
            workContext = testDispatcher
        )
        val name = viewModel.providePaymentMethodName(PaymentMethod.Type.Card.code)
        assertThat(name.resolve(ApplicationProvider.getApplicationContext()))
            .isEqualTo("Card")
    }

    @Test
    fun `When selection, primary button label should not be null`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerSheetLoader = FakeCustomerSheetLoader(
                customerPaymentMethods = listOf(
                    CARD_PAYMENT_METHOD
                ),
                paymentSelection = PaymentSelection.Saved(
                    paymentMethod = CARD_PAYMENT_METHOD
                )
            )
        )
        viewModel.viewState.test {
            val viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.primaryButtonLabel)
                .isNotNull()
            assertThat(viewState.primaryButtonEnabled)
                .isTrue()
        }
    }

    @Test
    fun `When no selection, the primary button is not visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher
        )
        viewModel.viewState.test {
            assertThat(awaitViewState<SelectPaymentMethod>().primaryButtonVisible)
                .isFalse()
        }
    }

    @Test
    fun `When Stripe payment method is selected, the primary button is visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher
        )
        viewModel.viewState.test {
            var viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.primaryButtonVisible)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    selection = PaymentSelection.Saved(
                        paymentMethod = CARD_PAYMENT_METHOD
                    )
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(R.string.stripe_paymentsheet_confirm.resolvableString)
            assertThat(viewState.primaryButtonEnabled)
                .isTrue()
            assertThat(viewState.primaryButtonVisible)
                .isTrue()
        }
    }

    @Test
    fun `When Google Pay is selected, the primary button is visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher
        )
        viewModel.viewState.test {
            var viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(R.string.stripe_paymentsheet_confirm.resolvableString)
            assertThat(viewState.primaryButtonVisible)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    selection = PaymentSelection.GooglePay
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(R.string.stripe_paymentsheet_confirm.resolvableString)
            assertThat(viewState.primaryButtonEnabled)
                .isTrue()
            assertThat(viewState.primaryButtonVisible)
                .isTrue()
        }
    }

    @Test
    fun `When CustomerViewAction#OnItemSelected with editing view state, payment selection should not be updated`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerSheetLoader = FakeCustomerSheetLoader(
                customerPaymentMethods = listOf(
                    CARD_PAYMENT_METHOD,
                    CARD_PAYMENT_METHOD.copy(id = "pm_2")
                ),
                paymentSelection = PaymentSelection.Saved(
                    CARD_PAYMENT_METHOD
                )
            ),
        )
        viewModel.viewState.test {
            val initialViewState = awaitViewState<SelectPaymentMethod>()
            val initialPaymentSelection = initialViewState.paymentSelection as PaymentSelection.Saved

            assertThat(initialPaymentSelection.paymentMethod).isEqualTo(CARD_PAYMENT_METHOD)

            viewModel.handleViewAction(CustomerSheetViewAction.OnEditPressed)
            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    PaymentSelection.Saved(
                        paymentMethod = CARD_PAYMENT_METHOD.copy("pm_2")
                    )
                )
            )

            val currentViewState = awaitViewState<SelectPaymentMethod>()
            val currentPaymentSelection = currentViewState.paymentSelection as PaymentSelection.Saved

            assertThat(currentPaymentSelection.paymentMethod).isEqualTo(CARD_PAYMENT_METHOD)
        }
    }

    @Test
    fun `When CustomerViewAction#OnItemSelected with Link, exception should be thrown`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher
        )
        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()
            val error = assertFailsWith<IllegalStateException> {
                viewModel.handleViewAction(
                    CustomerSheetViewAction.OnItemSelected(
                        selection = PaymentSelection.Link
                    )
                )
            }
            assertThat(error.message).contains("Unsupported payment selection")
        }
    }

    @Test
    fun `When CustomerViewAction#OnItemSelected with null, primary button label should be null`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher
        )
        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()
            val error = assertFailsWith<IllegalStateException> {
                viewModel.handleViewAction(
                    CustomerSheetViewAction.OnItemSelected(
                        selection = null
                    )
                )
            }
            assertThat(error.message).contains("Unsupported payment selection")
        }
    }

    @Test
    fun `When the payment configuration is test, isLiveMode should be false`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            paymentConfiguration = PaymentConfiguration(
                publishableKey = "pk_test_123",
                stripeAccountId = null,
            )
        )

        viewModel.viewState.test {
            assertThat(awaitItem().isLiveMode)
                .isFalse()
        }
    }

    @Test
    fun `When CustomerViewAction#OnAddCardPressed, view state is updated to CustomerViewAction#AddPaymentMethod and fields are shown`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher
        )

        viewModel.viewState.test {
            assertThat(awaitItem())
                .isInstanceOf<SelectPaymentMethod>()
            viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)

            val item = awaitItem()
            assertThat(item).isInstanceOf<AddPaymentMethod>()

            val formElements = item.asAddState().formElements

            assertThat(formElements[0]).isInstanceOf<CardDetailsSectionElement>()
            assertThat(formElements[1]).isInstanceOf<SectionElement>()
            assertThat(formElements[1].asSectionElement().fields[0])
                .isInstanceOf<CardBillingAddressElement>()
        }
    }

    @Test
    fun `When CustomerViewAction#OnEditPressed, view state isEditing should be updated`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(CARD_PAYMENT_METHOD),
        )
        viewModel.viewState.test {
            var viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.isEditing).isFalse()
            assertThat(viewState.topBarState {}.showEditMenu).isTrue()

            viewModel.handleViewAction(CustomerSheetViewAction.OnEditPressed)

            viewState = awaitViewState()
            assertThat(viewState.isEditing).isTrue()
            assertThat(viewState.topBarState {}.showEditMenu).isTrue()

            viewModel.handleViewAction(CustomerSheetViewAction.OnItemRemoved(CARD_PAYMENT_METHOD))

            viewState = awaitViewState()
            assertThat(viewState.isEditing).isFalse()
            assertThat(viewState.topBarState {}.showEditMenu).isFalse()
        }
    }

    @Test
    fun `When CustomerSheetViewAction#OnItemRemoved with allowsRemovalOfLastSavedPaymentMethod=false, view state isEditing should be updated`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            configuration = CustomerSheet.Configuration(
                merchantDisplayName = "Example",
                googlePayEnabled = true,
                allowsRemovalOfLastSavedPaymentMethod = false,
            ),
            customerPaymentMethods = listOf(CARD_PAYMENT_METHOD, CARD_PAYMENT_METHOD.copy(id = "pm_543")),
        )
        viewModel.viewState.test {
            var viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.isEditing).isFalse()
            assertThat(viewState.topBarState {}.showEditMenu).isTrue()

            viewModel.handleViewAction(CustomerSheetViewAction.OnEditPressed)

            viewState = awaitViewState()
            assertThat(viewState.isEditing).isTrue()
            assertThat(viewState.topBarState {}.showEditMenu).isTrue()

            viewModel.handleViewAction(CustomerSheetViewAction.OnItemRemoved(CARD_PAYMENT_METHOD))

            viewState = awaitViewState()
            assertThat(viewState.isEditing).isFalse()
            assertThat(viewState.topBarState {}.showEditMenu).isFalse()
        }
    }

    @Test
    fun `When removing a payment method, payment method list should be updated`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerSheetLoader = FakeCustomerSheetLoader(
                customerPaymentMethods = listOf(
                    CARD_PAYMENT_METHOD,
                    CARD_PAYMENT_METHOD.copy(id = "pm_2")
                ),
                paymentSelection = PaymentSelection.Saved(
                    CARD_PAYMENT_METHOD
                )
            ),
        )
        viewModel.viewState.test {
            var viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.savedPaymentMethods).hasSize(2)
            assertThat(viewState.paymentSelection).isNotNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemRemoved(
                    CARD_PAYMENT_METHOD
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.savedPaymentMethods).hasSize(1)
            assertThat(viewState.paymentSelection).isNull()
        }
    }

    @Test
    fun `When removing last payment method & google pay disabled, should transition to add payment screen`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerSheetLoader = FakeCustomerSheetLoader(
                isGooglePayAvailable = false,
                customerPaymentMethods = listOf(CARD_PAYMENT_METHOD),
                paymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD),
            ),
        )
        viewModel.viewState.test {
            val viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.savedPaymentMethods).hasSize(1)
            assertThat(viewState.paymentSelection).isNotNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemRemoved(
                    CARD_PAYMENT_METHOD
                )
            )

            // Briefly show the payment method removed then transition
            awaitViewState<SelectPaymentMethod>()
            val addPaymentMethodViewState = awaitViewState<AddPaymentMethod>()

            assertThat(addPaymentMethodViewState.isFirstPaymentMethod).isTrue()
        }
    }

    @Test
    fun `When removing a payment method fails, error message is displayed`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                onDetachPaymentMethod = {
                    CustomerSheetDataResult.failure(
                        cause = APIException(
                            stripeError = StripeError(
                                message = "Cannot remove this payment method."
                            )
                        ),
                        displayMessage = "We were unable to remove this payment method, try again."
                    )
                }
            )
        )
        viewModel.viewState.test {
            var viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.savedPaymentMethods).hasSize(1)
            assertThat(viewState.errorMessage).isNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemRemoved(
                    CARD_PAYMENT_METHOD
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.savedPaymentMethods).hasSize(1)
            assertThat(viewState.errorMessage)
                .isEqualTo("We were unable to remove this payment method, try again.")
        }
    }

    @Test
    fun `When primary button is pressed for saved payment method, selected payment method is emitted`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(CARD_PAYMENT_METHOD),
            savedPaymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD),
        )
        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(backgroundScope)
            val resultTurbine = viewModel.result.testIn(backgroundScope)

            assertThat(viewStateTurbine.awaitItem()).isInstanceOf<SelectPaymentMethod>()
            viewStateTurbine.cancelAndIgnoreRemainingEvents()
            assertThat(resultTurbine.awaitItem()).isNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            assertThat(resultTurbine.awaitItem()).isInstanceOf<InternalCustomerSheetResult.Selected>()
        }
    }

    @Test
    fun `When primary button is pressed for saved payment method that cannot be saved, error message is emitted`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(CARD_PAYMENT_METHOD),
            savedPaymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD),
            savedSelectionDataSource = FakeCustomerSheetSavedSelectionDataSource(
                onSetSavedSelection = {
                    CustomerSheetDataResult.failure(
                        cause = Exception("Unable to set payment option"),
                        displayMessage = "Something went wrong"
                    )
                }
            )
        )
        viewModel.viewState.test {
            val viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.errorMessage).isNull()
            assertThat(viewState.primaryButtonEnabled).isTrue()
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            assertThat(awaitViewState<SelectPaymentMethod>().primaryButtonEnabled)
                .isFalse()
            assertThat(awaitViewState<SelectPaymentMethod>().errorMessage)
                .isEqualTo("Something went wrong")
        }
    }

    @Test
    fun `When primary button is pressed for google pay, google pay is emitted`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = true,
            savedPaymentSelection = PaymentSelection.GooglePay,
            savedSelectionDataSource = FakeCustomerSheetSavedSelectionDataSource(
                savedSelection = CustomerSheetDataResult.success(SavedSelection.GooglePay),
            )
        )
        viewModel.result.test {
            assertThat(awaitItem()).isNull()
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            val result = awaitItem() as InternalCustomerSheetResult.Selected
            assertThat(result.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
        }
    }

    @Test
    fun `When primary button is pressed in the add payment flow, view should be loading`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
            intentDataSource = FakeCustomerSheetIntentDataSource(
                canCreateSetupIntents = false,
            ),
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                onAttachPaymentMethod = {
                    CustomerSheetDataResult.success(CARD_PAYMENT_METHOD)
                }
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
            )
        )

        viewModel.handleViewAction(CustomerSheetViewAction.OnFormFieldValuesCompleted(TEST_FORM_VALUES))

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf<AddPaymentMethod>()
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            val viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.isProcessing).isTrue()
            assertThat(viewState.enabled).isFalse()
            assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()
        }
    }

    @Test
    fun `When payment method could not be created, error message is visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.failure(
                    APIException(stripeError = StripeError(message = "Could not create payment method."))
                ),
            )
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnFormFieldValuesCompleted(
                formFieldValues = TEST_FORM_VALUES,
            )
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.errorMessage).isNull()
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.isProcessing).isTrue()
            viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.errorMessage).isEqualTo("Could not create payment method.".resolvableString)
            assertThat(viewState.isProcessing).isFalse()
        }
    }

    @Test
    fun `After attaching payment method with setup intent, methods are refreshed`() = runTest(testDispatcher) {
        val errorReporter = FakeErrorReporter()
        val viewModel = retrieveViewModelForAttaching(
            attachWithSetupIntent = true,
            shouldFailRefresh = false,
            errorReporter = errorReporter,
        )

        viewModel.viewState.test {
            assertThat(awaitViewState<AddPaymentMethod>().errorMessage).isNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isTrue()

            val newViewState = awaitViewState<SelectPaymentMethod>()
            assertThat(newViewState.errorMessage).isNull()
            assertThat(newViewState.isProcessing).isFalse()
            assertThat(newViewState.savedPaymentMethods).contains(CARD_PAYMENT_METHOD)
        }

        assertThat(errorReporter.getLoggedErrors())
            .contains(ErrorReporter.SuccessEvent.CUSTOMER_SHEET_PAYMENT_METHODS_REFRESH_SUCCESS.eventName)
    }

    @Test
    fun `if methods fail to refresh after attaching with setup intent, should dismiss and report event`() =
        runTest(testDispatcher) {
            val errorReporter = FakeErrorReporter()
            val viewModel = retrieveViewModelForAttaching(
                attachWithSetupIntent = true,
                errorReporter = errorReporter,
                shouldFailRefresh = true,
            )

            viewModel.viewState.test {
                assertThat(awaitViewState<AddPaymentMethod>().errorMessage).isNull()

                viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

                assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isTrue()
            }

            viewModel.result.test {
                assertThat(awaitItem()).isInstanceOf<InternalCustomerSheetResult.Canceled>()
            }

            assertThat(errorReporter.getLoggedErrors())
                .contains(ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_PAYMENT_METHODS_REFRESH_FAILURE.eventName)
        }

    @Test
    fun `After attaching payment method without setup intent, methods are refreshed`() = runTest(testDispatcher) {
        val errorReporter = FakeErrorReporter()
        val viewModel = retrieveViewModelForAttaching(
            attachWithSetupIntent = false,
            shouldFailRefresh = false,
            errorReporter = errorReporter,
        )

        viewModel.viewState.test {
            assertThat(awaitViewState<AddPaymentMethod>().errorMessage).isNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isTrue()

            val newViewState = awaitViewState<SelectPaymentMethod>()
            assertThat(newViewState.errorMessage).isNull()
            assertThat(newViewState.isProcessing).isFalse()
            assertThat(newViewState.savedPaymentMethods).contains(CARD_PAYMENT_METHOD)
        }

        assertThat(errorReporter.getLoggedErrors())
            .contains(ErrorReporter.SuccessEvent.CUSTOMER_SHEET_PAYMENT_METHODS_REFRESH_SUCCESS.eventName)
    }

    @Test
    fun `if methods fail to refresh after attaching without setup intent, should dismiss and report event`() =
        runTest(testDispatcher) {
            val errorReporter = FakeErrorReporter()
            val viewModel = retrieveViewModelForAttaching(
                attachWithSetupIntent = false,
                errorReporter = errorReporter,
                shouldFailRefresh = true,
            )

            viewModel.viewState.test {
                assertThat(awaitViewState<AddPaymentMethod>().errorMessage).isNull()

                viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

                assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isTrue()
            }

            viewModel.result.test {
                assertThat(awaitItem()).isInstanceOf<InternalCustomerSheetResult.Canceled>()
            }

            assertThat(errorReporter.getLoggedErrors())
                .contains(ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_PAYMENT_METHODS_REFRESH_FAILURE.eventName)
        }

    @Test
    fun `When payment method cannot be attached with setup intent, error message is visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
            intentDataSource = FakeCustomerSheetIntentDataSource(
                canCreateSetupIntents = true,
                onRetrieveSetupIntentClientSecret = {
                    CustomerSheetDataResult.success("invalid setup intent")
                },
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
                retrieveSetupIntent = Result.failure(
                    IllegalArgumentException("Invalid setup intent")
                ),
            ),
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnFormFieldValuesCompleted(
                formFieldValues = TEST_FORM_VALUES,
            )
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.errorMessage).isNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isTrue()

            viewState = awaitViewState()
            assertThat(viewState.errorMessage).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
            assertThat(viewState.enabled).isTrue()
            assertThat(viewState.isProcessing).isFalse()
        }
    }

    @Test
    fun `When setup intent provider error, error message is visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
            intentDataSource = FakeCustomerSheetIntentDataSource(
                canCreateSetupIntents = true,
                onRetrieveSetupIntentClientSecret = {
                    CustomerSheetDataResult.failure(
                        displayMessage = "Merchant provided error message",
                        cause = Exception("Some error"),
                    )
                },
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
            ),
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnFormFieldValuesCompleted(
                formFieldValues = TEST_FORM_VALUES,
            )
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.errorMessage).isNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isTrue()

            viewState = awaitViewState()
            assertThat(viewState.errorMessage).isEqualTo("Merchant provided error message".resolvableString)
            assertThat(viewState.isProcessing).isFalse()
        }
    }

    @Test
    fun `When payment method cannot be attached, error message is visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
            intentDataSource = FakeCustomerSheetIntentDataSource(
                canCreateSetupIntents = false,
            ),
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                onAttachPaymentMethod = {
                    CustomerSheetDataResult.failure(
                        cause = APIException(
                            stripeError = StripeError(
                                message = "Cannot attach payment method."
                            )
                        ),
                        displayMessage = "We couldn't save this payment method. Please try again."
                    )
                },
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
            ),
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnFormFieldValuesCompleted(formFieldValues = TEST_FORM_VALUES)
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.errorMessage).isNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isTrue()
            viewState = awaitViewState()
            assertThat(viewState.errorMessage)
                .isEqualTo("We couldn't save this payment method. Please try again.".resolvableString)
            assertThat(viewState.isProcessing).isFalse()
        }
    }

    @Test
    fun `When card form is complete, primary button should be enabled`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
        )

        viewModel.viewState.test {
            assertThat(awaitViewState<AddPaymentMethod>().primaryButtonEnabled).isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnFormFieldValuesCompleted(
                    formFieldValues = FormFieldValues(
                        fieldValuePairs = mapOf(
                            IdentifierSpec.Generic("test") to FormFieldEntry("test", true)
                        ),
                        userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
                    )
                )
            )

            assertThat(awaitViewState<AddPaymentMethod>().primaryButtonEnabled).isTrue()
        }
    }

    @Test
    fun `When card form is not complete, primary button should be disabled`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
        )

        viewModel.viewState.test {
            assertThat(awaitViewState<AddPaymentMethod>().primaryButtonEnabled).isFalse()
        }
    }

    @Test
    fun `When editing, primary button is not visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(
                CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                CARD_PAYMENT_METHOD.copy(id = "pm_2")
            ),
            savedPaymentSelection = PaymentSelection.Saved(
                paymentMethod = CARD_PAYMENT_METHOD.copy(id = "pm_2"),
            )
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnItemSelected(
                selection = PaymentSelection.Saved(
                    paymentMethod = CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                ),
            )
        )

        viewModel.viewState.test {
            assertThat(awaitViewState<SelectPaymentMethod>().primaryButtonVisible).isTrue()

            viewModel.handleViewAction(CustomerSheetViewAction.OnEditPressed)

            assertThat(awaitViewState<SelectPaymentMethod>().primaryButtonVisible).isFalse()
        }
    }

    @Test
    fun `When a new payment method is added, the primary button is visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(),
            isGooglePayAvailable = false,
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                paymentMethods = CustomerSheetDataResult.success(listOf(CARD_PAYMENT_METHOD)),
            ),
            intentDataSource = FakeCustomerSheetIntentDataSource(
                onRetrieveSetupIntentClientSecret = {
                    CustomerSheetDataResult.success("seti_123")
                }
            )
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnFormFieldValuesCompleted(
                formFieldValues = TEST_FORM_VALUES,
            )
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf<AddPaymentMethod>()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            assertThat(awaitViewState<AddPaymentMethod>().isProcessing)
                .isTrue()

            assertThat(awaitViewState<SelectPaymentMethod>().primaryButtonVisible)
                .isTrue()
        }
    }

    @Test
    fun `When removing the originally selected payment selection, primary button is not visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            savedPaymentSelection = PaymentSelection.Saved(
                paymentMethod = CARD_PAYMENT_METHOD.copy(id = "pm_2"),
            ),
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                paymentMethods = CustomerSheetDataResult.success(
                    listOf(
                        CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                        CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                    )
                ),
                onDetachPaymentMethod = {
                    CustomerSheetDataResult.success(
                        CARD_PAYMENT_METHOD.copy(id = "pm_2")
                    )
                }
            )
        )

        viewModel.viewState.test {
            assertThat(awaitViewState<SelectPaymentMethod>().primaryButtonVisible)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemRemoved(
                    CARD_PAYMENT_METHOD.copy(id = "pm_2")
                )
            )

            assertThat(awaitViewState<SelectPaymentMethod>().primaryButtonVisible)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnEditPressed
            )

            assertThat(awaitViewState<SelectPaymentMethod>().primaryButtonVisible)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnEditPressed
            )

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    PaymentSelection.Saved(
                        CARD_PAYMENT_METHOD.copy(id = "pm_1")
                    )
                )
            )

            skipItems(1)

            assertThat(awaitViewState<SelectPaymentMethod>().primaryButtonVisible)
                .isTrue()
        }
    }

    @Test
    fun `When removing the newly added payment, original payment selection is selected and primary button is not visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            savedPaymentSelection = PaymentSelection.Saved(
                CARD_PAYMENT_METHOD.copy(id = "pm_1"),
            ),
            customerPaymentMethods = listOf(CARD_PAYMENT_METHOD.copy(id = "pm_1")),
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                paymentMethods = CustomerSheetDataResult.success(
                    listOf(
                        CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                        CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                    )
                ),
                onDetachPaymentMethod = {
                    CustomerSheetDataResult.success(CARD_PAYMENT_METHOD.copy(id = "pm_2"))
                },
            ),
            intentDataSource = FakeCustomerSheetIntentDataSource(
                onRetrieveSetupIntentClientSecret = {
                    CustomerSheetDataResult.success("seti_123")
                },
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD.copy(id = "pm_2")),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
        )

        viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)
        viewModel.handleViewAction(CustomerSheetViewAction.OnFormFieldValuesCompleted(TEST_FORM_VALUES))

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf<AddPaymentMethod>()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isTrue()

            var viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.primaryButtonVisible).isTrue()

            viewModel.handleViewAction(CustomerSheetViewAction.OnEditPressed)

            viewState = awaitViewState()
            assertThat(viewState.primaryButtonVisible).isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemRemoved(CARD_PAYMENT_METHOD.copy(id = "pm_2"))
            )

            viewState = awaitViewState()
            assertThat(viewState.primaryButtonVisible).isFalse()

            viewModel.handleViewAction(CustomerSheetViewAction.OnEditPressed)

            viewState = awaitViewState()
            assertThat(viewState.primaryButtonVisible).isFalse()
            assertThat(viewState.paymentSelection)
                .isEqualTo(
                    PaymentSelection.Saved(CARD_PAYMENT_METHOD.copy(id = "pm_1"))
                )

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    PaymentSelection.Saved(CARD_PAYMENT_METHOD.copy(id = "pm_1"))
                )
            )

            expectNoEvents()
        }
    }

    @Test
    fun `Moving from screen to screen preserves state`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
        )

        viewModel.viewState.test {
            assertThat(awaitItem().asSelectState().isEditing).isFalse()

            viewModel.handleViewAction(CustomerSheetViewAction.OnEditPressed)

            assertThat(awaitItem().asSelectState().isEditing).isTrue()

            viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)

            assertThat(awaitItem())
                .isInstanceOf<AddPaymentMethod>()

            viewModel.handleViewAction(CustomerSheetViewAction.OnBackPressed)

            assertThat(awaitItem().asSelectState().isEditing).isTrue()
        }
    }

    @Test
    fun `When there is an initially selected PM, selecting another PM and cancelling should keep the original`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(
                CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                CARD_PAYMENT_METHOD.copy(id = "pm_2"),
            ),
            savedPaymentSelection = PaymentSelection.Saved(
                CARD_PAYMENT_METHOD.copy(id = "pm_2"),
            )
        )

        turbineScope {
            val resultTurbine = viewModel.result.testIn(this)
            val viewStateTurbine = viewModel.viewState.testIn(this)

            assertThat(resultTurbine.awaitItem()).isNull()

            assertThat(viewStateTurbine.awaitViewState<SelectPaymentMethod>().paymentSelection)
                .isEqualTo(
                    PaymentSelection.Saved(
                        CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                    )
                )

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    PaymentSelection.Saved(
                        CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                    )
                )
            )
            assertThat(viewStateTurbine.awaitViewState<SelectPaymentMethod>().paymentSelection)
                .isEqualTo(
                    PaymentSelection.Saved(
                        CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                    )
                )

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnDismissed
            )

            assertThat(resultTurbine.awaitItem())
                .isEqualTo(
                    InternalCustomerSheetResult.Canceled(
                        PaymentSelection.Saved(
                            CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                        )
                    )
                )

            resultTurbine.cancel()
            viewStateTurbine.cancel()
        }
    }

    @Test
    fun `If Google Pay is not available and config enables Google Pay, then Google Pay should not be enabled`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            configuration = CustomerSheet.Configuration(
                merchantDisplayName = "Example",
                googlePayEnabled = true,
            ),
            isGooglePayAvailable = false,
        )

        viewModel.viewState.test {
            assertThat(awaitViewState<SelectPaymentMethod>().isGooglePayEnabled).isFalse()
        }
    }

    @Test
    fun `If Google Pay is available and config enables Google Pay, then Google Pay should be enabled`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            configuration = CustomerSheet.Configuration(
                merchantDisplayName = "Example",
                googlePayEnabled = true,
            ),
            customerSheetLoader = FakeCustomerSheetLoader(
                isGooglePayAvailable = true,
            ),
        )

        viewModel.viewState.test {
            assertThat(awaitViewState<SelectPaymentMethod>().isGooglePayEnabled).isTrue()
        }
    }

    @Test
    fun `When select payment screen is presented, event is reported`() {
        val eventReporter: CustomerSheetEventReporter = mock()

        createViewModel(
            workContext = testDispatcher,
            eventReporter = eventReporter,
        )

        verify(eventReporter).onScreenPresented(CustomerSheetEventReporter.Screen.SelectPaymentMethod)
    }

    @Test
    fun `When add payment screen is presented, event is reported`() {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            eventReporter = eventReporter,
        )

        viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)

        verify(eventReporter).onScreenPresented(CustomerSheetEventReporter.Screen.AddPaymentMethod)
    }

    @Test
    fun `When edit payment screen is presented, no edit menu & event is reported`() = runTest {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(CARD_PAYMENT_METHOD),
            eventReporter = eventReporter,
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnModifyItem(paymentMethod = CARD_PAYMENT_METHOD)
        )

        viewModel.viewState.test {
            val state = awaitItem()

            assertThat(state).isInstanceOf<CustomerSheetViewState.EditPaymentMethod>()
            assertThat(state.topBarState {}.showEditMenu).isFalse()
        }

        verify(eventReporter).onScreenPresented(CustomerSheetEventReporter.Screen.EditPaymentMethod)
    }

    @Test
    fun `When edit payment screen is hidden, event is reported`() {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            eventReporter = eventReporter,
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnModifyItem(paymentMethod = CARD_PAYMENT_METHOD)
        )

        viewModel.handleViewAction(CustomerSheetViewAction.OnBackPressed)

        verify(eventReporter).onScreenHidden(CustomerSheetEventReporter.Screen.EditPaymentMethod)
    }

    @Test
    fun `When edit is tapped, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            eventReporter = eventReporter,
        )

        viewModel.viewState.test {
            viewModel.handleViewAction(CustomerSheetViewAction.OnEditPressed)
            verify(eventReporter).onEditTapped()
            viewModel.handleViewAction(CustomerSheetViewAction.OnEditPressed)
            verify(eventReporter).onEditCompleted()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When remove payment method succeeds, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                onDetachPaymentMethod = {
                    CustomerSheetDataResult.success(CARD_PAYMENT_METHOD)
                }
            ),
            eventReporter = eventReporter,
        )

        viewModel.viewState.test {
            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemRemoved(
                    CARD_PAYMENT_METHOD
                )
            )
            verify(eventReporter).onRemovePaymentMethodSucceeded()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When remove payment method failed, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                onDetachPaymentMethod = {
                    CustomerSheetDataResult.failure(
                        cause = Exception("Unable to detach payment option"),
                        displayMessage = "Something went wrong"
                    )
                }
            ),
            eventReporter = eventReporter,
        )

        viewModel.viewState.test {
            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemRemoved(
                    CARD_PAYMENT_METHOD
                )
            )
            verify(eventReporter).onRemovePaymentMethodFailed()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When google pay is confirmed, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = true,
            eventReporter = eventReporter,
        )

        viewModel.viewState.test {
            viewModel.handleViewAction(CustomerSheetViewAction.OnItemSelected(PaymentSelection.GooglePay))
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            verify(eventReporter).onConfirmPaymentMethodSucceeded("google_pay")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When google pay selection errors, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = true,
            savedSelectionDataSource = FakeCustomerSheetSavedSelectionDataSource(
                onSetSavedSelection = {
                    CustomerSheetDataResult.failure(
                        cause = Exception("Unable to set payment option"),
                        displayMessage = "Something went wrong"
                    )
                }
            ),
            eventReporter = eventReporter,
        )

        viewModel.viewState.test {
            viewModel.handleViewAction(CustomerSheetViewAction.OnItemSelected(PaymentSelection.GooglePay))
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            verify(eventReporter).onConfirmPaymentMethodFailed("google_pay")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When payment selection is confirmed, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            eventReporter = eventReporter,
        )

        viewModel.viewState.test {
            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    PaymentSelection.Saved(
                        CARD_PAYMENT_METHOD,
                    )
                )
            )
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            verify(eventReporter).onConfirmPaymentMethodSucceeded("card")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When payment selection errors, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            savedSelectionDataSource = FakeCustomerSheetSavedSelectionDataSource(
                onSetSavedSelection = {
                    CustomerSheetDataResult.failure(
                        cause = Exception("Unable to set payment option"),
                        displayMessage = "Something went wrong"
                    )
                }
            ),
            eventReporter = eventReporter,
        )

        viewModel.viewState.test {
            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    PaymentSelection.Saved(
                        CARD_PAYMENT_METHOD,
                    )
                )
            )
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            verify(eventReporter).onConfirmPaymentMethodFailed("card")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When attach without setup intent succeeds, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                onAttachPaymentMethod = {
                    CustomerSheetDataResult.success(CARD_PAYMENT_METHOD)
                },
            ),
            intentDataSource = FakeCustomerSheetIntentDataSource(
                canCreateSetupIntents = false,
            ),
            customerPaymentMethods = listOf(),
            isGooglePayAvailable = false,
            eventReporter = eventReporter,
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnFormFieldValuesCompleted(
                formFieldValues = TEST_FORM_VALUES,
            )
        )

        viewModel.viewState.test {
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            verify(eventReporter).onAttachPaymentMethodSucceeded(
                style = CustomerSheetEventReporter.AddPaymentMethodStyle.CreateAttach
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When attach without setup intent fails, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                onAttachPaymentMethod = {
                    CustomerSheetDataResult.failure(
                        cause = Exception("Unable to attach payment option"),
                        displayMessage = "Something went wrong"
                    )
                },
            ),
            intentDataSource = FakeCustomerSheetIntentDataSource(
                canCreateSetupIntents = false,
            ),
            customerPaymentMethods = listOf(),
            isGooglePayAvailable = false,
            eventReporter = eventReporter,
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnFormFieldValuesCompleted(
                formFieldValues = TEST_FORM_VALUES,
            )
        )

        viewModel.viewState.test {
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            verify(eventReporter).onAttachPaymentMethodFailed(
                style = CustomerSheetEventReporter.AddPaymentMethodStyle.CreateAttach
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When attach with setup intent succeeds, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
            intentDataSource = FakeCustomerSheetIntentDataSource(
                onRetrieveSetupIntentClientSecret = {
                    CustomerSheetDataResult.success("seti_123")
                },
                canCreateSetupIntents = true,
            ),
            customerPaymentMethods = listOf(),
            isGooglePayAvailable = false,
            eventReporter = eventReporter,
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnFormFieldValuesCompleted(
                formFieldValues = TEST_FORM_VALUES,
            )
        )

        viewModel.viewState.test {
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            verify(eventReporter).onAttachPaymentMethodSucceeded(
                style = CustomerSheetEventReporter.AddPaymentMethodStyle.SetupIntent
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When attach with setup intent fails, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
            intentDataSource = FakeCustomerSheetIntentDataSource(
                onRetrieveSetupIntentClientSecret = {
                    CustomerSheetDataResult.failure(
                        cause = Exception("Unable to retrieve setup intent"),
                        displayMessage = "Something went wrong"
                    )
                },
                canCreateSetupIntents = true,
            ),
            eventReporter = eventReporter,
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnFormFieldValuesCompleted(formFieldValues = TEST_FORM_VALUES)
        )

        viewModel.viewState.test {
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            verify(eventReporter).onAttachPaymentMethodFailed(
                style = CustomerSheetEventReporter.AddPaymentMethodStyle.SetupIntent
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When attach with setup intent handle next action fails, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(),
            isGooglePayAvailable = false,
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
            intentDataSource = FakeCustomerSheetIntentDataSource(
                onRetrieveSetupIntentClientSecret = {
                    CustomerSheetDataResult.success("seti_123")
                },
                canCreateSetupIntents = true,
            ),
            intentConfirmationInterceptor = FakeIntentConfirmationInterceptor().apply {
                enqueueFailureStep(
                    cause = Exception("Unable to confirm setup intent"),
                    message = "Something went wrong"
                )
            },
            eventReporter = eventReporter,
        )

        viewModel.handleViewAction(CustomerSheetViewAction.OnFormFieldValuesCompleted(TEST_FORM_VALUES))

        viewModel.viewState.test {
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            verify(eventReporter).onAttachPaymentMethodFailed(
                style = CustomerSheetEventReporter.AddPaymentMethodStyle.SetupIntent
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Payment method form changes on user selection`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(),
            isGooglePayAvailable = false,
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.paymentMethodCode)
                .isEqualTo("card")

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepositoryTestHelpers.usBankAccount
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.paymentMethodCode)
                .isEqualTo("us_bank_account")
        }
    }

    @Test
    fun `On payment method selection, should report event`() = runTest(testDispatcher) {
        val eventReporter = mock<CustomerSheetEventReporter>()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(),
            eventReporter = eventReporter,
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                LpmRepositoryTestHelpers.usBankAccount
            )
        )

        verify(eventReporter).onPaymentMethodSelected(PaymentMethod.Type.USBankAccount.code)
    }

    @Test
    fun `Payment method form elements are populated when switching payment method types`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(),
        )

        viewModel.viewState.test {
            awaitViewState<SelectPaymentMethod>()

            viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)

            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.paymentMethodCode)
                .isEqualTo("card")

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepositoryTestHelpers.usBankAccount
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.paymentMethodCode)
                .isEqualTo("us_bank_account")

            viewModel.handleViewAction(CustomerSheetViewAction.OnBackPressed)

            awaitViewState<SelectPaymentMethod>()

            viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)

            viewState = awaitViewState()
            assertThat(viewState.paymentMethodCode)
                .isEqualTo("us_bank_account")

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepositoryTestHelpers.card
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.paymentMethodCode)
                .isEqualTo("card")
            assertThat(viewState.formElements).isNotEmpty()
        }
    }

    @Test
    fun `Payment method user selection saved after returning to add screen`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
        )

        viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)

        viewModel.viewState.test {
            assertThat(
                awaitViewState<AddPaymentMethod>().paymentMethodCode
            ).isEqualTo("card")

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepositoryTestHelpers.usBankAccount
                )
            )

            assertThat(
                awaitViewState<AddPaymentMethod>().paymentMethodCode
            ).isEqualTo("us_bank_account")

            viewModel.handleViewAction(CustomerSheetViewAction.OnBackPressed)

            assertThat(
                awaitViewState<SelectPaymentMethod>()
            ).isInstanceOf<SelectPaymentMethod>()

            viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)

            assertThat(
                awaitViewState<AddPaymentMethod>().paymentMethodCode
            ).isEqualTo("us_bank_account")
        }
    }

    @Test
    fun `When the payment method form is us bank account, the primary button label is continue`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(R.string.stripe_paymentsheet_save.resolvableString)

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepositoryTestHelpers.usBankAccount
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(UiCoreR.string.stripe_continue_button_label.resolvableString)
        }
    }

    @Test
    fun `When 'paymentMethodOrder' is defined, initial shown payment method should be first from 'paymentMethodOrder'`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(
                workContext = testDispatcher,
                customerSheetLoader = FakeCustomerSheetLoader(
                    customerPaymentMethods = listOf(),
                    isGooglePayAvailable = false,
                    stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD_WITH_US_BANK_ACCOUNT,
                    financialConnectionsAvailable = true,
                ),
                configuration = CustomerSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    paymentMethodOrder = listOf("us_bank_account", "card")
                )
            )

            viewModel.viewState.test {
                val viewState = awaitViewState<AddPaymentMethod>()

                assertThat(viewState.paymentMethodCode).isEqualTo("us_bank_account")
            }
        }

    @Test
    fun `The custom primary button can be updated`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.customPrimaryButtonUiState)
                .isNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnUpdateCustomButtonUIState(
                    callback = {
                        PrimaryButton.UIState(
                            label = "Continue".resolvableString,
                            enabled = true,
                            lockVisible = false,
                            onClick = {}
                        )
                    }
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.customPrimaryButtonUiState)
                .isNotNull()
        }
    }

    @Test
    fun `The mandate text can be updated`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.mandateText)
                .isNull()
            assertThat(viewState.showMandateAbovePrimaryButton)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnUpdateMandateText(
                    mandateText = "This is a mandate.".resolvableString,
                    showAbovePrimaryButton = true,
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.mandateText)
                .isNotNull()
            assertThat(viewState.showMandateAbovePrimaryButton)
                .isTrue()
        }
    }

    @Test
    fun `US Bank Account can be created and attached`() = runTest(testDispatcher) {
        val usBankAccount = mockUSBankAccountPaymentSelection()
        val viewModel = createViewModel(
            workContext = testDispatcher,
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(US_BANK_ACCOUNT_VERIFIED),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                paymentMethods = CustomerSheetDataResult.success(listOf(US_BANK_ACCOUNT_VERIFIED)),
            ),
            intentDataSource = FakeCustomerSheetIntentDataSource(
                onRetrieveSetupIntentClientSecret = {
                    CustomerSheetDataResult.success("seti_123")
                },
                canCreateSetupIntents = true,
            ),
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf<AddPaymentMethod>()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepositoryTestHelpers.usBankAccount,
                )
            )

            assertThat(awaitItem()).isInstanceOf<AddPaymentMethod>()

            viewModel.handleViewAction(CustomerSheetViewAction.OnBankAccountSelectionChanged(usBankAccount))

            val viewStateAfterAdding = awaitViewState<AddPaymentMethod>()
            assertThat(viewStateAfterAdding.bankAccountSelection).isEqualTo(usBankAccount)

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            assertThat(awaitItem()).isInstanceOf<AddPaymentMethod>()

            val viewStateAfterConfirming = awaitViewState<SelectPaymentMethod>()

            assertThat(viewStateAfterConfirming.paymentSelection).isEqualTo(
                PaymentSelection.Saved(US_BANK_ACCOUNT_VERIFIED)
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When a form error is emitted, screen state is updated`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(),
            isGooglePayAvailable = false,
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.errorMessage)
                .isNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnFormError(
                    error = "This is an error.".resolvableString
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.errorMessage)
                .isEqualTo("This is an error.".resolvableString)
        }
    }

    @Test
    fun `When adding a US Bank account and user taps on scrim, a confirmation dialog should be visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                LpmRepositoryTestHelpers.usBankAccount,
            )
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.displayDismissConfirmationModal)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnBankAccountSelectionChanged(
                    paymentSelection = mockUSBankAccountPaymentSelection(),
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.displayDismissConfirmationModal)
                .isFalse()

            viewModel.bottomSheetConfirmStateChange()

            viewState = awaitViewState()
            assertThat(viewState.displayDismissConfirmationModal)
                .isTrue()
        }
    }

    @Test
    fun `When adding a Card and user taps on scrim, a confirmation dialog should not be visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
        )

        viewModel.viewState.test {
            val viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.displayDismissConfirmationModal)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepositoryTestHelpers.card,
                )
            )

            assertThat(viewState.displayDismissConfirmationModal)
                .isFalse()

            viewModel.bottomSheetConfirmStateChange()

            expectNoEvents()
        }
    }

    @Test
    fun `When user dismisses the confirmation dialog, the dialog should not be visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
            supportedPaymentMethods = listOf(LpmRepositoryTestHelpers.usBankAccount),
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                LpmRepositoryTestHelpers.usBankAccount,
            )
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.displayDismissConfirmationModal)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnBankAccountSelectionChanged(mockUSBankAccountPaymentSelection())
            )

            viewState = awaitViewState()
            assertThat(viewState.displayDismissConfirmationModal)
                .isFalse()

            viewModel.bottomSheetConfirmStateChange()

            viewState = awaitViewState()
            assertThat(viewState.displayDismissConfirmationModal)
                .isTrue()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnCancelClose
            )

            viewState = awaitViewState()
            assertThat(viewState.displayDismissConfirmationModal)
                .isFalse()
        }
    }

    @Test
    fun `When user confirms the confirmation dialog, the sheet should close`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                LpmRepositoryTestHelpers.usBankAccount,
            )
        )

        turbineScope {
            val viewStateTurbine = viewModel.viewState.testIn(backgroundScope)
            val resultTurbine = viewModel.result.testIn(backgroundScope)

            assertThat(resultTurbine.awaitItem()).isNull()

            var viewState = viewStateTurbine.awaitViewState<AddPaymentMethod>()
            assertThat(viewState.displayDismissConfirmationModal)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnBankAccountSelectionChanged(mockUSBankAccountPaymentSelection())
            )

            viewState = viewStateTurbine.awaitViewState()
            assertThat(viewState.displayDismissConfirmationModal)
                .isFalse()

            viewModel.bottomSheetConfirmStateChange()

            viewState = viewStateTurbine.awaitViewState()
            assertThat(viewState.displayDismissConfirmationModal)
                .isTrue()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnDismissed
            )

            viewStateTurbine.expectNoEvents()

            assertThat(resultTurbine.awaitItem())
                .isEqualTo(
                    InternalCustomerSheetResult.Canceled(null)
                )

            resultTurbine.cancel()
            viewStateTurbine.cancel()
        }
    }

    @Test
    fun `When in add flow and us bank account is retrieved, then shouldDisplayConfirmationDialog should be true`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                LpmRepositoryTestHelpers.usBankAccount,
            )
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnBankAccountSelectionChanged(mockUSBankAccountPaymentSelection())
        )

        viewModel.viewState.test {
            val viewState = awaitViewState<AddPaymentMethod>()
            assertThat(
                viewState.shouldDisplayDismissConfirmationModal()
            ).isTrue()
        }
    }

    @Test
    fun `Selecting the already selected payment method in add flow does nothing`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                LpmRepositoryTestHelpers.usBankAccount
            )
        )

        viewModel.viewState.test {
            val viewState = awaitViewState<AddPaymentMethod>()

            assertThat(viewState.paymentMethodCode)
                .isEqualTo(LpmRepositoryTestHelpers.usBankAccount.code)

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepositoryTestHelpers.usBankAccount
                )
            )

            expectNoEvents()
        }
    }

    @Test
    fun `When adding a us bank account and the account is retrieved, the primary button should say save`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
            supportedPaymentMethods = listOf(LpmRepositoryTestHelpers.usBankAccount),
        )

        viewModel.viewState.test {
            val viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(R.string.stripe_paymentsheet_save.resolvableString)
        }
    }

    @Test
    fun `When adding us bank and primary button says save, it should stay as save`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                LpmRepositoryTestHelpers.usBankAccount,
            )
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnBankAccountSelectionChanged(mockUSBankAccountPaymentSelection())
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(R.string.stripe_paymentsheet_save.resolvableString)

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepositoryTestHelpers.card
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(R.string.stripe_paymentsheet_save.resolvableString)

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepositoryTestHelpers.usBankAccount
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(R.string.stripe_paymentsheet_save.resolvableString)
        }
    }

    @Test
    fun `Mandate is required depending on payment method`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.mandateText)
                .isNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepositoryTestHelpers.usBankAccount
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.mandateText)
                .isNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnUpdateMandateText(
                    mandateText = "Mandate".resolvableString,
                    showAbovePrimaryButton = false
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.mandateText)
                .isNotNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepositoryTestHelpers.card
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.mandateText)
                .isNull()
        }
    }

    @Test
    fun `When confirming a US Bank Account, mandate text should be visible in select payment method screen`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(CARD_PAYMENT_METHOD, US_BANK_ACCOUNT),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.mandateText)
                .isNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    selection = PaymentSelection.Saved(
                        paymentMethod = US_BANK_ACCOUNT
                    )
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.mandateText)
                .isNotNull()
        }
    }

    @Test
    fun `A confirmed US Bank Account shouldn't show mandate when selected in select payment method screen`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            savedPaymentSelection = PaymentSelection.Saved(
                paymentMethod = US_BANK_ACCOUNT
            ),
            customerPaymentMethods = listOf(CARD_PAYMENT_METHOD, US_BANK_ACCOUNT),
        )

        viewModel.viewState.test {
            val viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.mandateText).isNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    selection = PaymentSelection.Saved(
                        paymentMethod = US_BANK_ACCOUNT
                    )
                )
            )

            expectNoEvents()
        }
    }

    @Test
    fun `When confirming a card, the card form should be reset when trying to add another card`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            intentDataSource = FakeCustomerSheetIntentDataSource(
                canCreateSetupIntents = false,
            ),
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                onAttachPaymentMethod = {
                    CustomerSheetDataResult.success(CARD_PAYMENT_METHOD)
                }
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
            )
        )

        viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf<AddPaymentMethod>()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnFormFieldValuesCompleted(
                    formFieldValues = FormFieldValues(
                        fieldValuePairs = mapOf(
                            IdentifierSpec.Generic("test") to FormFieldEntry("test", true)
                        ),
                        userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
                    )
                )
            )
            assertThat(awaitViewState<AddPaymentMethod>().formFieldValues).isNotNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            assertThat(awaitItem()).isInstanceOf<AddPaymentMethod>()
            assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()

            viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)

            val newViewState = awaitViewState<AddPaymentMethod>()
            assertThat(newViewState.formFieldValues).isNull()
        }
    }

    @Test
    fun `When attaching a non-verified bank account, the sheet closes and returns the account`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = listOf(),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(US_BANK_ACCOUNT),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
            intentDataSource = FakeCustomerSheetIntentDataSource(
                onRetrieveSetupIntentClientSecret = {
                    CustomerSheetDataResult.success("seti_123")
                },
                canCreateSetupIntents = true,
            ),
        )

        viewModel.handleViewAction(CustomerSheetViewAction.OnFormFieldValuesCompleted(TEST_FORM_VALUES))

        viewModel.result.test {
            assertThat(awaitItem()).isNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnBankAccountSelectionChanged(mockUSBankAccountPaymentSelection())
            )

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnPrimaryButtonPressed
            )

            assertThat(awaitItem())
                .isEqualTo(
                    InternalCustomerSheetResult.Selected(
                        PaymentSelection.Saved(US_BANK_ACCOUNT)
                    )
                )
        }
    }

    @Test
    fun `Removing payment method in edit screen goes through expected states when removing only payment method`() = runTest(testDispatcher) {
        val paymentMethods = PaymentMethodFactory.cards(size = 1)

        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = false,
            customerPaymentMethods = paymentMethods,
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()
            viewModel.handleViewAction(CustomerSheetViewAction.OnModifyItem(paymentMethods.single()))

            val editViewState = awaitViewState<CustomerSheetViewState.EditPaymentMethod>()
            editViewState.editPaymentMethodInteractor.handleViewAction(OnRemovePressed)

            expectNoEvents()
            editViewState.editPaymentMethodInteractor.handleViewAction(OnRemoveConfirmed)

            // Confirm that nothing has changed yet. We're waiting to remove the payment method
            // once we return to the SPM screen.
            val updatedViewState = awaitViewState<SelectPaymentMethod>()
            assertThat(updatedViewState.savedPaymentMethods).containsExactlyElementsIn(paymentMethods)

            // Show users that the payment method was removed briefly
            assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()
            assertThat(awaitItem()).isInstanceOf<AddPaymentMethod>()
        }
    }

    @Test
    fun `Removing payment method in edit screen goes through expected states when removing one of multiple payment methods`() = runTest(testDispatcher) {
        val paymentMethods = PaymentMethodFactory.cards(size = 2)

        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = paymentMethods,
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()
            viewModel.handleViewAction(CustomerSheetViewAction.OnModifyItem(paymentMethods.first()))

            val editViewState = awaitViewState<CustomerSheetViewState.EditPaymentMethod>()
            editViewState.editPaymentMethodInteractor.handleViewAction(OnRemovePressed)

            expectNoEvents()
            editViewState.editPaymentMethodInteractor.handleViewAction(OnRemoveConfirmed)

            // Confirm that nothing has changed yet. We're waiting to remove the payment method
            // once we return to the SPM screen.
            val updatedViewState = awaitViewState<SelectPaymentMethod>()
            assertThat(updatedViewState.savedPaymentMethods).containsExactlyElementsIn(paymentMethods)

            val finalViewState = awaitViewState<SelectPaymentMethod>()
            assertThat(finalViewState.savedPaymentMethods).containsExactly(paymentMethods.last())
        }
    }

    @Test
    fun `Updating payment method in edit screen goes through expected states & reports event`() =
        runTest(testDispatcher) {
            val eventReporter: CustomerSheetEventReporter = mock()
            val paymentMethods = PaymentMethodFactory.cards(size = 1)

            val firstMethod = paymentMethods.single()

            val updatedMethod = firstMethod.copy(
                card = firstMethod.card?.copy(
                    networks = PaymentMethod.Card.Networks(
                        available = setOf("visa", "cartes_bancaires"),
                        preferred = "visa"
                    )
                )
            )

            val paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                paymentMethods = CustomerSheetDataResult.Success(paymentMethods),
                onUpdatePaymentMethod = { _, _ ->
                    CustomerSheetDataResult.Success(updatedMethod)
                }
            )

            val viewModel = createViewModel(
                workContext = testDispatcher,
                eventReporter = eventReporter,
                customerPaymentMethods = paymentMethods,
                paymentMethodDataSource = paymentMethodDataSource,
                editInteractorFactory = createModifiableEditPaymentMethodViewInteractorFactory(
                    workContext = testDispatcher
                ),
            )

            viewModel.viewState.test {
                assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()
                viewModel.handleViewAction(CustomerSheetViewAction.OnModifyItem(firstMethod))

                val editViewState = awaitViewState<CustomerSheetViewState.EditPaymentMethod>()
                editViewState.editPaymentMethodInteractor.handleViewAction(
                    OnBrandChoiceChanged(
                        EditPaymentMethodViewState.CardBrandChoice(
                            brand = CardBrand.Visa
                        )
                    )
                )
                editViewState.editPaymentMethodInteractor.handleViewAction(OnUpdatePressed)

                // Confirm that nothing has changed yet. We're waiting to update the payment method
                // once we return to the SPM screen.
                val updatedViewState = awaitViewState<SelectPaymentMethod>()
                assertThat(updatedViewState.savedPaymentMethods).containsExactlyElementsIn(paymentMethods)

                verify(eventReporter).onUpdatePaymentMethodSucceeded(CardBrand.Visa)

                val finalViewState = awaitViewState<SelectPaymentMethod>()
                assertThat(finalViewState.savedPaymentMethods).containsExactlyElementsIn(listOf(updatedMethod))
            }
        }

    @Test
    fun `Card Brand Choice should be enabled in 'SelectPaymentMethod' after attaching first payment method`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(
                workContext = testDispatcher,
                stripeRepository = FakeStripeRepository(
                    createPaymentMethodResult = Result.success(CARD_WITH_NETWORKS_PAYMENT_METHOD)
                ),
                customerSheetLoader = FakeCustomerSheetLoader(
                    customerPaymentMethods = listOf(),
                    paymentSelection = null,
                    isGooglePayAvailable = false,
                    cbcEligibility = CardBrandChoiceEligibility.Eligible(
                        preferredNetworks = listOf(CardBrand.CartesBancaires)
                    ),
                ),
                intentDataSource = FakeCustomerSheetIntentDataSource(
                    canCreateSetupIntents = false,
                ),
                paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                    onAttachPaymentMethod = {
                        CustomerSheetDataResult.success(CARD_WITH_NETWORKS_PAYMENT_METHOD)
                    }
                )
            )

            viewModel.viewState.test {
                // Skip initial add state
                awaitViewState<AddPaymentMethod>()

                viewModel.handleViewAction(
                    CustomerSheetViewAction.OnFormFieldValuesCompleted(
                        formFieldValues = FormFieldValues(
                            fieldValuePairs = mapOf(
                                IdentifierSpec.Generic("test") to FormFieldEntry("test", true)
                            ),
                            userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
                        )
                    )
                )

                // Skip updated add state
                awaitViewState<AddPaymentMethod>()

                viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

                // Skip updated add state
                awaitViewState<AddPaymentMethod>()

                val selectPaymentMethodState = awaitViewState<SelectPaymentMethod>()

                assertThat(selectPaymentMethodState.isCbcEligible).isTrue()
            }
        }

    @Test
    fun `Updating original selection then dismissing 'CustomerSheet' should have updated PM in Canceled result`() =
        runTest(testDispatcher) {
            val paymentMethods = PaymentMethodFactory.cards(size = 4)

            val originalPaymentMethod = paymentMethods.first()

            val updatedPaymentMethod = originalPaymentMethod.copy(
                card = originalPaymentMethod.card?.copy(
                    networks = PaymentMethod.Card.Networks(
                        available = setOf("visa", "cartes_bancaires"),
                        preferred = "visa"
                    )
                )
            )

            val viewModel = retrieveViewModelForUpdating(
                savedPaymentMethods = paymentMethods,
                originalSelection = PaymentSelection.Saved(originalPaymentMethod),
                updatedPaymentMethod = updatedPaymentMethod,
            )

            viewModel.updatePaymentMethod(
                originalPaymentMethod = originalPaymentMethod,
                updatedPaymentMethod = updatedPaymentMethod
            )

            viewModel.result.test {
                // Skip the initial null item
                skipItems(1)

                viewModel.handleViewAction(CustomerSheetViewAction.OnBackPressed)

                val savedPaymentSelection = awaitItem().retrieveCanceledPaymentSelection()

                assertThat(savedPaymentSelection.paymentMethod).isEqualTo(updatedPaymentMethod)
            }
        }

    @Test
    fun `Updating current selection should have updated PM in view state`() =
        runTest(testDispatcher) {
            val paymentMethods = PaymentMethodFactory.cards(size = 4)

            val originalPaymentMethod = paymentMethods.last()
            val updatedPaymentMethod = originalPaymentMethod.copy(
                card = originalPaymentMethod.card?.copy(
                    networks = PaymentMethod.Card.Networks(
                        available = setOf("visa", "cartes_bancaires"),
                        preferred = "visa"
                    )
                )
            )

            val viewModel = retrieveViewModelForUpdating(
                savedPaymentMethods = paymentMethods,
                originalSelection = PaymentSelection.Saved(paymentMethods.first()),
                updatedPaymentMethod = updatedPaymentMethod,
            )

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    selection = PaymentSelection.Saved(originalPaymentMethod)
                )
            )

            viewModel.updatePaymentMethod(
                originalPaymentMethod = originalPaymentMethod,
                updatedPaymentMethod = updatedPaymentMethod
            )

            viewModel.viewState.test {
                val viewState = awaitViewState<SelectPaymentMethod>()

                assertThat(viewState.paymentSelection).isEqualTo(PaymentSelection.Saved(updatedPaymentMethod))
            }
        }

    @Test
    fun `Failed update payment method in edit screen reports event`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()
        val paymentMethods = PaymentMethodFactory.cards(size = 1)

        val firstMethod = paymentMethods.single()

        val paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
            paymentMethods = CustomerSheetDataResult.Success(paymentMethods),
            onUpdatePaymentMethod = { _, _ ->
                CustomerSheetDataResult.failure(
                    Exception("No network found!"),
                    "No network found!"
                )
            }
        )

        val viewModel = createViewModel(
            workContext = testDispatcher,
            eventReporter = eventReporter,
            customerPaymentMethods = paymentMethods,
            paymentMethodDataSource = paymentMethodDataSource,
            editInteractorFactory = createModifiableEditPaymentMethodViewInteractorFactory(
                workContext = testDispatcher
            ),
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()
            viewModel.handleViewAction(CustomerSheetViewAction.OnModifyItem(firstMethod))

            val editViewState = awaitViewState<CustomerSheetViewState.EditPaymentMethod>()
            editViewState.editPaymentMethodInteractor.handleViewAction(
                OnBrandChoiceChanged(
                    EditPaymentMethodViewState.CardBrandChoice(
                        brand = CardBrand.Visa
                    )
                )
            )
            editViewState.editPaymentMethodInteractor.handleViewAction(OnUpdatePressed)

            verify(eventReporter).onUpdatePaymentMethodFailed(
                eq(CardBrand.Visa),
                argThat {
                    message == "No network found!"
                }
            )
        }
    }

    @Test
    fun `Showing payment option brands in edit screen reports event`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()
        val paymentMethods = listOf(CARD_WITH_NETWORKS_PAYMENT_METHOD)

        val viewModel = createViewModel(
            workContext = testDispatcher,
            eventReporter = eventReporter,
            customerPaymentMethods = paymentMethods
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()
            viewModel.handleViewAction(CustomerSheetViewAction.OnModifyItem(paymentMethods.single()))

            val editViewState = awaitViewState<CustomerSheetViewState.EditPaymentMethod>()
            editViewState.editPaymentMethodInteractor.handleViewAction(
                EditPaymentMethodViewAction.OnBrandChoiceOptionsShown
            )

            verify(eventReporter).onShowPaymentOptionBrands(
                source = CustomerSheetEventReporter.CardBrandChoiceEventSource.Edit,
                selectedBrand = CardBrand.CartesBancaires
            )
        }
    }

    @Test
    fun `Hiding payment option brands in edit screen reports event`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()
        val paymentMethods = listOf(CARD_WITH_NETWORKS_PAYMENT_METHOD)

        val viewModel = createViewModel(
            workContext = testDispatcher,
            eventReporter = eventReporter,
            customerPaymentMethods = paymentMethods,
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()
            viewModel.handleViewAction(CustomerSheetViewAction.OnModifyItem(paymentMethods.single()))

            val editViewState = awaitViewState<CustomerSheetViewState.EditPaymentMethod>()
            editViewState.editPaymentMethodInteractor.handleViewAction(
                EditPaymentMethodViewAction.OnBrandChoiceOptionsDismissed
            )

            verify(eventReporter).onHidePaymentOptionBrands(
                source = CustomerSheetEventReporter.CardBrandChoiceEventSource.Edit,
                selectedBrand = null
            )
        }
    }

    @Test
    fun `Changing payment option brand in edit screen reports event`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()
        val paymentMethods = listOf(CARD_WITH_NETWORKS_PAYMENT_METHOD)

        val viewModel = createViewModel(
            workContext = testDispatcher,
            eventReporter = eventReporter,
            customerPaymentMethods = paymentMethods,
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()
            viewModel.handleViewAction(CustomerSheetViewAction.OnModifyItem(paymentMethods.single()))

            val editViewState = awaitViewState<CustomerSheetViewState.EditPaymentMethod>()
            editViewState.editPaymentMethodInteractor.handleViewAction(
                OnBrandChoiceChanged(
                    EditPaymentMethodViewState.CardBrandChoice(brand = CardBrand.Visa)
                )
            )

            verify(eventReporter).onHidePaymentOptionBrands(
                source = CustomerSheetEventReporter.CardBrandChoiceEventSource.Edit,
                selectedBrand = CardBrand.Visa
            )
        }
    }

    @Test
    fun `Modifying a payment method does not show remove`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()
        val paymentMethods = listOf(CARD_WITH_NETWORKS_PAYMENT_METHOD)

        val viewModel = createViewModel(
            workContext = testDispatcher,
            eventReporter = eventReporter,
            customerPaymentMethods = paymentMethods,
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()
            viewModel.handleViewAction(CustomerSheetViewAction.OnModifyItem(paymentMethods.single()))

            val editViewState = awaitViewState<CustomerSheetViewState.EditPaymentMethod>()
            editViewState.editPaymentMethodInteractor.handleViewAction(
                OnBrandChoiceChanged(
                    EditPaymentMethodViewState.CardBrandChoice(brand = CardBrand.Visa)
                )
            )

            verify(eventReporter).onHidePaymentOptionBrands(
                source = CustomerSheetEventReporter.CardBrandChoiceEventSource.Edit,
                selectedBrand = CardBrand.Visa
            )
        }
    }

    @Test
    fun `Removing the current and original payment selection results in the selection being null`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(CARD_PAYMENT_METHOD, US_BANK_ACCOUNT),
            savedPaymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD),
        )

        viewModel.viewState.test {
            val initialViewState = awaitViewState<SelectPaymentMethod>()
            assertThat(initialViewState.savedPaymentMethods)
                .containsExactly(CARD_PAYMENT_METHOD, US_BANK_ACCOUNT).inOrder()
            assertThat(initialViewState.paymentSelection)
                .isEqualTo(PaymentSelection.Saved(CARD_PAYMENT_METHOD))

            viewModel.handleViewAction(CustomerSheetViewAction.OnItemRemoved(CARD_PAYMENT_METHOD))

            val finalViewState = awaitViewState<SelectPaymentMethod>()
            assertThat(finalViewState.savedPaymentMethods).containsExactly(US_BANK_ACCOUNT).inOrder()
            assertThat(finalViewState.paymentSelection).isNull()
        }
    }

    @Test
    fun `Removing original selection from edit screen then dismissing 'CustomerSheet' should have null Canceled result`() =
        runTest(testDispatcher) {
            val paymentMethods = PaymentMethodFactory.cards(size = 4)
            val paymentMethodToRemove = paymentMethods.first()

            val viewModel = retrieveViewModelForRemoving(
                savedPaymentMethods = paymentMethods,
                originalSelection = PaymentSelection.Saved(paymentMethodToRemove),
                paymentMethodToRemove = paymentMethodToRemove
            )

            viewModel.removePaymentMethodFromEditScreen(paymentMethodToRemove)

            viewModel.result.test {
                // Skip the initial null item
                skipItems(1)

                viewModel.handleViewAction(CustomerSheetViewAction.OnBackPressed)

                val canceledResult = awaitItem().asCanceled()

                assertThat(canceledResult.paymentSelection).isNull()
            }
        }

    @Test
    fun `Removing current selection from edit screen should update editable state if last PM cannot be removed`() =
        runTest(testDispatcher) {
            val paymentMethods = PaymentMethodFactory.cards(size = 2)
            val paymentMethodToRemove = paymentMethods.last()

            val viewModel = retrieveViewModelForRemoving(
                savedPaymentMethods = paymentMethods,
                originalSelection = PaymentSelection.Saved(paymentMethodToRemove),
                paymentMethodToRemove = paymentMethodToRemove,
                allowsRemovalOfLastSavedPaymentMethod = false
            )

            viewModel.viewState.test {
                val viewState = awaitViewState<SelectPaymentMethod>()

                assertThat(viewState.topBarState {}.showEditMenu).isTrue()

                viewModel.handleViewAction(CustomerSheetViewAction.OnEditPressed)

                val viewStateAfterClickingEdit = awaitViewState<SelectPaymentMethod>()

                assertThat(viewStateAfterClickingEdit.isEditing).isTrue()
                assertThat(viewStateAfterClickingEdit.topBarState {}.showEditMenu).isTrue()
            }

            viewModel.removePaymentMethodFromEditScreen(paymentMethodToRemove)

            viewModel.viewState.test {
                val viewStateAfterRemoval = awaitViewState<SelectPaymentMethod>()

                assertThat(viewStateAfterRemoval.isEditing).isFalse()
                assertThat(viewStateAfterRemoval.topBarState {}.showEditMenu).isFalse()
            }
        }

    @Test
    fun `When card number input is completed, should report event`() = runTest(testDispatcher) {
        val eventReporter = mock<CustomerSheetEventReporter>()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            eventReporter = eventReporter,
        )

        viewModel.handleViewAction(CustomerSheetViewAction.OnCardNumberInputCompleted)

        verify(eventReporter).onCardNumberCompleted()
    }

    @Test
    fun `When disallowed brand entered, should report event`() = runTest(testDispatcher) {
        val eventReporter = mock<CustomerSheetEventReporter>()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            eventReporter = eventReporter,
        )

        viewModel.handleViewAction(CustomerSheetViewAction.OnDisallowedCardBrandEntered(CardBrand.AmericanExpress))

        verify(eventReporter).onDisallowedCardBrandEntered(CardBrand.AmericanExpress)
    }

    @Test
    fun `When setting up with intent, should call 'IntentConfirmationInterceptor' with expected params`() =
        runTest(testDispatcher) {
            val intentConfirmationInterceptor = FakeIntentConfirmationInterceptor()

            val viewModel = createViewModel(
                workContext = testDispatcher,
                customerPaymentMethods = listOf(),
                isGooglePayAvailable = false,
                stripeRepository = FakeStripeRepository(
                    createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
                    retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
                ),
                intentDataSource = FakeCustomerSheetIntentDataSource(
                    onRetrieveSetupIntentClientSecret = {
                        CustomerSheetDataResult.success("seti_123")
                    },
                    canCreateSetupIntents = true,
                ),
                intentConfirmationInterceptor = intentConfirmationInterceptor,
            )

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnFormFieldValuesCompleted(
                    formFieldValues = TEST_FORM_VALUES,
                )
            )

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            val call = intentConfirmationInterceptor.calls.awaitItem()

            assertThat(call).isEqualTo(
                FakeIntentConfirmationInterceptor.InterceptCall.WithExistingPaymentMethod(
                    initializationMode = PaymentElementLoader.InitializationMode.SetupIntent(
                        clientSecret = "seti_123"
                    ),
                    paymentMethod = CARD_PAYMENT_METHOD,
                    shippingValues = null,
                    paymentMethodOptionsParams = null,
                )
            )

            intentConfirmationInterceptor.calls.ensureAllEventsConsumed()
        }

    @Test
    fun `If has remove permissions, can remove should be true in state`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(CARD_PAYMENT_METHOD),
            customerPermissions = CustomerPermissions(
                canRemovePaymentMethods = true,
            ),
        )

        val selectState = viewModel.viewState.value.asSelectState()

        assertThat(selectState.canRemovePaymentMethods).isTrue()
        assertThat(selectState.canEdit).isTrue()
        assertThat(selectState.topBarState {}.showEditMenu).isTrue()
    }

    @Test
    fun `If has no remove permissions, can remove should be false in state`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(CARD_PAYMENT_METHOD),
            customerPermissions = CustomerPermissions(
                canRemovePaymentMethods = false,
            ),
        )

        val selectState = viewModel.viewState.value.asSelectState()

        assertThat(selectState.canRemovePaymentMethods).isFalse()
        assertThat(selectState.canEdit).isFalse()
        assertThat(selectState.topBarState {}.showEditMenu).isFalse()
    }

    @Test
    fun `If has no remove permissions but is CBC eligible, can remove is false but can edit is true`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(
                workContext = testDispatcher,
                customerPaymentMethods = listOf(CARD_WITH_NETWORKS_PAYMENT_METHOD),
                cbcEligibility = CardBrandChoiceEligibility.Eligible(
                    preferredNetworks = listOf(CardBrand.CartesBancaires),
                ),
                customerPermissions = CustomerPermissions(
                    canRemovePaymentMethods = false,
                ),
            )

            val selectState = viewModel.viewState.value.asSelectState()

            assertThat(selectState.canRemovePaymentMethods).isFalse()
            assertThat(selectState.canEdit).isTrue()
            assertThat(selectState.topBarState {}.showEditMenu).isTrue()
        }

    @Test
    fun `If refreshed payment methods does not contain newly added payment method, keep original selection`() =
        runTest(testDispatcher) {
            val attachedPaymentMethod = PaymentMethodFactory.card(random = true)
            val paymentMethods = PaymentMethodFactory.cards(size = 4)
            val originalSelection = PaymentSelection.Saved(paymentMethods[0])

            val viewModel = retrieveViewModelForAttaching(
                attachWithSetupIntent = true,
                shouldFailRefresh = false,
                originalSelection = originalSelection,
                originalPaymentMethods = paymentMethods,
                attachedPaymentMethod = attachedPaymentMethod,
                refreshedPaymentMethods = paymentMethods,
            )

            viewModel.viewState.test {
                assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isFalse()

                viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

                assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isTrue()

                val newViewState = awaitViewState<SelectPaymentMethod>()

                assertThat(newViewState.savedPaymentMethods).containsExactlyElementsIn(paymentMethods)
                assertThat(newViewState.paymentSelection).isEqualTo(originalSelection)
            }
        }

    @Test
    fun `If refreshed payment methods does contain newly added payment method, use new selection & sort PMs`() =
        runTest(testDispatcher) {
            val attachedPaymentMethod = PaymentMethodFactory.card(random = true)
            val paymentMethods = PaymentMethodFactory.cards(size = 4)
            val originalSelection = PaymentSelection.Saved(paymentMethods[0])
            val newSelection = PaymentSelection.Saved(attachedPaymentMethod)

            val viewModel = retrieveViewModelForAttaching(
                attachWithSetupIntent = true,
                shouldFailRefresh = false,
                originalSelection = originalSelection,
                originalPaymentMethods = paymentMethods,
                attachedPaymentMethod = attachedPaymentMethod,
                refreshedPaymentMethods = paymentMethods + listOf(attachedPaymentMethod),
            )

            viewModel.viewState.test {
                assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isFalse()

                viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

                assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isTrue()

                val newViewState = awaitViewState<SelectPaymentMethod>()

                assertThat(newViewState.savedPaymentMethods)
                    .containsExactlyElementsIn(listOf(attachedPaymentMethod) + paymentMethods)
                    .inOrder()
                assertThat(newViewState.paymentSelection).isEqualTo(newSelection)
            }
        }

    @Test
    fun `When removing an un-selected card, selection should be maintained & primary button should not visible`() =
        runTest(testDispatcher) {
            val paymentMethods = PaymentMethodFactory.cards(size = 3)
            val paymentMethodToRemove = paymentMethods.last()
            val originalSelection = PaymentSelection.Saved(paymentMethods.first())

            val viewModel = retrieveViewModelForRemoving(
                savedPaymentMethods = paymentMethods,
                originalSelection = originalSelection,
                paymentMethodToRemove = paymentMethodToRemove
            )

            viewModel.handleViewAction(CustomerSheetViewAction.OnItemRemoved(paymentMethodToRemove))

            viewModel.viewState.test {
                val currentState = awaitViewState<SelectPaymentMethod>()

                assertThat(currentState.isEditing).isFalse()
                assertThat(currentState.savedPaymentMethods).containsExactlyElementsIn(paymentMethods.dropLast(1))
                assertThat(currentState.paymentSelection).isEqualTo(originalSelection)
                assertThat(currentState.primaryButtonVisible).isFalse()
            }
        }

    @Test
    fun `When updating the original selection, original selection should be updated even if current selection is not the original`() =
        runTest(testDispatcher) {
            val paymentMethods = PaymentMethodFactory.cards(size = 3)

            val paymentMethodToUpdate = paymentMethods.first()
            val updatedPaymentMethod = paymentMethodToUpdate.copy(
                card = paymentMethodToUpdate.card?.copy(
                    expiryYear = 2030,
                    expiryMonth = 4
                )
            )

            val originalSelection = PaymentSelection.Saved(paymentMethodToUpdate)

            val viewModel = retrieveViewModelForUpdating(
                savedPaymentMethods = paymentMethods,
                originalSelection = originalSelection,
                updatedPaymentMethod = updatedPaymentMethod,
            )

            viewModel.updatePaymentMethod(
                originalPaymentMethod = paymentMethodToUpdate,
                updatedPaymentMethod = updatedPaymentMethod
            )

            viewModel.viewState.test {
                val currentState = awaitViewState<SelectPaymentMethod>()

                assertThat(currentState.isEditing).isFalse()
                assertThat(currentState.savedPaymentMethods)
                    .containsExactlyElementsIn(listOf(updatedPaymentMethod) + paymentMethods.drop(1))
                assertThat(currentState.primaryButtonVisible).isFalse()
            }

            viewModel.handleViewAction(CustomerSheetViewAction.OnDismissed)

            viewModel.result.test {
                val result = awaitItem()

                assertThat(result).isInstanceOf<InternalCustomerSheetResult.Canceled>()

                val canceledResult = result.asCanceled()

                assertThat(canceledResult.paymentSelection).isInstanceOf<PaymentSelection.Saved>()

                val savedSelection = canceledResult.paymentSelection.asSaved()

                assertThat(savedSelection.paymentMethod).isEqualTo(updatedPaymentMethod)
            }
        }

    private fun mockUSBankAccountPaymentSelection(): PaymentSelection.New.USBankAccount {
        return PaymentSelection.New.USBankAccount(
            labelResource = "Test",
            iconResource = 0,
            paymentMethodCreateParams = mock(),
            customerRequestedSave = mock(),
            input = PaymentSelection.New.USBankAccount.Input(
                name = "",
                email = null,
                phone = null,
                address = null,
                saveForFutureUse = false,
            ),
            instantDebits = null,
            screenState = BankFormScreenStateFactory.createWithSession("session_1234"),
        )
    }

    private fun retrieveViewModelForAttaching(
        attachWithSetupIntent: Boolean,
        shouldFailRefresh: Boolean,
        originalPaymentMethods: List<PaymentMethod> = listOf(),
        originalSelection: PaymentSelection.Saved? = null,
        attachedPaymentMethod: PaymentMethod = CARD_PAYMENT_METHOD,
        refreshedPaymentMethods: List<PaymentMethod> = listOf(CARD_PAYMENT_METHOD),
        errorReporter: ErrorReporter = FakeErrorReporter()
    ): CustomerSheetViewModel {
        return createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = originalPaymentMethods,
            isGooglePayAvailable = false,
            errorReporter = errorReporter,
            savedPaymentSelection = originalSelection,
            intentDataSource = FakeCustomerSheetIntentDataSource(
                canCreateSetupIntents = attachWithSetupIntent,
                onRetrieveSetupIntentClientSecret = {
                    CustomerSheetDataResult.success("seti_123")
                }
            ),
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                paymentMethods = if (shouldFailRefresh) {
                    CustomerSheetDataResult.failure(
                        cause = IllegalStateException("An error occurred!"),
                        displayMessage = null
                    )
                } else {
                    CustomerSheetDataResult.success(refreshedPaymentMethods)
                },
                onAttachPaymentMethod = {
                    CustomerSheetDataResult.success(attachedPaymentMethod)
                },
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(attachedPaymentMethod),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
        ).apply {
            if (originalPaymentMethods.isNotEmpty()) {
                handleViewAction(CustomerSheetViewAction.OnAddCardPressed)
            }

            handleViewAction(
                CustomerSheetViewAction.OnFormFieldValuesCompleted(
                    formFieldValues = TEST_FORM_VALUES,
                )
            )
        }
    }

    private suspend fun retrieveViewModelForUpdating(
        savedPaymentMethods: List<PaymentMethod>,
        originalSelection: PaymentSelection.Saved,
        updatedPaymentMethod: PaymentMethod
    ): CustomerSheetViewModel {
        return createViewModel(
            workContext = coroutineContext,
            savedPaymentSelection = originalSelection,
            customerPaymentMethods = savedPaymentMethods,
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                paymentMethods = CustomerSheetDataResult.success(savedPaymentMethods),
                onUpdatePaymentMethod = { _, _ ->
                    CustomerSheetDataResult.success(updatedPaymentMethod)
                }
            ),
            editInteractorFactory = createModifiableEditPaymentMethodViewInteractorFactory(
                workContext = testDispatcher
            ),
        )
    }

    private suspend fun retrieveViewModelForRemoving(
        savedPaymentMethods: List<PaymentMethod>,
        originalSelection: PaymentSelection.Saved,
        paymentMethodToRemove: PaymentMethod,
        allowsRemovalOfLastSavedPaymentMethod: Boolean = true,
    ): CustomerSheetViewModel {
        return createViewModel(
            workContext = coroutineContext,
            configuration = CustomerSheet.Configuration(
                merchantDisplayName = "Example",
                googlePayEnabled = true,
                allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod
            ),
            savedPaymentSelection = originalSelection,
            customerPaymentMethods = savedPaymentMethods,
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                paymentMethods = CustomerSheetDataResult.success(savedPaymentMethods),
                onDetachPaymentMethod = { _ ->
                    CustomerSheetDataResult.success(paymentMethodToRemove)
                },
            ),
            editInteractorFactory = createModifiableEditPaymentMethodViewInteractorFactory(
                workContext = testDispatcher
            ),
        )
    }

    private suspend fun CustomerSheetViewModel.updatePaymentMethod(
        originalPaymentMethod: PaymentMethod,
        updatedPaymentMethod: PaymentMethod
    ) {
        viewState.test {
            assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()

            handleViewAction(CustomerSheetViewAction.OnModifyItem(originalPaymentMethod))

            val editViewState = awaitViewState<CustomerSheetViewState.EditPaymentMethod>()
            editViewState.editPaymentMethodInteractor.handleViewAction(
                OnBrandChoiceChanged(
                    EditPaymentMethodViewState.CardBrandChoice(
                        brand = CardBrand.Visa
                    )
                )
            )
            editViewState.editPaymentMethodInteractor.handleViewAction(OnUpdatePressed)

            val updatedViewState = awaitViewState<SelectPaymentMethod>()
            assertThat(updatedViewState.savedPaymentMethods).contains(originalPaymentMethod)

            val finalViewState = awaitViewState<SelectPaymentMethod>()
            assertThat(finalViewState.savedPaymentMethods).contains(updatedPaymentMethod)
        }
    }

    private suspend fun CustomerSheetViewModel.removePaymentMethodFromEditScreen(
        paymentMethodToRemove: PaymentMethod,
    ) {
        viewState.test {
            assertThat(awaitItem()).isInstanceOf<SelectPaymentMethod>()

            handleViewAction(CustomerSheetViewAction.OnModifyItem(paymentMethodToRemove))

            val editViewState = awaitViewState<CustomerSheetViewState.EditPaymentMethod>()
            editViewState.editPaymentMethodInteractor.handleViewAction(OnRemoveConfirmed)

            val updatedViewState = awaitViewState<SelectPaymentMethod>()
            assertThat(updatedViewState.savedPaymentMethods).contains(paymentMethodToRemove)

            val finalViewState = awaitViewState<SelectPaymentMethod>()
            assertThat(finalViewState.savedPaymentMethods).doesNotContain(paymentMethodToRemove)
        }
    }

    private fun InternalCustomerSheetResult?.retrieveCanceledPaymentSelection(): PaymentSelection.Saved {
        assertThat(this).isInstanceOf<InternalCustomerSheetResult.Canceled>()

        val cancelled = asCanceled()

        assertThat(cancelled.paymentSelection).isInstanceOf<PaymentSelection>()

        return cancelled.paymentSelection.asSaved()
    }

    private fun InternalCustomerSheetResult?.asCanceled(): InternalCustomerSheetResult.Canceled {
        return this as InternalCustomerSheetResult.Canceled
    }

    private fun PaymentSelection?.asSaved(): PaymentSelection.Saved {
        return this as PaymentSelection.Saved
    }

    private fun CustomerSheetViewState.asSelectState(): SelectPaymentMethod {
        return this as SelectPaymentMethod
    }

    private fun CustomerSheetViewState.asAddState(): AddPaymentMethod {
        return this as AddPaymentMethod
    }

    private fun FormElement.asSectionElement(): SectionElement {
        return this as SectionElement
    }

    @Suppress("UNCHECKED_CAST")
    private suspend inline fun <R> ReceiveTurbine<*>.awaitViewState(): R {
        return awaitItem() as R
    }

    private companion object {
        val TEST_FORM_VALUES = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.Generic("test") to FormFieldEntry("test", true)
            ),
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
        )
    }
}
