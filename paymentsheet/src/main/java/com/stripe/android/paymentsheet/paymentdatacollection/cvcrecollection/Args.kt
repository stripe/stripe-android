package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.model.CardBrand

data class Args(
    val lastFour: String,
    val cardBrand: CardBrand,
    val cvc: String? = null,
    val isTestMode: Boolean
)
