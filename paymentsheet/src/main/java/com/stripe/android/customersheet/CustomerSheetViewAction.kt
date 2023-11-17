package com.stripe.android.customersheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.forms.resources.LpmRepository

internal sealed class CustomerSheetViewAction {
    object OnDismissed : CustomerSheetViewAction()
    object OnBackPressed : CustomerSheetViewAction()
    object OnEditPressed : CustomerSheetViewAction()
    object OnAddCardPressed : CustomerSheetViewAction()
    object OnPrimaryButtonPressed : CustomerSheetViewAction()
    object OnCancelClose : CustomerSheetViewAction()
    class OnItemSelected(val selection: PaymentSelection?) : CustomerSheetViewAction()
    class OnModifyItem(val paymentMethod: PaymentMethod) : CustomerSheetViewAction()
    class OnItemRemoved(val paymentMethod: PaymentMethod) : CustomerSheetViewAction()
    class OnAddPaymentMethodItemChanged(
        val paymentMethod: LpmRepository.SupportedPaymentMethod,
    ) : CustomerSheetViewAction()
    class OnFormFieldValuesCompleted(
        val formFieldValues: FormFieldValues?,
    ) : CustomerSheetViewAction()
    class OnUpdateCustomButtonUIState(
        val callback: (PrimaryButton.UIState?) -> PrimaryButton.UIState?,
    ) : CustomerSheetViewAction()
    class OnUpdateMandateText(
        val mandateText: String?,
        val showAbovePrimaryButton: Boolean,
    ) : CustomerSheetViewAction()
    class OnConfirmUSBankAccount(
        val usBankAccount: PaymentSelection.New.USBankAccount,
    ) : CustomerSheetViewAction()
    class OnCollectBankAccountResult(
        val bankAccountResult: CollectBankAccountResultInternal,
    ) : CustomerSheetViewAction()
    class OnFormError(
        val error: String?,
    ) : CustomerSheetViewAction()
}
