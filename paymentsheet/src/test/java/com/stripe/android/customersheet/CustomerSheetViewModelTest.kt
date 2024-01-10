package com.stripe.android.customersheet

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.cash.turbine.testIn
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.CustomerSheetViewState.AddPaymentMethod
import com.stripe.android.customersheet.CustomerSheetViewState.SelectPaymentMethod
import com.stripe.android.customersheet.analytics.CustomerSheetEventReporter
import com.stripe.android.customersheet.injection.CustomerSheetViewModelModule
import com.stripe.android.customersheet.utils.CustomerSheetTestHelper.addPaymentMethodViewState
import com.stripe.android.customersheet.utils.CustomerSheetTestHelper.createModifiableEditPaymentMethodViewInteractorFactory
import com.stripe.android.customersheet.utils.CustomerSheetTestHelper.createViewModel
import com.stripe.android.customersheet.utils.CustomerSheetTestHelper.selectPaymentMethodViewState
import com.stripe.android.customersheet.utils.FakeCustomerSheetLoader
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.PaymentAccount
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.model.PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
import com.stripe.android.model.PaymentMethodFixtures.US_BANK_ACCOUNT
import com.stripe.android.model.PaymentMethodFixtures.US_BANK_ACCOUNT_VERIFIED
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponseInternal
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormScreenState
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewAction
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewAction.OnBrandChoiceChanged
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewAction.OnRemoveConfirmed
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewAction.OnRemovePressed
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewAction.OnUpdatePressed
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import com.stripe.android.utils.FeatureFlags
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
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith
import com.stripe.android.ui.core.R as UiCoreR

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSheetViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher(TestCoroutineScheduler())

    @get:Rule
    val featureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.customerSheetACHv2,
        isEnabled = false,
    )

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
            assertThat(awaitItem()).isInstanceOf(
                AddPaymentMethod::class.java
            )
        }
    }

    @Test
    fun `init emits CustomerSheetViewState#SelectPaymentMethod when only google pay available`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isGooglePayAvailable = true
        )
        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(
                SelectPaymentMethod::class.java
            )
        }
    }

    @Test
    fun `init emits CustomerSheetViewState#SelectPaymentMethod when payment methods available`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher
        )
        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(
                SelectPaymentMethod::class.java
            )
        }
    }

    @Test
    fun `CustomerSheetViewAction#OnBackPressed emits canceled result`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
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
                .isInstanceOf(PaymentSelection.Saved::class.java)
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
        assertThat(name)
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
                .isEqualTo("Confirm")
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
    fun `When CustomerViewAction#OnItemSelected with null, primary button label should be null`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher
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
    fun `When CustomerViewAction#OnAddCardPressed, view state is updated to CustomerViewAction#AddPaymentMethod`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher
        )

        viewModel.viewState.test {
            assertThat(awaitItem())
                .isInstanceOf(SelectPaymentMethod::class.java)
            viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)
            assertThat(awaitItem())
                .isInstanceOf(AddPaymentMethod::class.java)
        }
    }

    @Test
    fun `When CustomerViewAction#OnEditPressed, view state isEditing should be updated`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher
        )
        viewModel.viewState.test {
            var viewState = awaitViewState<SelectPaymentMethod>()
            assertThat(viewState.isEditing)
                .isFalse()

            viewModel.handleViewAction(CustomerSheetViewAction.OnEditPressed)

            viewState = awaitViewState()
            assertThat(viewState.isEditing)
                .isTrue()

            viewModel.handleViewAction(CustomerSheetViewAction.OnItemRemoved(CARD_PAYMENT_METHOD))

            viewState = awaitViewState()
            assertThat(viewState.isEditing)
                .isTrue()
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

            val addPaymentMethodViewState = awaitViewState<AddPaymentMethod>()

            assertThat(addPaymentMethodViewState.isFirstPaymentMethod).isTrue()
        }
    }

    @Test
    fun `When removing a payment method fails, error message is displayed`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerAdapter = FakeCustomerAdapter(
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
    fun `When primary button is pressed for saved payment method, selected payment method is emitted`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(CARD_PAYMENT_METHOD),
            savedPaymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD),
        )
        val viewStateTurbine = viewModel.viewState.testIn(backgroundScope)
        val resultTurbine = viewModel.result.testIn(backgroundScope)

        assertThat(viewStateTurbine.awaitItem()).isInstanceOf(SelectPaymentMethod::class.java)
        assertThat(resultTurbine.awaitItem()).isNull()

        viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

        assertThat(resultTurbine.awaitItem()).isInstanceOf(InternalCustomerSheetResult.Selected::class.java)
    }

    @Test
    fun `When primary button is pressed for saved payment method that cannot be saved, error message is emitted`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            customerPaymentMethods = listOf(CARD_PAYMENT_METHOD),
            savedPaymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD),
            customerAdapter = FakeCustomerAdapter(
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
    fun `When primary button is pressed for google pay, google pay is emitted`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When primary button is pressed in the add payment flow, view should be loading`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When payment method could not be created, error message is visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `Payment method is attached to customer with setup intent`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `Payment method is attached to customer without setup intent`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When payment method cannot be attached with setup intent, error message is visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When setup intent provider is not provided, error message is visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When payment method cannot be attached, error message is visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When card form is complete, primary button should be enabled`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            initialBackStack = listOf(
                addPaymentMethodViewState.copy(
                    formViewData = FormViewModel.ViewData(),
                )
            ),
        )

        viewModel.viewState.test {
            assertThat(awaitViewState<AddPaymentMethod>().primaryButtonEnabled).isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnFormFieldValuesCompleted(
                    formFieldValues = FormFieldValues(
                        fieldValuePairs = mapOf(
                            IdentifierSpec.Generic("test") to FormFieldEntry("test", true)
                        ),
                        showsMandate = false,
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
    fun `When editing, primary button is not visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When a new payment method is added, the primary button is visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When removing the originally selected payment selection, primary button is not visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When removing the newly added payment, original payment selection is selected and primary button is not visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `Moving from screen to screen preserves state`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When there is an initially selected PM, selecting another PM and cancelling should keep the original`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `If Google Pay is not available and config enables Google Pay, then Google Pay should not be enabled`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            initialBackStack = listOf(
                CustomerSheetViewState.Loading(false),
                selectPaymentMethodViewState,
            ),
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
            initialBackStack = listOf(
                selectPaymentMethodViewState,
                CustomerSheetViewState.Loading(false),
            ),
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
    fun `When edit payment screen is presented, event is reported`() {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
            eventReporter = eventReporter,
        )

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnModifyItem(paymentMethod = CARD_PAYMENT_METHOD)
        )

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
    fun `When remove payment method failed, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When google pay is confirmed, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When google pay selection errors, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When payment selection is confirmed, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When payment selection errors, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When attach without setup intent succeeds, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When attach without setup intent fails, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When attach with setup intent succeeds, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When attach with setup intent fails, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
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
    fun `When attach with setup intent handle next action fails, event is reported`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()

        val viewModel = createViewModel(
            workContext = testDispatcher,
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

    @Test
    fun `Payment method form changes on user selection`() = runTest(testDispatcher) {
        featureFlagTestRule.setEnabled(true)

        val viewModel = createViewModel(
            workContext = testDispatcher,
            initialBackStack = listOf(
                addPaymentMethodViewState,
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.selectedPaymentMethod.code)
                .isEqualTo("card")

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepository.hardCodedUsBankAccount
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.selectedPaymentMethod.code)
                .isEqualTo("us_bank_account")
        }
    }

    @Test
    fun `Payment method user selection saved after returning to add screen`() = runTest(testDispatcher) {
        featureFlagTestRule.setEnabled(true)

        val viewModel = createViewModel(
            workContext = testDispatcher,
            initialBackStack = listOf(
                selectPaymentMethodViewState,
                addPaymentMethodViewState,
            ),
        )

        viewModel.viewState.test {
            assertThat(
                awaitViewState<AddPaymentMethod>().selectedPaymentMethod.code
            ).isEqualTo("card")

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepository.hardCodedUsBankAccount
                )
            )

            assertThat(
                awaitViewState<AddPaymentMethod>().selectedPaymentMethod.code
            ).isEqualTo("us_bank_account")

            viewModel.handleViewAction(CustomerSheetViewAction.OnBackPressed)

            assertThat(
                awaitViewState<SelectPaymentMethod>()
            ).isInstanceOf(SelectPaymentMethod::class.java)

            viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)

            assertThat(
                awaitViewState<AddPaymentMethod>().selectedPaymentMethod.code
            ).isEqualTo("us_bank_account")
        }
    }

    @Test
    fun `When the payment method form is us bank account, the primary button label is continue`() = runTest(testDispatcher) {
        featureFlagTestRule.setEnabled(true)

        val viewModel = createViewModel(
            workContext = testDispatcher,
            initialBackStack = listOf(
                addPaymentMethodViewState,
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(resolvableString(R.string.stripe_paymentsheet_save))

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepository.hardCodedUsBankAccount
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(resolvableString(UiCoreR.string.stripe_continue_button_label))
        }
    }

    @Test
    fun `The custom primary button can be updated`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            initialBackStack = listOf(
                addPaymentMethodViewState,
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.customPrimaryButtonUiState)
                .isNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnUpdateCustomButtonUIState(
                    callback = {
                        PrimaryButton.UIState(
                            label = "Continue",
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
            initialBackStack = listOf(
                addPaymentMethodViewState,
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.mandateText)
                .isNull()
            assertThat(viewState.showMandateAbovePrimaryButton)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnUpdateMandateText(
                    mandateText = "This is a mandate.",
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
        val usBankAccount = PaymentSelection.New.USBankAccount(
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
            screenState = USBankAccountFormScreenState.SavedAccount(
                financialConnectionsSessionId = "session_1234",
                intentId = "intent_1234",
                bankName = "Stripe Bank",
                last4 = "6789",
                primaryButtonText = "Continue",
                mandateText = null,
            ),
        )
        val viewModel = createViewModel(
            workContext = testDispatcher,
            initialBackStack = listOf(
                selectPaymentMethodViewState,
                addPaymentMethodViewState,
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(US_BANK_ACCOUNT_VERIFIED),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
            customerAdapter = FakeCustomerAdapter(
                onSetupIntentClientSecretForCustomerAttach = {
                    CustomerAdapter.Result.success("seti_123")
                },
                canCreateSetupIntents = true,
            ),
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(AddPaymentMethod::class.java)

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnConfirmUSBankAccount(usBankAccount)
            )

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnPrimaryButtonPressed
            )

            val viewState = awaitViewState<SelectPaymentMethod>()

            assertThat(viewState.paymentSelection)
                .isEqualTo(
                    PaymentSelection.Saved(US_BANK_ACCOUNT_VERIFIED)
                )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When a form error is emitted, screen state is updated`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            initialBackStack = listOf(
                addPaymentMethodViewState,
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.errorMessage)
                .isNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnFormError(
                    error = "This is an error."
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.errorMessage)
                .isEqualTo("This is an error.")
        }
    }

    @Test
    fun `When adding a US Bank account and user taps on scrim, a confirmation dialog should be visible`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isFinancialConnectionsAvailable = { true },
            initialBackStack = listOf(
                addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                ),
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.displayDismissConfirmationModal)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnCollectBankAccountResult(
                    bankAccountResult = mockUSBankAccountResult(
                        isVerified = true
                    ),
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
            isFinancialConnectionsAvailable = { true },
            initialBackStack = listOf(
                addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.Card.code,
                ),
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.displayDismissConfirmationModal)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnCollectBankAccountResult(
                    bankAccountResult = CollectBankAccountResultInternal.Completed(
                        response = mock(),
                    ),
                )
            )

            viewState = awaitViewState()
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
            isFinancialConnectionsAvailable = { true },
            initialBackStack = listOf(
                addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                ),
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.displayDismissConfirmationModal)
                .isFalse()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnCollectBankAccountResult(
                    bankAccountResult = mockUSBankAccountResult(
                        isVerified = true
                    ),
                )
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
            isFinancialConnectionsAvailable = { true },
            initialBackStack = listOf(
                addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                ),
            ),
        )

        val viewStateTurbine = viewModel.viewState.testIn(backgroundScope)
        val resultTurbine = viewModel.result.testIn(backgroundScope)

        assertThat(resultTurbine.awaitItem()).isNull()

        var viewState = viewStateTurbine.awaitViewState<AddPaymentMethod>()
        assertThat(viewState.displayDismissConfirmationModal)
            .isFalse()

        viewModel.handleViewAction(
            CustomerSheetViewAction.OnCollectBankAccountResult(
                bankAccountResult = mockUSBankAccountResult(
                    isVerified = true
                ),
            )
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

    @Test
    fun `When in add flow and us bank account is retrieved, then shouldDisplayConfirmationDialog should be true`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isFinancialConnectionsAvailable = { true },
            initialBackStack = listOf(
                addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                    bankAccountResult = mockUSBankAccountResult(
                        isVerified = true
                    ),
                ),
            ),
        )

        viewModel.viewState.test {
            val viewState = awaitViewState<AddPaymentMethod>()
            assertThat(
                viewState.shouldDisplayDismissConfirmationModal(
                    isFinancialConnectionsAvailable = { true }
                )
            ).isTrue()
        }
    }

    @Test
    fun `When in add flow and unverified us bank account is retrieved, then shouldDisplayConfirmationDialog should be false`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isFinancialConnectionsAvailable = { true },
            initialBackStack = listOf(
                addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                    bankAccountResult = mockUSBankAccountResult(
                        isVerified = false
                    ),
                ),
            ),
        )

        viewModel.viewState.test {
            val viewState = awaitViewState<AddPaymentMethod>()
            assertThat(
                viewState.shouldDisplayDismissConfirmationModal(
                    isFinancialConnectionsAvailable = { true }
                )
            ).isFalse()
        }
    }

    @Test
    fun `When financial connections is not available, then shouldDisplayConfirmationDialog should be false`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isFinancialConnectionsAvailable = { true },
            initialBackStack = listOf(
                addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                    bankAccountResult = mockUSBankAccountResult(
                        isVerified = false
                    ),
                ),
            ),
        )

        viewModel.viewState.test {
            val viewState = awaitViewState<AddPaymentMethod>()
            assertThat(
                viewState.shouldDisplayDismissConfirmationModal(
                    isFinancialConnectionsAvailable = { false }
                )
            ).isFalse()
        }
    }

    @Test
    fun `Selecting the already selected payment method in add flow does nothing`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isFinancialConnectionsAvailable = { true },
            initialBackStack = listOf(
                addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                    selectedPaymentMethod = LpmRepository.hardCodedUsBankAccount,
                ),
            ),
        )

        viewModel.viewState.test {
            val viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.selectedPaymentMethod)
                .isEqualTo(LpmRepository.hardCodedUsBankAccount)

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepository.hardCodedUsBankAccount
                )
            )

            expectNoEvents()
        }
    }

    @Test
    fun `When adding a us bank account and the account is retrieved, the primary button should say save`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isFinancialConnectionsAvailable = { true },
            initialBackStack = listOf(
                addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                    selectedPaymentMethod = LpmRepository.hardCodedUsBankAccount,
                    bankAccountResult = CollectBankAccountResultInternal.Completed(
                        response = mock(),
                    ),
                ),
            ),
        )

        viewModel.viewState.test {
            val viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(resolvableString(R.string.stripe_paymentsheet_save))
        }
    }

    @Test
    fun `When adding a us bank account and the account is cancelled, the primary button should say continue`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isFinancialConnectionsAvailable = { true },
            initialBackStack = listOf(
                addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                    selectedPaymentMethod = LpmRepository.hardCodedUsBankAccount,
                    bankAccountResult = null,
                ),
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(resolvableString(R.string.stripe_paymentsheet_save))

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnCollectBankAccountResult(
                    CollectBankAccountResultInternal.Cancelled
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(resolvableString(UiCoreR.string.stripe_continue_button_label))
        }
    }

    @Test
    fun `When adding us bank and primary button says save, it should stay as save`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isFinancialConnectionsAvailable = { true },
            initialBackStack = listOf(
                addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                    selectedPaymentMethod = LpmRepository.hardCodedUsBankAccount,
                    bankAccountResult = CollectBankAccountResultInternal.Completed(
                        response = mock(),
                    ),
                ),
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(resolvableString(R.string.stripe_paymentsheet_save))

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepository.HardcodedCard
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(resolvableString(R.string.stripe_paymentsheet_save))

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepository.hardCodedUsBankAccount
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.primaryButtonLabel)
                .isEqualTo(resolvableString(R.string.stripe_paymentsheet_save))
        }
    }

    @Test
    fun `Mandate is required depending on payment method`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
            isFinancialConnectionsAvailable = { true },
            initialBackStack = listOf(
                addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.Card.code,
                    selectedPaymentMethod = LpmRepository.HardcodedCard,
                ),
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitViewState<AddPaymentMethod>()
            assertThat(viewState.mandateText)
                .isNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepository.hardCodedUsBankAccount
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.mandateText)
                .isNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnUpdateMandateText(
                    mandateText = "Mandate",
                    showAbovePrimaryButton = false
                )
            )

            viewState = awaitViewState()
            assertThat(viewState.mandateText)
                .isNotNull()

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnAddPaymentMethodItemChanged(
                    LpmRepository.HardcodedCard
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
            isFinancialConnectionsAvailable = { true },
            initialBackStack = listOf(
                selectPaymentMethodViewState.copy(
                    savedPaymentMethods = listOf(CARD_PAYMENT_METHOD, US_BANK_ACCOUNT),
                ),
            ),
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
            isFinancialConnectionsAvailable = { true },
            savedPaymentSelection = PaymentSelection.Saved(
                paymentMethod = US_BANK_ACCOUNT
            ),
            initialBackStack = listOf(
                selectPaymentMethodViewState.copy(
                    savedPaymentMethods = listOf(CARD_PAYMENT_METHOD, US_BANK_ACCOUNT),
                ),
            ),
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
                .isNull()
        }
    }

    @Test
    fun `When confirming a card, the card form should be reset when trying to add another card`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            workContext = testDispatcher,
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

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnFormFieldValuesCompleted(
                    formFieldValues = FormFieldValues(
                        fieldValuePairs = mapOf(
                            IdentifierSpec.Generic("test") to FormFieldEntry("test", true)
                        ),
                        showsMandate = false,
                        userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
                    )
                )
            )
            assertThat(awaitViewState<AddPaymentMethod>().formViewData.completeFormValues).isNotNull()

            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            assertThat(awaitItem()).isInstanceOf(AddPaymentMethod::class.java)
            assertThat(awaitItem()).isInstanceOf(SelectPaymentMethod::class.java)

            viewModel.handleViewAction(CustomerSheetViewAction.OnAddCardPressed)

            val newViewState = awaitViewState<AddPaymentMethod>()
            assertThat(newViewState.formViewData.completeFormValues).isNull()
        }
    }

    @Test
    fun `When attaching a non-verified bank account, the sheet closes and returns the account`() = runTest(testDispatcher) {
        val usBankAccount = PaymentSelection.New.USBankAccount(
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
            screenState = USBankAccountFormScreenState.SavedAccount(
                financialConnectionsSessionId = "session_1234",
                intentId = "intent_1234",
                bankName = "Stripe Bank",
                last4 = "6789",
                primaryButtonText = "Continue",
                mandateText = null,
            ),
        )
        val viewModel = createViewModel(
            workContext = testDispatcher,
            initialBackStack = listOf(
                addPaymentMethodViewState,
            ),
            stripeRepository = FakeStripeRepository(
                createPaymentMethodResult = Result.success(US_BANK_ACCOUNT),
                retrieveSetupIntent = Result.success(SetupIntentFixtures.SI_SUCCEEDED),
            ),
            customerAdapter = FakeCustomerAdapter(
                onSetupIntentClientSecretForCustomerAttach = {
                    CustomerAdapter.Result.success("seti_123")
                },
                canCreateSetupIntents = true,
            ),
        )

        viewModel.result.test {
            assertThat(awaitItem())
                .isNull()
            viewModel.handleViewAction(
                CustomerSheetViewAction.OnConfirmUSBankAccount(usBankAccount)
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
            initialBackStack = listOf(
                selectPaymentMethodViewState.copy(
                    savedPaymentMethods = paymentMethods,
                )
            ),
            customerPaymentMethods = paymentMethods,
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(SelectPaymentMethod::class.java)
            viewModel.handleViewAction(CustomerSheetViewAction.OnModifyItem(paymentMethods.single()))

            val editViewState = awaitViewState<CustomerSheetViewState.EditPaymentMethod>()
            editViewState.editPaymentMethodInteractor.handleViewAction(OnRemovePressed)

            expectNoEvents()
            editViewState.editPaymentMethodInteractor.handleViewAction(OnRemoveConfirmed)

            // Confirm that nothing has changed yet. We're waiting to remove the payment method
            // once we return to the SPM screen.
            val updatedViewState = awaitViewState<SelectPaymentMethod>()
            assertThat(updatedViewState.savedPaymentMethods).containsExactlyElementsIn(paymentMethods)

            // Simulate the delay
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(awaitItem()).isInstanceOf(AddPaymentMethod::class.java)
        }
    }

    @Test
    fun `Removing payment method in edit screen goes through expected states when removing one of multiple payment methods`() = runTest(testDispatcher) {
        val paymentMethods = PaymentMethodFactory.cards(size = 2)

        val viewModel = createViewModel(
            workContext = testDispatcher,
            initialBackStack = listOf(
                selectPaymentMethodViewState.copy(
                    savedPaymentMethods = paymentMethods,
                )
            ),
            customerPaymentMethods = paymentMethods,
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(SelectPaymentMethod::class.java)
            viewModel.handleViewAction(CustomerSheetViewAction.OnModifyItem(paymentMethods.first()))

            val editViewState = awaitViewState<CustomerSheetViewState.EditPaymentMethod>()
            editViewState.editPaymentMethodInteractor.handleViewAction(OnRemovePressed)

            expectNoEvents()
            editViewState.editPaymentMethodInteractor.handleViewAction(OnRemoveConfirmed)

            // Confirm that nothing has changed yet. We're waiting to remove the payment method
            // once we return to the SPM screen.
            val updatedViewState = awaitViewState<SelectPaymentMethod>()
            assertThat(updatedViewState.savedPaymentMethods).containsExactlyElementsIn(paymentMethods)

            // Simulate the delay
            testDispatcher.scheduler.advanceUntilIdle()

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

            val customerAdapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.Success(paymentMethods),
                onUpdatePaymentMethod = { _, _ ->
                    CustomerAdapter.Result.Success(updatedMethod)
                }
            )

            val viewModel = createViewModel(
                workContext = testDispatcher,
                initialBackStack = listOf(
                    selectPaymentMethodViewState.copy(
                        savedPaymentMethods = paymentMethods,
                    )
                ),
                eventReporter = eventReporter,
                customerPaymentMethods = paymentMethods,
                customerAdapter = customerAdapter,
                editInteractorFactory = createModifiableEditPaymentMethodViewInteractorFactory(
                    workContext = testDispatcher
                ),
            )

            viewModel.viewState.test {
                assertThat(awaitItem()).isInstanceOf(SelectPaymentMethod::class.java)
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

                // Simulate the delay
                testDispatcher.scheduler.advanceUntilIdle()

                verify(eventReporter).onUpdatePaymentMethodSucceeded(CardBrand.Visa)

                val finalViewState = awaitViewState<SelectPaymentMethod>()
                assertThat(finalViewState.savedPaymentMethods).containsExactlyElementsIn(listOf(updatedMethod))
            }
        }

    @Test
    fun `Failed update payment method in edit screen reports event`() = runTest(testDispatcher) {
        val eventReporter: CustomerSheetEventReporter = mock()
        val paymentMethods = PaymentMethodFactory.cards(size = 1)

        val firstMethod = paymentMethods.single()

        val customerAdapter = FakeCustomerAdapter(
            paymentMethods = CustomerAdapter.Result.Success(paymentMethods),
            onUpdatePaymentMethod = { _, _ ->
                CustomerAdapter.Result.failure(
                    Exception("No network found!"),
                    "No network found!"
                )
            }
        )

        val viewModel = createViewModel(
            workContext = testDispatcher,
            initialBackStack = listOf(
                selectPaymentMethodViewState.copy(
                    savedPaymentMethods = paymentMethods,
                )
            ),
            eventReporter = eventReporter,
            customerPaymentMethods = paymentMethods,
            customerAdapter = customerAdapter,
            editInteractorFactory = createModifiableEditPaymentMethodViewInteractorFactory(
                workContext = testDispatcher
            ),
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(SelectPaymentMethod::class.java)
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

            // Simulate the delay
            testDispatcher.scheduler.advanceUntilIdle()

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
            initialBackStack = listOf(
                selectPaymentMethodViewState.copy(
                    savedPaymentMethods = paymentMethods,
                )
            ),
            customerPaymentMethods = paymentMethods
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(SelectPaymentMethod::class.java)
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
            initialBackStack = listOf(
                selectPaymentMethodViewState.copy(
                    savedPaymentMethods = paymentMethods,
                )
            ),
            customerPaymentMethods = paymentMethods,
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(SelectPaymentMethod::class.java)
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
            initialBackStack = listOf(
                selectPaymentMethodViewState.copy(
                    savedPaymentMethods = paymentMethods,
                )
            ),
            customerPaymentMethods = paymentMethods,
        )

        viewModel.viewState.test {
            assertThat(awaitItem()).isInstanceOf(SelectPaymentMethod::class.java)
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
            isFinancialConnectionsAvailable = { true },
            savedPaymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD),
            initialBackStack = listOf(
                selectPaymentMethodViewState.copy(
                    savedPaymentMethods = listOf(CARD_PAYMENT_METHOD, US_BANK_ACCOUNT),
                    paymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD),
                ),
            ),
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

    private fun mockUSBankAccountResult(
        isVerified: Boolean
    ): CollectBankAccountResultInternal.Completed {
        val paymentAccount = mock<PaymentAccount>()
        val financialConnectionsAccount = mock<FinancialConnectionsAccount>()
        val financialConnectionsSession = mock<FinancialConnectionsSession>()
        val bankAccountResponse = mock<CollectBankAccountResponseInternal>()
        whenever(bankAccountResponse.financialConnectionsSession).thenReturn(financialConnectionsSession)
        whenever(financialConnectionsSession.paymentAccount).thenReturn(
            if (isVerified) {
                financialConnectionsAccount
            } else {
                paymentAccount
            }
        )
        return CollectBankAccountResultInternal.Completed(
            response = bankAccountResponse,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private suspend inline fun <R> ReceiveTurbine<*>.awaitViewState(): R {
        return awaitItem() as R
    }
}
