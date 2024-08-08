package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

sealed interface CVCRecollectionCompletion {
    data object Incomplete : CVCRecollectionCompletion
    data class Completed(val cvc: String): CVCRecollectionCompletion
}