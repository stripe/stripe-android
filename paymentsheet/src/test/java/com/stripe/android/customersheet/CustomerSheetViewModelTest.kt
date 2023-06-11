package com.stripe.android.customersheet

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
            PaymentIntentFactory.create(
                paymentMethodTypes = listOf(
                    PaymentMethod.Type.Card.code,
                    PaymentMethod.Type.USBankAccount.code
                )
            ),
            null
        )
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
        viewModel.viewState.test {
            assertThat(
                (awaitItem() as CustomerSheetViewState.SelectPaymentMethod).result
            ).isEqualTo(null)
            viewModel.handleViewAction(CustomerSheetViewAction.OnBackPressed)
            assertThat(
                (awaitItem() as CustomerSheetViewState.SelectPaymentMethod).result
            ).isEqualTo(InternalCustomerSheetResult.Canceled)
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
                        paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
                        showEditMenu = true,
                        isLiveMode = false,
                        isProcessing = false,
                        isEditing = false,
                        isGooglePayEnabled = false,
                        errorMessage = null,
                        primaryButtonLabel = "Continue",
                        primaryButtonEnabled = true,
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
    fun `When the selected payment method cannot be loaded, selectedPaymentMethod is null`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                selectedPaymentOption = Result.failure(Exception("Failed to retrieve selected payment option."))
            )
        )
        viewModel.viewState.test {
            val viewState = awaitItem() as CustomerSheetViewState.SelectPaymentMethod
            assertThat(viewState.paymentSelection)
                .isEqualTo(null)
            assertThat(viewState.errorMessage)
                .isEqualTo(null)
            assertThat(viewState.savedPaymentMethods)
                .isNotEmpty()
        }
    }

    @Test
    fun `When a payment option is selected, the view state is updated`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    )
                ),
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
                        paymentSelection = null,
                        showEditMenu = true,
                        isLiveMode = false,
                        isProcessing = false,
                        isEditing = false,
                        isGooglePayEnabled = false,
                        errorMessage = null,
                        primaryButtonLabel = null,
                        primaryButtonEnabled = false,
                    )
                )

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    selection = PaymentSelection.Saved(
                        paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                )
            )

            assertThat(awaitItem())
                .isEqualTo(
                    CustomerSheetViewState.SelectPaymentMethod(
                        title = null,
                        savedPaymentMethods = listOf(
                            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                        ),
                        paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
                        showEditMenu = true,
                        isLiveMode = false,
                        isProcessing = false,
                        isEditing = false,
                        isGooglePayEnabled = false,
                        errorMessage = null,
                        primaryButtonLabel = "Continue",
                        primaryButtonEnabled = true,
                    )
                )
        }
    }

    @Test
    fun `providePaymentMethodName provides the name for a payment method`() {
        val viewModel = createViewModel()
        val name = viewModel.providePaymentMethodName("card")
        assertThat(name)
            .isEqualTo("Card")
    }

    @Test
    fun `GooglePay is selectable`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                paymentMethods = Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    )
                ),
            ),
            configuration = CustomerSheet.Configuration(
                googlePayEnabled = true
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
                        paymentSelection = null,
                        showEditMenu = true,
                        isLiveMode = false,
                        isProcessing = false,
                        isEditing = false,
                        isGooglePayEnabled = true,
                        errorMessage = null,
                        primaryButtonLabel = null,
                        primaryButtonEnabled = false,
                    )
                )

            viewModel.handleViewAction(
                CustomerSheetViewAction.OnItemSelected(
                    selection = PaymentSelection.GooglePay,
                )
            )

            assertThat(awaitItem())
                .isEqualTo(
                    CustomerSheetViewState.SelectPaymentMethod(
                        title = null,
                        savedPaymentMethods = listOf(
                            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                        ),
                        paymentSelection = PaymentSelection.GooglePay,
                        showEditMenu = true,
                        isLiveMode = false,
                        isProcessing = false,
                        isEditing = false,
                        isGooglePayEnabled = true,
                        errorMessage = null,
                        primaryButtonLabel = "Continue",
                        primaryButtonEnabled = true,
                    )
                )
        }
    }

    private fun createViewModel(
        customerAdapter: CustomerAdapter = FakeCustomerAdapter(),
        lpmRepository: LpmRepository = this.lpmRepository,
        configuration: CustomerSheet.Configuration = CustomerSheet.Configuration(),
    ): CustomerSheetViewModel {
        return CustomerSheetViewModel(
            resources = application.resources,
            customerAdapter = customerAdapter,
            lpmRepository = lpmRepository,
            configuration = configuration
        )
    }
}
