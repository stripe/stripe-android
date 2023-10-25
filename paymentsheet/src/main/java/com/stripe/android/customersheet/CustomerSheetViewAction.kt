package com.stripe.android.customersheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.forms.resources.LpmRepository

internal sealed class CustomerSheetViewAction {
    object OnDismissed : CustomerSheetViewAction()
    object OnBackPressed : CustomerSheetViewAction()
    object OnEditPressed : CustomerSheetViewAction()
    object OnAddCardPressed : CustomerSheetViewAction()
    object OnPrimaryButtonPressed : CustomerSheetViewAction()
    class OnItemSelected(val selection: PaymentSelection?) : CustomerSheetViewAction()
    class OnModifyItem(val paymentMethod: PaymentMethod) : CustomerSheetViewAction()
    class OnItemRemoved(val paymentMethod: PaymentMethod) : CustomerSheetViewAction()
    class OnAddPaymentMethodItemChanged(
        val paymentMethod: LpmRepository.SupportedPaymentMethod,
    ) : CustomerSheetViewAction()
    class OnFormFieldValuesChanged(
        val formFieldValues: FormFieldValues?,
    ) : CustomerSheetViewAction()
}
