package com.stripe.android.customersheet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.customersheet.CustomerAdapter.PaymentOption.Companion.toPaymentSelection
import com.stripe.android.customersheet.injection.CustomerSessionComponent
import com.stripe.android.customersheet.injection.DaggerCustomerSessionComponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Class that represents a customers session for the [CustomerSheet]. The customer session is based
 * on the dependencies supplied in [createCustomerSessionComponent]. If the merchant supplies any
 * new dependencies, then the customer session component is recreated. The lifecycle of the
 * customer session lives longer than the [CustomerSheetActivity].
 */
@OptIn(ExperimentalCustomerSheetApi::class)
internal class CustomerSessionViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _paymentSelection = MutableStateFlow<PaymentOptionSelection?>(null)
    val paymentSelection: StateFlow<PaymentOptionSelection?> = _paymentSelection

    internal fun createCustomerSessionComponent(
        configuration: CustomerSheet.Configuration,
        customerAdapter: CustomerAdapter,
    ): CustomerSessionComponent {
        val shouldCreateNewComponent = configuration != backingComponent?.configuration &&
            customerAdapter != backingComponent?.customerAdapter
        if (shouldCreateNewComponent) {
            backingComponent = DaggerCustomerSessionComponent
                .builder()
                .application(getApplication())
                .configuration(configuration)
                .customerAdapter(customerAdapter)
                .customerSessionViewModel(this)
                .build()
        }

        retrievePaymentSelection()

        return component
    }

    internal fun onCustomerSheetResult(result: InternalCustomerSheetResult?) {
        requireNotNull(result)
        when (result) {
            is InternalCustomerSheetResult.Canceled -> {

            }
            is InternalCustomerSheetResult.Error -> {

            }
            is InternalCustomerSheetResult.Selected -> {
                retrievePaymentSelection()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        clear()
    }

    private fun retrievePaymentSelection() {
        viewModelScope.launch {
            val selectedPaymentOption = component.customerAdapter.retrieveSelectedPaymentOption()
            selectedPaymentOption.mapCatching { paymentOption ->
                paymentOption?.toPaymentSelection {
                    component.customerAdapter.retrievePaymentMethods().getOrNull()?.find {
                        it.id == paymentOption.id
                    }
                }?.let { paymentSelection ->
                    when (paymentSelection) {
                        is PaymentSelection.GooglePay -> {
                            PaymentOptionSelection(
                                paymentMethodId = "google_pay",
                                paymentOption = component.paymentOptionFactory.create(paymentSelection)
                            )
                        }
                        is PaymentSelection.Saved -> {
                            PaymentOptionSelection(
                                paymentMethodId = paymentSelection.paymentMethod.id!!,
                                paymentOption = component.paymentOptionFactory.create(paymentSelection)
                            )
                        }
                        else -> null
                    }
                }
            }.onSuccess { selection ->
                _paymentSelection.update {
                    selection
                }
            }.onFailure {
                _paymentSelection.update{
                    null
                }
            }
        }
    }

    internal companion object {
        internal fun clear() {
            backingComponent = null
        }

        private var backingComponent: CustomerSessionComponent? = null
        val component: CustomerSessionComponent
            get() = backingComponent ?: error("Component could not be retrieved")
    }
}
