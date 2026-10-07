package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

internal data class CvcRecollectionViewState(
    val lastFour: String,
    val cvcState: CvcState,
    val isEnabled: Boolean,
)
