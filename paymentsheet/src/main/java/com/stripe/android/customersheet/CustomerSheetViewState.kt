package com.stripe.android.customersheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection

@OptIn(ExperimentalCustomerSheetApi::class)
internal sealed class CustomerSheetViewState {
    object Loading : CustomerSheetViewState()

    data class SelectPaymentMethod(
        val title: String?,
        val savedPaymentMethods: List<PaymentMethod>,
        val paymentSelection: PaymentSelection?,
        val showEditMenu: Boolean,
        val isLiveMode: Boolean,
        val isProcessing: Boolean,
        val isEditing: Boolean,
        val isGooglePayEnabled: Boolean,
        val primaryButtonLabel: String?,
        val primaryButtonEnabled: Boolean,
        val errorMessage: String? = null,
        val result: InternalCustomerSheetResult? = null
    ) : CustomerSheetViewState()
}
