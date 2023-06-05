package com.stripe.android.customersheet

internal sealed class CustomerSheetViewState {
    object Loading : CustomerSheetViewState()
    class Data(val data: String) : CustomerSheetViewState()
}
