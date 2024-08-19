package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.elements.CvcController

internal data class CvcRecollectionViewState(
    val cardBrand: CardBrand,
    val lastFour: String,
    val cvc: String?,
    val isTestMode: Boolean,
    val controller: CvcController
)
