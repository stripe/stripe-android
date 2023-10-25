package com.stripe.android.paymentsheet.ui

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.SingleChoiceDropdownItem

internal data class EditPaymentViewState(
    val last4: String,
    val canUpdate: Boolean,
    val selectedBrand: CardBrandChoice,
    val availableBrands: List<CardBrandChoice>
) {
    data class CardBrandChoice(
        val id: String,
        override val label: ResolvableString,
        override val icon: Int
    ) : SingleChoiceDropdownItem
}
