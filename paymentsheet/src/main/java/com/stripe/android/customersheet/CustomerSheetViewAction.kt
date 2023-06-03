package com.stripe.android.customersheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection

internal sealed class CustomerSheetViewAction {
    object OnBackPress : CustomerSheetViewAction()
    object OnEdit : CustomerSheetViewAction()
    object OnAddCardPressed : CustomerSheetViewAction()
    class OnItemSelected(val selection: PaymentSelection?) : CustomerSheetViewAction()
    class OnItemRemoved(val paymentMethod: PaymentMethod) : CustomerSheetViewAction()
}
