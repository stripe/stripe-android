package com.stripe.android.customersheet

internal sealed class CustomerSheetAction {
    object NavigateUp : CustomerSheetAction()
}
