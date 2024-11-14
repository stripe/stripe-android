package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.model.CardBrand

internal data class Args(
    val lastFour: String,
    val cardBrand: CardBrand,
    val cvc: String,
    val isTestMode: Boolean
)
