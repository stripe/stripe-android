package com.stripe.android.customersheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.forms.resources.LpmRepository

internal sealed class CustomerSheetViewAction {
    object OnDismissed : CustomerSheetViewAction()
    object OnBackPressed : CustomerSheetViewAction()
    object OnEditPressed : CustomerSheetViewAction()
    object OnAddCardPressed : CustomerSheetViewAction()
    object OnPrimaryButtonPressed : CustomerSheetViewAction()
    class OnFormDataUpdated(val formData: FormViewModel.ViewData) : CustomerSheetViewAction()
    class OnItemSelected(val selection: PaymentSelection?) : CustomerSheetViewAction()
    class OnItemRemoved(val paymentMethod: PaymentMethod) : CustomerSheetViewAction()
    class OnPaymentOptionFormItemSelected(val selection: LpmRepository.SupportedPaymentMethod) : CustomerSheetViewAction()
    class OnUpdatePrimaryButton(
        val text: String,
        val enabled: Boolean,
        val shouldShowProcessing: Boolean,
        val onClick: () -> Unit
    ) : CustomerSheetViewAction()

    class OnUpdatePrimaryButtonUiState(
        val uiState: PrimaryButton.UIState?,
    ) : CustomerSheetViewAction()
    class OnAddPaymentMethodError(val error: String?) : CustomerSheetViewAction()
    class OnUpdateMandateText(val mandate: String?) : CustomerSheetViewAction()
    class OnConfirmUSBankAccount(val selection: PaymentSelection.New.USBankAccount) : CustomerSheetViewAction()
}
