package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

internal sealed interface CvcRecollectionViewAction {
    data object OnConfirmPressed : CvcRecollectionViewAction
    data object OnBackPressed : CvcRecollectionViewAction
    data class OnCvcChanged(val cvc: String) : CvcRecollectionViewAction
}
