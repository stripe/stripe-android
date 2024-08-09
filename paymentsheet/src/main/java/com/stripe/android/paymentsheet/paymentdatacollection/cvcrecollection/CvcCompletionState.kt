package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

sealed interface CvcCompletionState {
    data object Incomplete : CvcCompletionState
    data class Completed(val cvc: String) : CvcCompletionState
}

data class CvcState(val cvc: String = "", val isComplete: Boolean = false)
