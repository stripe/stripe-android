package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

internal sealed interface CvcRecollectionViewAction {
    data class OnConfirmPressed(val cvc: String) : CvcRecollectionViewAction
    object OnBackPressed : CvcRecollectionViewAction
    data class CvcCompletionChanged(val completion: CvcCompletionState) : CvcRecollectionViewAction
}
