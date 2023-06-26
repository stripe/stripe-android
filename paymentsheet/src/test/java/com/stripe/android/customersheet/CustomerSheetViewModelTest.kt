package com.stripe.android.customersheet

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import app.cash.turbine.testIn
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.customersheet.injection.CustomerSheetViewModelModule
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.address.AddressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSheetViewModelTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val lpmRepository = LpmRepository(
        LpmRepository.LpmRepositoryArguments(
            resources = application.resources,
            isFinancialConnectionsAvailable = { true },
            enableACHV2InDeferredFlow = true
        )
    ).apply {
        update(
            PaymentIntentFactory.create(paymentMethodTypes = this.supportedPaymentMethodTypes),
            null
        )
    }

    @Test
    fun `The initial view model view state should be loading with live mode based on payment configuration`() {
        val viewModelModule = CustomerSheetViewModelModule()
        var initialViewState = viewModelModule.initialViewState(
            PaymentConfiguration(
                publishableKey = "pk_test_123"
            )
        ) as CustomerSheetViewState.Loading

        assertThat(initialViewState.isLiveMode).isFalse()

        initialViewState = viewModelModule.initialViewState(
            PaymentConfiguration(
                publishableKey = "pk_live_123"
            )
        ) as CustomerSheetViewState.Loading

        assertThat(initialViewState.isLiveMode).isTrue()
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
            assertThat(awaitItem()).isEqualTo(InternalCustomerSheetResult.Canceled)
        }
    }

    @Test
    fun `When payment methods loaded, CustomerSheetViewState is populated`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = Result.success(
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
                paymentMethods = Result.failure(Exception("Failed to retrieve payment methods."))
            )
        )
        viewModel.viewState.test {
            assertThat(
                (awaitItem() as CustomerSheetViewState.SelectPaymentMethod).errorMessage
            ).isEqualTo("Failed to retrieve payment methods.")
        }
    }

    @Test
    fun `When the selected payment method cannot be loaded, paymentSelection is null`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                selectedPaymentOption = Result.failure(Exception("Failed to retrieve selected payment option."))
            )
        )
        viewModel.viewState.test {
            val viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(viewState.paymentSelection)
                .isEqualTo(null)
            assertThat(viewState.errorMessage)
                .isEqualTo(null)
        }
    }

    @Test
    fun `When the Google Pay is selected payment method, paymentSelection is GooglePay`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                selectedPaymentOption = Result.success(CustomerAdapter.PaymentOption.GooglePay)
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
                selectedPaymentOption = Result.success(CustomerAdapter.PaymentOption.Link)
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
                paymentMethods = Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = Result.success(
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
                paymentMethods = Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = Result.success(
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
                paymentMethods = Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = Result.success(null)
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
                paymentMethods = Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = Result.success(null)
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
                paymentMethods = Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = Result.success(null)
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
                paymentMethods = Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = Result.success(null)
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
                paymentMethods = Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = Result.success(null)
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
    fun `When the payment configuration is live, isLiveMode should be true`() = runTest {
        val viewModel = createViewModel(
            paymentConfiguration = PaymentConfiguration(
                publishableKey = "pk_live_123",
                stripeAccountId = null,
            )
        )

        viewModel.viewState.test {
            assertThat(awaitItem().isLiveMode)
                .isTrue()
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
                paymentMethods = Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = Result.success(
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
                paymentMethods = Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                onDetachPaymentMethod = {
                    Result.failure(Exception("Cannot remove this payment method"))
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
                .isEqualTo("Unable to remove payment method")
        }
    }

    @Test
    fun `When primary button is pressed for saved payment method, selected payment method is emitted`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = Result.success(
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
                paymentMethods = Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = Result.success(
                    CustomerAdapter.PaymentOption.fromId(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!
                    )
                ),
                onSetSelectedPaymentOption = {
                    Result.failure(Exception("Unable to set payment option"))
                }
            )
        )
        viewModel.viewState.test {
            var viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(viewState.errorMessage).isNull()
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(viewState.errorMessage).isEqualTo("Unable to save the selected payment option")
        }
    }

    @Test
    fun `When primary button is pressed for google pay, google pay is emitted`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                selectedPaymentOption = Result.success(
                    CustomerAdapter.PaymentOption.GooglePay
                )
            )
        )
        viewModel.result.test {
            assertThat(awaitItem()).isNull()
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)

            val result = awaitItem() as InternalCustomerSheetResult.Selected
            assertThat(result.label).isEqualTo("Google Pay")
        }
    }

    @Test
    fun `When primary button is pressed in the add payment flow, view should be loading`() = runTest {
        val viewModel = createViewModel(
            injectedViewState = CustomerSheetViewState.AddPaymentMethod(
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
            injectedViewState = CustomerSheetViewState.AddPaymentMethod(
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
            ),
        )

        viewModel.viewState.test {
            var viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.errorMessage).isNull()
            viewModel.handleViewAction(CustomerSheetViewAction.OnPrimaryButtonPressed)
            viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.isProcessing).isTrue()
            viewState = awaitItem() as CustomerSheetViewState.AddPaymentMethod
            assertThat(viewState.errorMessage).isEqualTo("Could not create payment method")
            assertThat(viewState.isProcessing).isFalse()
        }
    }

    private fun createViewModel(
        injectedViewState: CustomerSheetViewState? = null,
        customerAdapter: CustomerAdapter = FakeCustomerAdapter(),
        stripeRepository: StripeRepository = FakeStripeRepository(),
        lpmRepository: LpmRepository = this.lpmRepository,
        paymentConfiguration: PaymentConfiguration = PaymentConfiguration(
            publishableKey = "pk_test_123",
            stripeAccountId = null,
        )
    ): CustomerSheetViewModel {
        val formViewModel = FormViewModel(
            context = application,
            formArguments = FormArguments(
                PaymentMethod.Type.Card.code,
                showCheckbox = false,
                showCheckboxControlledFields = false,
                merchantName = "",
                initialPaymentMethodCreateParams = null
            ),
            lpmRepository = lpmRepository,
            addressRepository = AddressRepository(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
                workContext = Dispatchers.Unconfined,
            ),
            showCheckboxFlow = mock()
        )
        val mockFormBuilder = mock<FormViewModelSubcomponent.Builder>()
        val mockFormSubcomponent = mock<FormViewModelSubcomponent>()
        val mockFormSubComponentBuilderProvider =
            mock<Provider<FormViewModelSubcomponent.Builder>>()
        whenever(mockFormBuilder.build()).thenReturn(mockFormSubcomponent)
        whenever(mockFormBuilder.formArguments(any())).thenReturn(mockFormBuilder)
        whenever(mockFormBuilder.showCheckboxFlow(any())).thenReturn(mockFormBuilder)
        whenever(mockFormSubcomponent.viewModel).thenReturn(formViewModel)
        whenever(mockFormSubComponentBuilderProvider.get()).thenReturn(mockFormBuilder)

        return CustomerSheetViewModel(
            initialViewState = injectedViewState ?: CustomerSheetViewState.Loading(false),
            paymentConfiguration = paymentConfiguration,
            formViewModelSubcomponentBuilderProvider = mockFormSubComponentBuilderProvider,
            resources = application.resources,
            stripeRepository = stripeRepository,
            customerAdapter = customerAdapter,
            lpmRepository = lpmRepository,
            configuration = CustomerSheet.Configuration(),
        )
    }

    class FakeStripeRepository(
        private val onCreatePaymentMethod:
            ((paymentMethodCreateParams: PaymentMethodCreateParams) -> PaymentMethod)? = null
    ) : AbsFakeStripeRepository() {
        override suspend fun createPaymentMethod(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            options: ApiRequest.Options
        ): PaymentMethod? {
            return onCreatePaymentMethod?.invoke(paymentMethodCreateParams)
        }
    }
}
