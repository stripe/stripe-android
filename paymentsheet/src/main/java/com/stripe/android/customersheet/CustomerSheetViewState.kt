package com.stripe.android.customersheet

internal sealed class CustomerSheetViewState {
    object Loading : CustomerSheetViewState()
    class SelectPaymentMethod(
        val title: String?
    ) : CustomerSheetViewState()
}
