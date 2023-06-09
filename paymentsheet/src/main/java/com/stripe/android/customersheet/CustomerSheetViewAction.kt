package com.stripe.android.customersheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection

internal sealed class CustomerSheetViewAction {
    object OnBackPressed : CustomerSheetViewAction()
    object OnEditPressed : CustomerSheetViewAction()
    object OnAddCardPressed : CustomerSheetViewAction()

    object OnPrimaryButtonPressed : CustomerSheetViewAction()
    class OnItemSelected(val selection: PaymentSelection?) : CustomerSheetViewAction()
    class OnItemRemoved(val paymentMethod: PaymentMethod) : CustomerSheetViewAction()
}
