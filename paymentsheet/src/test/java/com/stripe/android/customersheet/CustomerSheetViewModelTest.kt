package com.stripe.android.customersheet

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.cash.turbine.testIn
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.customersheet.CustomerSheetTestHelper.createViewModel
import com.stripe.android.customersheet.CustomerSheetViewState.AddPaymentMethod
import com.stripe.android.customersheet.CustomerSheetViewState.SelectPaymentMethod
import com.stripe.android.customersheet.analytics.CustomerSheetEventReporter
import com.stripe.android.customersheet.injection.CustomerSheetViewModelModule
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSheetViewModelTest {

    private val selectPaymentMethodViewState = SelectPaymentMethod(
        title = null,
        savedPaymentMethods = listOf(CARD_PAYMENT_METHOD),
        paymentSelection = null,
        isLiveMode = false,
        isProcessing = false,
        isEditing = false,
        isGooglePayEnabled = false,
        primaryButtonVisible = false,
        primaryButtonLabel = null,
    )

    private val addPaymentMethodViewState = AddPaymentMethod(
        paymentMethodCode = PaymentMethod.Type.Card.code,
        formViewData = FormViewModel.ViewData(
            completeFormValues = FormFieldValues(
                showsMandate = false,
                userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse,
            ),
        ),
        enabled = true,
        isLiveMode = false,
        isProcessing = false,
        isFirstPaymentMethod = false
    )

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
    fun `init emits CustomerSheetViewState#AddPaymentMethod when no payment methods available`() = runTest {
        val viewModel = createViewModel(
            isGooglePayAvailable = false,
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(listOf())
            )
        )
        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(
                AddPaymentMethod::class.java
            )
        }
    }

    @Test
    fun `init emits CustomerSheetViewState#SelectPaymentMethod when only google pay available`() = runTest {
        val viewModel = createViewModel(
            isGooglePayAvailable = true
        )
        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(
                SelectPaymentMethod::class.java
            )
        }
    }

    @Test
    fun `init emits CustomerSheetViewState#SelectPaymentMethod when payment methods available`() = runTest {
        val viewModel = createViewModel()
        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(
                SelectPaymentMethod::class.java
            )
        }
    }

    @Test
    fun `CustomerSheetViewAction#OnBackPressed emits canceled result`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState
            )
        )
        viewModel.result.test {
            assertThat(awaitItem()).isEqualTo(null)
            viewModel.handleViewAction(CustomerSheetViewAction.OnBackPressed)
            assertThat(awaitItem()).isEqualTo(InternalCustomerSheetResult.Canceled(null))
        }
    }

    @Test
    fun `When payment methods loaded, CustomerSheetViewState is populated`() = runTest {
        val viewModel = createViewModel(
            isGooglePayAvailable = false,
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(
                        CARD_PAYMENT_METHOD.id!!
                    )
                )
            )
        )
        viewModel.viewState.test {
            assertThat(awaitItem())
                .isEqualTo(
                    selectPaymentMethodViewState.copy(
                        savedPaymentMethods = listOf(
                            CARD_PAYMENT_METHOD,
                        ),
                        paymentSelection = PaymentSelection.Saved(
                            paymentMethod = CARD_PAYMENT_METHOD,
                        ),
                        primaryButtonLabel = "Confirm",
                    )
                )
        }
    }

    @Test
    fun `When payment methods cannot be loaded, sheet closes`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.failure(
                    cause = APIException(message = "Failed to retrieve payment methods."),
                    displayMessage = "We couldn't get your payment methods. Please try again."
                )
            )
        )
        viewModel.result.test {
            assertThat((awaitItem() as InternalCustomerSheetResult.Error).exception.message)
                .isEqualTo("Failed to retrieve payment methods.")
        }
    }

    @Test
    fun `When the selected payment method cannot be loaded, paymentSelection is null and an error message is displayed`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                selectedPaymentOption = CustomerAdapter.Result.failure(
                    cause = Exception("Failed to retrieve selected payment option."),
                    displayMessage = null,
                )
            )
        )
        viewModel.result.test {
            assertThat((awaitItem() as InternalCustomerSheetResult.Error).exception.message)
                .isEqualTo("Failed to retrieve selected payment option.")
        }
    }

    @Test
    fun `When the Google Pay is selected payment method, paymentSelection is GooglePay`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                selectedPaymentOption = CustomerAdapter.Result.success(CustomerAdapter.PaymentOption.GooglePay)
            )
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
    fun `When the Link is selected payment method, paymentSelection is Link`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                selectedPaymentOption = CustomerAdapter.Result.success(CustomerAdapter.PaymentOption.Link)
            )
        )
        viewModel.viewState.test {
            val viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.paymentSelection)
                .isEqualTo(PaymentSelection.Link)
            assertThat(viewState.errorMessage)
                .isEqualTo(null)
        }
    }

    @Test
    fun `When the payment method is selected payment method, paymentSelection is payment method`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(
                        CARD_PAYMENT_METHOD.id!!
                    )
                )
            )
        )
        viewModel.viewState.test {
            val viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.paymentSelection)
                .isInstanceOf(PaymentSelection.Saved::class.java)
            assertThat(viewState.errorMessage)
                .isEqualTo(null)
        }
    }

    @Test
    fun `providePaymentMethodName provides payment method name given code`() {
        val viewModel = createViewModel()
        val name = viewModel.providePaymentMethodName(PaymentMethod.Type.Card.code)
        assertThat(name)
            .isEqualTo("Card")
    }

    @Test
    fun `When selection, primary button label should not be null`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(
                        CARD_PAYMENT_METHOD.id!!
                    )
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
    fun `When no selection, the primary button is not visible`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(null)
            )
        )
        viewModel.viewState.test {
            assertThat(awaitViewState<SelectPaymentMethod>().primaryButtonVisible)
                .isFalse()
        }
    }

    @Test
    fun `When Stripe payment method is selected, the primary button is visible`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(null)
            )
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
                .isEqualTo("Confirm")
            assertThat(viewState.primaryButtonEnabled)
                .isTrue()
            assertThat(viewState.primaryButtonVisible)
                .isTrue()
        }
    }

    @Test
    fun `When Google Pay is selected, the primary button is visible`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(null)
            )
        )
        viewModel.viewState.test {
            var viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo("Confirm")
            assertThat(viewState.primaryButtonVisible)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    selection = PaymentSelection.GooglePay
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo("Confirm")
            assertThat(viewState.primaryButtonEnabled)
                .isTrue()
            assertThat(viewState.primaryButtonVisible)
                .isTrue()
        }
    }

    @Test
    fun `When CustomerViewAction#OnItemSelected with editing view state, payment selection should not be updated`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        CARD_PAYMENT_METHOD,
                        CARD_PAYMENT_METHOD.copy(id = "pm_2")
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(CARD_PAYMENT_METHOD.id!!)
                )
            )
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
    fun `When CustomerViewAction#OnItemSelected with Link, exception should be thrown`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(null)
            )
        )
        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(SelectPaymentMethod::class.java)
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
    fun `When CustomerViewAction#OnItemSelected with null, primary button label should be null`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(null)
            )
        )
        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(SelectPaymentMethod::class.java)
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
    fun `When the payment configuration is test, isLiveMode should be false`() = runTest {
        val viewModel = createViewModel(
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
    fun `When CustomerViewAction#OnAddCardPressed, view state is updated to CustomerViewAction#AddPaymentMethod`() = runTest {
        val viewModel = createViewModel()

        viewModel.viewState.test {
            assertThat(awaitItem())
                .isInstanceOf(SelectPaymentMethod::class.java)
            viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)
            assertThat(awaitItem())
                .isInstanceOf(AddPaymentMethod::class.java)
        }
    }

    @Test
    fun `When CustomerViewAction#OnEditPressed, view state isEditing should be updated`() = runTest {
        val viewModel = createViewModel()
        viewModel.viewState.test {
            var viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.isEditing)
                .isFalse()

            viewModel.handleViewAction(CustomerSheetViewAction.OnEditPressed)

            viewState = awaitViewState()
            assertThat(viewState.isEditing)
                .isTrue()
        }
    }

    @Test
    fun `When removing a payment method, payment method list should be updated`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        CARD_PAYMENT_METHOD,
                        CARD_PAYMENT_METHOD.copy(id = "pm_2")
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(
                        CARD_PAYMENT_METHOD.id!!
                    )
                )
            )
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
    fun `When removing last payment method & google pay disabled, should transition to add payment screen`() = runTest {
        val viewModel = createViewModel(
            isGooglePayAvailable = false,
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(
                        CARD_PAYMENT_METHOD.id!!
                    )
                )
            )
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

            val addPaymentMethodViewState = awaitViewState<AddPaymentMethod>()

            assertThat(addPaymentMethodViewState.isFirstPaymentMethod).isTrue()
        }
    }

    @Test
    fun `When removing a payment method fails, error message is displayed`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        CARD_PAYMENT_METHOD
                    )
                ),
                onDetachPaymentMethod = {
                    CustomerAdapter.Result.failure(
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
    fun `When primary button is pressed for saved payment method, selected payment method is emitted`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(
                        CARD_PAYMENT_METHOD.id!!
                    )
                )
            )
        )
        val viewStateTurbine = viewModel.viewState.testIn(backgroundScope)
        val resultTurbine = viewModel.result.testIn(backgroundScope)

        assertThat(viewStateTurbine.awaitItem()).isInstanceOf(SelectPaymentMethod::class.java)
        assertThat(resultTurbine.awaitItem()).isNull()

        viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

        assertThat(resultTurbine.awaitItem()).isInstanceOf(InternalCustomerSheetResult.Selected::class.java)
    }

    @Test
    fun `When primary button is pressed for saved payment method that cannot be saved, error message is emitted`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(
                        CARD_PAYMENT_METHOD.id!!
                    )
                ),
                onSetSelectedPaymentOption = {
                    CustomerAdapter.Result.failure(
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
    fun `When primary button is pressed for google pay, google pay is emitted`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState.copy(
                    isGooglePayEnabled = true,
                    paymentSelection = PaymentSelection.GooglePay,
                )
            ),
            customerAdapter = FakeCustomerAdapter(
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.GooglePay
                )
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
    fun `When primary button is pressed in the add payment flow, view should be loading`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState,
                addPaymentMethodViewState,
            ),
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = false,
                onAttachPaymentMethod = {
                    CustomerAdapter.Result.success(CARD_PAYMENT_METHOD)
                }
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
            )
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(AddPaymentMethod::class.java)
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            val viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.isProcessing).isTrue()
            assertThat(viewState.enabled).isFalse()
            assertThat(awaitItem()).isInstanceOf(SelectPaymentMethod::class.java)
        }
    }

    @Test
    fun `When payment method could not be created, error message is visible`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                addPaymentMethodViewState,
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.failure(
                    APIException(stripeError = StripeError(message = "Could not create payment method."))
                ),
            )
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.errorMessage).isNull()
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.isProcessing).isTrue()
            viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.errorMessage).isEqualTo("Could not create payment method.")
            assertThat(viewState.isProcessing).isFalse()
        }
    }

    @Test
    fun `Payment method is attached to customer with setup intent`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState,
                addPaymentMethodViewState,
            ),
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = true,
                onSetupIntentClientSecretForCustomerAttach = {
                    CustomerAdapter.Result.success("seti_123")
                },
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
        )

        viewModel.viewState.test {
            assertThat(awaitViewState<AddPaymentMethod>().errorMessage).isNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isTrue()

            val newViewState = awaitViewState<SelectPaymentMethod>()
            assertThat(newViewState.errorMessage).isNull()
            assertThat(newViewState.isProcessing).isFalse()
            assertThat(newViewState.savedPaymentMethods.contains(CARD_PAYMENT_METHOD))
        }
    }

    @Test
    fun `Payment method is attached to customer without setup intent`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState,
                addPaymentMethodViewState,
            ),
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = false,
                onAttachPaymentMethod = {
                    CustomerAdapter.Result.success(CARD_PAYMENT_METHOD)
                },
                onSetupIntentClientSecretForCustomerAttach = null,
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
            ),
        )

        viewModel.viewState.test {
            assertThat(awaitViewState<AddPaymentMethod>().errorMessage).isNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isTrue()

            val newViewState = awaitViewState<SelectPaymentMethod>()
            assertThat(newViewState.errorMessage).isNull()
            assertThat(newViewState.isProcessing).isFalse()
            assertThat(newViewState.savedPaymentMethods.contains(CARD_PAYMENT_METHOD))
        }
    }

    @Test
    fun `When payment method cannot be attached with setup intent, error message is visible`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState,
                addPaymentMethodViewState,
            ),
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = true,
                onSetupIntentClientSecretForCustomerAttach = {
                    CustomerAdapter.Result.success("invalid setup intent")
                },
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
                retrieveSetupIntent = Result.failure(
                    IllegalArgumentException("Invalid setup intent")
                ),
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.errorMessage).isNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isTrue()

            viewState = awaitViewState()
            assertThat(viewState.errorMessage).isEqualTo("Something went wrong")
            assertThat(viewState.enabled).isTrue()
            assertThat(viewState.isProcessing).isFalse()
        }
    }

    @Test
    fun `When setup intent provider is not provided, error message is visible`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState,
                addPaymentMethodViewState,
            ),
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = true,
                onSetupIntentClientSecretForCustomerAttach = null,
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.errorMessage).isNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isTrue()

            viewState = awaitViewState()
            assertThat(viewState.errorMessage).isEqualTo("Merchant provided error message")
            assertThat(viewState.isProcessing).isFalse()
        }
    }

    @Test
    fun `When payment method cannot be attached, error message is visible`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState,
                addPaymentMethodViewState,
            ),
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = false,
                onSetupIntentClientSecretForCustomerAttach = null,
                onAttachPaymentMethod = {
                    CustomerAdapter.Result.failure(
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

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.errorMessage).isNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            assertThat(awaitViewState<AddPaymentMethod>().isProcessing).isTrue()
            viewState = awaitViewState()
            assertThat(viewState.errorMessage).isEqualTo("We couldn't save this payment method. Please try again.")
            assertThat(viewState.isProcessing).isFalse()
        }
    }

    @Test
    fun `When card form is complete, primary button should be enabled`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                addPaymentMethodViewState.copy(
                    formViewData = FormViewModel.ViewData(),
                )
            ),
        )

        viewModel.viewState.test {
            assertThat(awaitViewState<AddPaymentMethod>().primaryButtonEnabled).isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnFormDataUpdated(
                    formData = FormViewModel.ViewData(
                        completeFormValues = FormFieldValues(
                            fieldValuePairs = mapOf(
                                IdentifierSpec.Generic("test") to FormFieldEntry("test", true)
                            ),
                            showsMandate = false,
                            userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
                        )
                    )
                )
            )

            assertThat(awaitViewState<AddPaymentMethod>().primaryButtonEnabled).isTrue()
        }
    }

    @Test
    fun `When card form is not complete, primary button should be disabled`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                addPaymentMethodViewState.copy(
                    formViewData = FormViewModel.ViewData()
                )
            ),
        )

        viewModel.viewState.test {
            assertThat(awaitViewState<AddPaymentMethod>().primaryButtonEnabled).isFalse()
        }
    }

    @Test
    fun `When editing, primary button is not visible`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState.copy(
                    primaryButtonVisible = true,
                )
            ),
        )

        viewModel.viewState.test {
            assertThat(awaitViewState<SelectPaymentMethod>().primaryButtonVisible).isTrue()

            viewModel.handleViewAction(CustomerSheetViewAction.OnEditPressed)

            assertThat(awaitViewState<SelectPaymentMethod>().primaryButtonVisible).isFalse()
        }
    }

    @Test
    fun `When a new payment method is added, the primary button is visible`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState,
                addPaymentMethodViewState,
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
            customerAdapter = FakeCustomerAdapter(
                onSetupIntentClientSecretForCustomerAttach = {
                    CustomerAdapter.Result.success("seti_123")
                }
            )
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(AddPaymentMethod::class.java)

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            assertThat(awaitViewState<AddPaymentMethod>().isProcessing)
                .isTrue()

            assertThat(awaitViewState<SelectPaymentMethod>().primaryButtonVisible)
                .isTrue()
        }
    }

    @Test
    fun `When removing the originally selected payment selection, primary button is not visible`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState.copy(
                    savedPaymentMethods = listOf(
                        CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                        CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                    ),
                    paymentSelection = PaymentSelection.Saved(
                        CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                    )
                ),
            ),
            customerAdapter = FakeCustomerAdapter(
                onDetachPaymentMethod = {
                    CustomerAdapter.Result.success(
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
    fun `When removing the newly added payment, original payment selection is selected and primary button is not visible`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState.copy(
                    savedPaymentMethods = listOf(
                        CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                    ),
                    paymentSelection = PaymentSelection.Saved(
                        CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                    ),
                ),
                addPaymentMethodViewState,
            ),
            savedPaymentSelection = PaymentSelection.Saved(
                CARD_PAYMENT_METHOD.copy(id = "pm_1"),
            ),
            customerAdapter = FakeCustomerAdapter(
                onDetachPaymentMethod = {
                    CustomerAdapter.Result.success(CARD_PAYMENT_METHOD.copy(id = "pm_2"))
                },
                onSetupIntentClientSecretForCustomerAttach = {
                    CustomerAdapter.Result.success("seti_123")
                }
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD.copy(id = "pm_2"),),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(AddPaymentMethod::class.java)

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
    fun `When there is a payment selection, the selected PM should be first in the list`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                        CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                        CARD_PAYMENT_METHOD.copy(id = "pm_3"),
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId("pm_3")
                )
            )
        )

        viewModel.viewState.test {
            val viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.savedPaymentMethods.indexOfFirst { it.id == "pm_3" }).isEqualTo(0)
        }
    }

    @Test
    fun `When there is no payment selection, the order of the payment methods is preserved`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                        CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                        CARD_PAYMENT_METHOD.copy(id = "pm_3"),
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(null)
            )
        )

        viewModel.viewState.test {
            val viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.savedPaymentMethods.indexOfFirst { it.id == "pm_1" }).isEqualTo(0)
            assertThat(viewState.savedPaymentMethods.indexOfFirst { it.id == "pm_2" }).isEqualTo(1)
            assertThat(viewState.savedPaymentMethods.indexOfFirst { it.id == "pm_3" }).isEqualTo(2)
        }
    }

    @Test
    fun `Moving from screen to screen preserves state`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState
            ),
        )

        viewModel.viewState.test {
            assertThat(awaitItem())
                .isEqualTo(selectPaymentMethodViewState)

            viewModel.handleViewAction(CustomerSheetViewAction.OnEditPressed)

            assertThat(awaitItem())
                .isEqualTo(selectPaymentMethodViewState.copy(isEditing = true))

            viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)

            assertThat(awaitItem())
                .isInstanceOf(AddPaymentMethod::class.java)

            viewModel.handleViewAction(CustomerSheetViewAction.OnBackPressed)

            assertThat(awaitItem())
                .isEqualTo(selectPaymentMethodViewState.copy(isEditing = true))
        }
    }

    @Test
    fun `When there is an initially selected PM, selecting another PM and cancelling should keep the original`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState.copy(
                    savedPaymentMethods = listOf(
                        CARD_PAYMENT_METHOD.copy(id = "pm_1"),
                        CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                    ),
                    paymentSelection = PaymentSelection.Saved(
                        CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                    ),
                )
            ),
            savedPaymentSelection = PaymentSelection.Saved(
                CARD_PAYMENT_METHOD.copy(id = "pm_2"),
            )
        )

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

    @Test
    fun `If Google Pay is not available and config enables Google Pay, then Google Pay should not be enabled`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                CustomerSheetViewState.Loading(false),
                selectPaymentMethodViewState,
            ),
            configuration = CustomerSheet.Configuration(
                googlePayEnabled = true,
            ),
            isGooglePayAvailable = false,
        )

        viewModel.viewState.test {
            assertThat(awaitViewState<SelectPaymentMethod>().isGooglePayEnabled).isFalse()
        }
    }

    @Test
    fun `If Google Pay is available and config enables Google Pay, then Google Pay should be enabled`() = runTest {
        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState,
                CustomerSheetViewState.Loading(false),
            ),
            configuration = CustomerSheet.Configuration(
                googlePayEnabled = true,
            ),
            isGooglePayAvailable = true,
        )

        viewModel.viewState.test {
            assertThat(awaitViewState<SelectPaymentMethod>().isGooglePayEnabled).isTrue()
        }
    }

    @Test
    fun `When select payment screen is presented, event is reported`() {
        val eventReporter: CustomerSheetEventReporter = mock()

        createViewModel(
            eventReporter = eventReporter,
        )

        verify(eventReporter).onScreenPresented(CustomerSheetEventReporter.Screen.SelectPaymentMethod)
    }

    @Test
    fun `When add payment screen is presented, event is reported`() {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            eventReporter = eventReporter,
        )

        viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)

        verify(eventReporter).onScreenPresented(CustomerSheetEventReporter.Screen.AddPaymentMethod)
    }

    @Test
    fun `When edit is tapped, event is reported`() = runTest {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
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
    fun `When remove payment method succeeds, event is reported`() = runTest {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                onDetachPaymentMethod = {
                    CustomerAdapter.Result.success(CARD_PAYMENT_METHOD)
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
    fun `When remove payment method failed, event is reported`() = runTest {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                onDetachPaymentMethod = {
                    CustomerAdapter.Result.failure(
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
    fun `When google pay is confirmed, event is reported`() = runTest {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState.copy(
                    isGooglePayEnabled = true,
                ),
            ),
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
    fun `When google pay selection errors, event is reported`() = runTest {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState.copy(
                    isGooglePayEnabled = true,
                ),
            ),
            customerAdapter = FakeCustomerAdapter(
                onSetSelectedPaymentOption = {
                    CustomerAdapter.Result.failure(
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
    fun `When payment selection is confirmed, event is reported`() = runTest {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState,
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
            verify(eventReporter).onConfirmPaymentMethodSucceeded("card")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When payment selection errors, event is reported`() = runTest {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState,
            ),
            customerAdapter = FakeCustomerAdapter(
                onSetSelectedPaymentOption = {
                    CustomerAdapter.Result.failure(
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
    fun `When attach without setup intent succeeds, event is reported`() = runTest {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState,
                addPaymentMethodViewState,
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
            customerAdapter = FakeCustomerAdapter(
                onAttachPaymentMethod = {
                    CustomerAdapter.Result.success(CARD_PAYMENT_METHOD)
                },
                canCreateSetupIntents = false,
            ),
            eventReporter = eventReporter,
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
    fun `When attach without setup intent fails, event is reported`() = runTest {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState,
                addPaymentMethodViewState,
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
            customerAdapter = FakeCustomerAdapter(
                onAttachPaymentMethod = {
                    CustomerAdapter.Result.failure(
                        cause = Exception("Unable to attach payment option"),
                        displayMessage = "Something went wrong"
                    )
                },
                canCreateSetupIntents = false,
            ),
            eventReporter = eventReporter,
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
    fun `When attach with setup intent succeeds, event is reported`() = runTest {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState,
                addPaymentMethodViewState,
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
            customerAdapter = FakeCustomerAdapter(
                onSetupIntentClientSecretForCustomerAttach = {
                    CustomerAdapter.Result.success("seti_123")
                },
                canCreateSetupIntents = true,
            ),
            eventReporter = eventReporter,
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
    fun `When attach with setup intent fails, event is reported`() = runTest {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState,
                addPaymentMethodViewState,
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
            customerAdapter = FakeCustomerAdapter(
                onSetupIntentClientSecretForCustomerAttach = {
                    CustomerAdapter.Result.failure(
                        cause = Exception("Unable to retrieve setup intent"),
                        displayMessage = "Something went wrong"
                    )
                },
                canCreateSetupIntents = true,
            ),
            eventReporter = eventReporter,
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
    fun `When attach with setup intent handle next action fails, event is reported`() = runTest {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            initialBackStack = listOf(
                selectPaymentMethodViewState,
                addPaymentMethodViewState,
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(CARD_PAYMENT_METHOD),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
            customerAdapter = FakeCustomerAdapter(
                onSetupIntentClientSecretForCustomerAttach = {
                    CustomerAdapter.Result.success("seti_123")
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

        viewModel.viewState.test {
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            verify(eventReporter).onAttachPaymentMethodFailed(
                style = CustomerSheetEventReporter.AddPaymentMethodStyle.SetupIntent
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend inline fun <R> ReceiveTurbine<*>.awaitViewState(): R {
        return awaitItem() as R
    }
}
