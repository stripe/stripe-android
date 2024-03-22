package com.stripe.android.financialconnections.features.accountpicker

import com.stripe.android.financialconnections.model.PartnerAccount

internal sealed interface AccountPickerViewAction {
    data class OnAccountClicked(val account: PartnerAccount) : AccountPickerViewAction
    data object OnSubmitClicked : AccountPickerViewAction
    data class OnClickableTextClicked(val text: String) : AccountPickerViewAction
}
