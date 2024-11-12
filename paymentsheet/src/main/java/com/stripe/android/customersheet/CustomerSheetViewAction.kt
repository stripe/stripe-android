package com.stripe.android.customersheet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton

internal sealed class CustomerSheetViewAction {
    object OnDismissed : CustomerSheetViewAction()
    object OnBackPressed : CustomerSheetViewAction()
    object OnEditPressed : CustomerSheetViewAction()
    object OnCardNumberInputCompleted : CustomerSheetViewAction()
    object OnAddCardPressed : CustomerSheetViewAction()
    object OnPrimaryButtonPressed : CustomerSheetViewAction()
    object OnCancelClose : CustomerSheetViewAction()
    class OnDisallowedCardBrandEntered(val brand: CardBrand) : CustomerSheetViewAction()
    class OnItemSelected(val selection: PaymentSelection?) : CustomerSheetViewAction()
    class OnModifyItem(val paymentMethod: PaymentMethod) : CustomerSheetViewAction()
    class OnItemRemoved(val paymentMethod: PaymentMethod) : CustomerSheetViewAction()
    class OnAddPaymentMethodItemChanged(
        val paymentMethod: SupportedPaymentMethod,
    ) : CustomerSheetViewAction()
    class OnFormFieldValuesCompleted(
        val formFieldValues: FormFieldValues?,
    ) : CustomerSheetViewAction()
    class OnUpdateCustomButtonUIState(
        val callback: (PrimaryButton.UIState?) -> PrimaryButton.UIState?,
    ) : CustomerSheetViewAction()
    class OnUpdateMandateText(
        val mandateText: ResolvableString?,
        val showAbovePrimaryButton: Boolean,
    ) : CustomerSheetViewAction()
    class OnBankAccountSelectionChanged(
        val paymentSelection: PaymentSelection.New.USBankAccount?,
    ) : CustomerSheetViewAction()
    class OnFormError(
        val error: ResolvableString?,
    ) : CustomerSheetViewAction()
}
