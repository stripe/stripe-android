package com.stripe.android.customersheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection

internal sealed class CustomerSheetViewAction {
    class OnBackPressed(val from: CustomerSheetViewState) : CustomerSheetViewAction()
    object OnEditPressed : CustomerSheetViewAction()
    object OnAddCardPressed : CustomerSheetViewAction()

    object OnPrimaryButtonPressed : CustomerSheetViewAction()
    class OnItemSelected(val selection: PaymentSelection?) : CustomerSheetViewAction()
    class OnItemRemoved(val paymentMethod: PaymentMethod) : CustomerSheetViewAction()
}
