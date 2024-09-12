package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

internal data class CvcRecollectionViewState(
    val lastFour: String,
    val isTestMode: Boolean,
    val cvcState: CvcState,
    val isEnabled: Boolean,
)
