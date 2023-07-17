package com.stripe.android.customersheet

import app.cash.turbine.test
import app.cash.turbine.testIn
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.customersheet.CustomerSheetTestHelper.createViewModel
import com.stripe.android.customersheet.injection.CustomerSheetViewModelModule
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Stack

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSheetViewModelTest {

    @Test
    fun `isLiveMode is true when publishable key is live`() {
        val viewModelModule = CustomerSheetViewModelModule()
        var isLiveMode = viewModelModule.isLiveMode(
            PaymentConfiguration(
                publishableKey = "pk_test_123"
            )
        )

        assertThat(isLiveMode).isFalse()

        isLiveMode = viewModelModule.isLiveMode(
            PaymentConfiguration(
                publishableKey = "pk_live_123"
            )
        )

        assertThat(isLiveMode).isTrue()

        isLiveMode = viewModelModule.isLiveMode(
            PaymentConfiguration(
                publishableKey = "pk_test_51HvTI7Lu5o3livep6t5AgBSkMvWoTtA0nyA7pVYDqpfLkRtWun7qZTYCOHCReprfLM464yaBeF72UFfB7cY9WG4a00ZnDtiC2C"
            )
        )

        assertThat(isLiveMode).isFalse()
    }

    @Test
    fun `init emits CustomerSheetViewState#SelectPaymentMethod`() = runTest {
        val viewModel = createViewModel()
        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(
                CustomerSheetViewState.SelectPaymentMethod::class.java
            )
        }
    }

    @Test
    fun `CustomerSheetViewAction#OnBackPressed emits canceled result`() = runTest {
        val viewModel = createViewModel()
        viewModel.result.test {
            assertThat(awaitItem()).isEqualTo(null)
            viewModel.handleViewAction(CustomerSheetViewAction.OnBackPressed)
            assertThat(awaitItem()).isEqualTo(InternalCustomerSheetResult.Canceled(null))
        }
    }

    @Test
    fun `When payment methods loaded, CustomerSheetViewState is populated`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!
                    )
                )
            )
        )
        viewModel.viewState.test {
            assertThat(awaitItem())
                .isEqualTo(
                    CustomerSheetViewState.SelectPaymentMethod(
                        title = null,
                        savedPaymentMethods = listOf(
                            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                        ),
                        paymentSelection = PaymentSelection.Saved(
                            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                        ),
                        isLiveMode = false,
                        isProcessing = false,
                        isEditing = false,
                        isGooglePayEnabled = false,
                        primaryButtonLabel = "Continue",
                        primaryButtonEnabled = true,
                        errorMessage = null,
                    )
                )
        }
    }

    @Test
    fun `When payment methods cannot be loaded, errorMessage is emitted`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.failure(
                    cause = APIException(message = "Failed to retrieve payment methods."),
                    displayMessage = "We could\'nt get your payment methods. Please try again."
                )
            )
        )
        viewModel.viewState.test {
            assertThat(
                (awaitItem() as CustomerSheetViewState.SelectPaymentMethod).errorMessage
            ).isEqualTo("We could\'nt get your payment methods. Please try again.")
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
        viewModel.viewState.test {
            val viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(viewState.paymentSelection)
                .isEqualTo(null)
            assertThat(viewState.errorMessage)
                .isEqualTo("Something went wrong")
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
            val viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
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
            val viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
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
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!
                    )
                )
            )
        )
        viewModel.viewState.test {
            val viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
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
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!
                    )
                )
            )
        )
        viewModel.viewState.test {
            val viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(viewState.primaryButtonLabel)
                .isNotNull()
            assertThat(viewState.primaryButtonEnabled)
                .isTrue()
        }
    }

    @Test
    fun `When no selection, primary button label should be null`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(null)
            )
        )
        viewModel.viewState.test {
            val viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(viewState.primaryButtonLabel)
                .isNull()
            assertThat(viewState.primaryButtonEnabled)
                .isFalse()
        }
    }

    @Test
    fun `When CustomerViewAction#OnItemSelected with payment method, primary button label should not be null`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(null)
            )
        )
        viewModel.viewState.test {
            var viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(viewState.primaryButtonLabel)
                .isNull()
            assertThat(viewState.primaryButtonEnabled)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    selection = PaymentSelection.Saved(
                        paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                )
            )

            viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod

            assertThat(viewState.primaryButtonLabel)
                .isNotNull()
            assertThat(viewState.primaryButtonEnabled)
                .isTrue()
        }
    }

    @Test
    fun `When CustomerViewAction#OnItemSelected with Google Pay, primary button label should not be null`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(null)
            )
        )
        viewModel.viewState.test {
            var viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(viewState.primaryButtonLabel)
                .isNull()
            assertThat(viewState.primaryButtonEnabled)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    selection = PaymentSelection.GooglePay
                )
            )

            viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod

            assertThat(viewState.primaryButtonLabel)
                .isNotNull()
            assertThat(viewState.primaryButtonEnabled)
                .isTrue()
        }
    }

    @Test
    fun `When CustomerViewAction#OnItemSelected with Link, primary button label should be null`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(null)
            )
        )
        viewModel.viewState.test {
            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    selection = PaymentSelection.Link
                )
            )

            val viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod

            assertThat(viewState.primaryButtonLabel)
                .isNull()
            assertThat(viewState.primaryButtonEnabled)
                .isFalse()
        }
    }

    @Test
    fun `When CustomerViewAction#OnItemSelected with null, primary button label should be null`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(null)
            )
        )
        viewModel.viewState.test {
            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    selection = null
                )
            )

            val viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod

            assertThat(viewState.primaryButtonLabel)
                .isNull()
            assertThat(viewState.primaryButtonEnabled)
                .isFalse()
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
                .isInstanceOf(CustomerSheetViewState.SelectPaymentMethod::class.java)
            viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)
            assertThat(awaitItem())
                .isInstanceOf(CustomerSheetViewState.AddPaymentMethod::class.java)
        }
    }

    @Test
    fun `When CustomerViewAction#OnEditPressed, view state isEditing should be updated`() = runTest {
        val viewModel = createViewModel()
        viewModel.viewState.test {
            var viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(viewState.isEditing)
                .isFalse()

            viewModel.handleViewAction(CustomerSheetViewAction.OnEditPressed)

            viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
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
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!
                    )
                )
            )
        )
        viewModel.viewState.test {
            var viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(viewState.savedPaymentMethods).hasSize(1)
            assertThat(viewState.paymentSelection).isNotNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemRemoved(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                )
            )

            viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(viewState.savedPaymentMethods).hasSize(0)
            assertThat(viewState.paymentSelection).isNull()
        }
    }

    @Test
    fun `When removing a payment method fails, error message is displayed`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
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
            var viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(viewState.savedPaymentMethods).hasSize(1)
            assertThat(viewState.errorMessage).isNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemRemoved(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                )
            )

            viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
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
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!
                    )
                )
            )
        )
        val viewStateTurbine = viewModel.viewState.testIn(backgroundScope)
        val resultTurbine = viewModel.result.testIn(backgroundScope)

        assertThat(viewStateTurbine.awaitItem()).isInstanceOf(CustomerSheetViewState.SelectPaymentMethod::class.java)
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
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!
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
            var viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(viewState.errorMessage).isNull()
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(viewState.errorMessage).isEqualTo("Something went wrong")
        }
    }

    @Test
    fun `When primary button is pressed for google pay, google pay is emitted`() = runTest {
        val viewModel = createViewModel(
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
            backstack = buildBackstack(
                CustomerSheetViewState.AddPaymentMethod(
                    paymentMethodCode = PaymentMethod.Type.Card.code,
                    formViewData = FormViewModel.ViewData(
                        completeFormValues = FormFieldValues(
                            showsMandate = false,
                            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse,
                        )
                    ),
                    enabled = true,
                    isLiveMode = false,
                    isProcessing = false,
                )
            ),
            stripeRepository = FakeStripeRepository(
                onCreatePaymentMethod = {
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                }
            )
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(CustomerSheetViewState.AddPaymentMethod::class.java)
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            assertThat(awaitItem().isProcessing).isTrue()

            // Payment method was created and attached to the customer
            assertThat(awaitItem().isProcessing).isFalse()
        }
    }

    @Test
    fun `When payment method could not be created, error message is visible`() = runTest {
        val viewModel = createViewModel(
            backstack = buildBackstack(
                CustomerSheetViewState.AddPaymentMethod(
                    paymentMethodCode = PaymentMethod.Type.Card.code,
                    formViewData = FormViewModel.ViewData(
                        completeFormValues = FormFieldValues(
                            showsMandate = false,
                            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse,
                        )
                    ),
                    enabled = true,
                    isLiveMode = false,
                    isProcessing = false,
                )
            ),
            stripeRepository = FakeStripeRepository(
                onCreatePaymentMethod = {
                    throw APIException(
                        stripeError = StripeError(
                            message = "Could not create payment method."
                        )
                    )
                }
            )
        )

        viewModel.viewState.test {
            var viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.errorMessage).isNull()
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.isProcessing).isTrue()
            viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.errorMessage).isEqualTo("Could not create payment method.")
            assertThat(viewState.isProcessing).isFalse()
        }
    }

    @Test
    fun `Payment method is attached to customer with setup intent`() = runTest {
        val initialViewState = CustomerSheetViewState.AddPaymentMethod(
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
        )
        val viewModel = createViewModel(
            backstack = buildBackstack(
                CustomerSheetViewState.SelectPaymentMethod(
                    title = null,
                    savedPaymentMethods = listOf(),
                    paymentSelection = null,
                    isLiveMode = false,
                    isProcessing = false,
                    isEditing = false,
                    isGooglePayEnabled = false,
                    primaryButtonLabel = null,
                    primaryButtonEnabled = false,
                ),
                initialViewState
            ),
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = true,
                onSetupIntentClientSecretForCustomerAttach = {
                    CustomerAdapter.Result.success("seti_123")
                },
            ),
            stripeRepository = FakeStripeRepository(
                onCreatePaymentMethod = {
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                },
                onConfirmSetupIntent = {
                    SetupIntentFixtures.SI_SUCCEEDED
                }
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.errorMessage).isNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.isProcessing).isTrue()

            val newViewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(newViewState.errorMessage).isNull()
            assertThat(newViewState.isProcessing).isFalse()
            assertThat(newViewState.savedPaymentMethods.contains(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        }
    }

    @Test
    fun `Payment method is attached to customer without setup intent`() = runTest {
        val initialViewState = CustomerSheetViewState.AddPaymentMethod(
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
        )
        val viewModel = createViewModel(
            backstack = buildBackstack(
                CustomerSheetViewState.SelectPaymentMethod(
                    title = null,
                    savedPaymentMethods = listOf(),
                    paymentSelection = null,
                    isLiveMode = false,
                    isProcessing = false,
                    isEditing = false,
                    isGooglePayEnabled = false,
                    primaryButtonLabel = null,
                    primaryButtonEnabled = false,
                ),
                initialViewState
            ),
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = false,
                onAttachPaymentMethod = {
                    CustomerAdapter.Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                },
                onSetupIntentClientSecretForCustomerAttach = null,
            ),
            stripeRepository = FakeStripeRepository(
                onCreatePaymentMethod = {
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                },
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.errorMessage).isNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.isProcessing).isTrue()

            val newViewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(newViewState.errorMessage).isNull()
            assertThat(newViewState.isProcessing).isFalse()
            assertThat(newViewState.savedPaymentMethods.contains(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        }
    }

    @Test
    fun `When payment method cannot be attached with setup intent, error message is visible`() = runTest {
        val initialViewState = CustomerSheetViewState.AddPaymentMethod(
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
        )
        val viewModel = createViewModel(
            backstack = buildBackstack(initialViewState),
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = true,
                onSetupIntentClientSecretForCustomerAttach = {
                    CustomerAdapter.Result.success("seti_123")
                },
            ),
            stripeRepository = FakeStripeRepository(
                onCreatePaymentMethod = {
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                },
                onConfirmSetupIntent = {
                    throw APIException(
                        stripeError = StripeError(
                            message = "Could not attach payment method."
                        )
                    )
                },
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.errorMessage).isNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.isProcessing).isTrue()
            viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.errorMessage).isEqualTo("Could not attach payment method.")
            assertThat(viewState.isProcessing).isFalse()
        }
    }

    @Test
    fun `When setup intent provider is not provided, error message is visible`() = runTest {
        val initialViewState = CustomerSheetViewState.AddPaymentMethod(
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
        )
        val viewModel = createViewModel(
            backstack = buildBackstack(initialViewState),
            customerAdapter = FakeCustomerAdapter(
                canCreateSetupIntents = true,
                onSetupIntentClientSecretForCustomerAttach = null,
            ),
            stripeRepository = FakeStripeRepository(
                onCreatePaymentMethod = {
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                },
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.errorMessage).isNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.isProcessing).isTrue()
            viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.errorMessage).isEqualTo("Merchant provided error message")
            assertThat(viewState.isProcessing).isFalse()
        }
    }

    @Test
    fun `When payment method cannot be attached, error message is visible`() = runTest {
        val initialViewState = CustomerSheetViewState.AddPaymentMethod(
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
        )
        val viewModel = createViewModel(
            backstack = buildBackstack(initialViewState),
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
                        displayMessage = "We could not save this payment method. Please try again."
                    )
                },
            ),
            stripeRepository = FakeStripeRepository(
                onCreatePaymentMethod = {
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                },
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.errorMessage).isNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.isProcessing).isTrue()
            viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.errorMessage).isEqualTo("We could not save this payment method. Please try again.")
            assertThat(viewState.isProcessing).isFalse()
        }
    }

    @Test
    fun `When card form is complete, primary button should be enabled`() = runTest {
        val initialViewState = CustomerSheetViewState.AddPaymentMethod(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            formViewData = FormViewModel.ViewData(),
            enabled = true,
            isLiveMode = false,
            isProcessing = false,
        )
        val viewModel = createViewModel(
            backstack = buildBackstack(initialViewState),
        )

        viewModel.viewState.test {
            var viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.primaryButtonEnabled).isFalse()

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

            viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.primaryButtonEnabled).isTrue()
        }
    }

    @Test
    fun `When card form is not complete, primary button should be disabled`() = runTest {
        val initialViewState = CustomerSheetViewState.AddPaymentMethod(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            formViewData = FormViewModel.ViewData(),
            enabled = true,
            isLiveMode = false,
            isProcessing = false,
        )
        val viewModel = createViewModel(
            backstack = buildBackstack(initialViewState),
        )

        viewModel.viewState.test {
            val viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.primaryButtonEnabled).isFalse()
        }
    }

    private fun buildBackstack(vararg states: CustomerSheetViewState): Stack<CustomerSheetViewState> {
        return Stack<CustomerSheetViewState>().apply {
            states.forEach {
                push(it)
            }
        }
    }
}
