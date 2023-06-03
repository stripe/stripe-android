package com.stripe.android.customersheet

import com.stripe.android.paymentsheet.PaymentOptionsItem

internal sealed class CustomerSheetViewState {
    object Loading : CustomerSheetViewState()
    data class SelectPaymentMethod(
        val title: String?,
        val paymentMethods: List<PaymentOptionsItem.SavedPaymentMethod>,
        val selectedPaymentMethodId: String?,
        val isLiveMode: Boolean,
        val isProcessing: Boolean,
        val isEditing: Boolean,
        val error: String? = null
    ) : CustomerSheetViewState()
}
