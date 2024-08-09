package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

internal sealed interface CvcRecollectionViewAction {
    data class OnConfirmPressed(val cvc: String) : CvcRecollectionViewAction
    data object OnBackPressed : CvcRecollectionViewAction
}

data class CvcStateChanged(val state: CvcState)

