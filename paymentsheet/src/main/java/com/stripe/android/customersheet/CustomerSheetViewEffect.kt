package com.stripe.android.customersheet

internal sealed class CustomerSheetViewEffect {
    object NavigateUp : CustomerSheetViewEffect()
}
