package com.stripe.android.customersheet

import com.stripe.android.paymentsheet.PaymentOptionsItem

@OptIn(ExperimentalCustomerSheetApi::class)
internal sealed class CustomerSheetViewState {
    object Loading : CustomerSheetViewState()

    @Suppress("unused")
    data class SelectPaymentMethod(
        val config: CustomerSheet.Configuration,
        val title: String?,
        val paymentMethods: List<PaymentOptionsItem.SavedPaymentMethod>,
        val selectedPaymentMethodId: String?,
        val isLiveMode: Boolean,
        val isProcessing: Boolean,
        val isEditing: Boolean,
        val primaryButtonLabel: String?,
        val primaryButtonEnabled: Boolean,
        val errorMessage: String? = null,
        val result: InternalCustomerSheetResult? = null
    ) : CustomerSheetViewState()
}
