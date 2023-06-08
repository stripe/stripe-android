package com.stripe.android.customersheet

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentOptionsItem
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
    )

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
                        paymentMethods = listOf(
                            PaymentOptionsItem.SavedPaymentMethod(
                                displayName = "····4242",
                                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
                            )
                        ),
                        selectedPaymentMethodId = PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!,
                        isLiveMode = false,
                        isProcessing = false,
                        isEditing = false,
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
    fun `When the selected payment method cannot be loaded, errorMessage is emitted`() = runTest {
        val viewModel = createViewModel(
            customerAdapter = FakeCustomerAdapter(
                selectedPaymentOption = Result.failure(Exception("Failed to retrieve selected payment option."))
            )
        )
        viewModel.viewState.test {
            assertThat(
                (awaitItem() as CustomerSheetViewState.SelectPaymentMethod).errorMessage
            ).isEqualTo("Failed to retrieve selected payment option.")
        }
    }

    private fun createViewModel(
        customerAdapter: CustomerAdapter = FakeCustomerAdapter(),
        lpmRepository: LpmRepository = this.lpmRepository
    ): CustomerSheetViewModel {
        return CustomerSheetViewModel(
            resources = application.resources,
            customerAdapter = customerAdapter,
            lpmRepository = lpmRepository,
            configuration = CustomerSheet.Configuration()
        )
    }
}
