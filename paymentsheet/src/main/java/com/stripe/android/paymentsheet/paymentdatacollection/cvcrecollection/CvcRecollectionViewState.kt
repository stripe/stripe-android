package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.model.CardBrand

internal data class CvcRecollectionViewState(
    val cardBrand: CardBrand,
    val lastFour: String,
    val cvc: String?,
    val displayMode: Args.DisplayMode
)
