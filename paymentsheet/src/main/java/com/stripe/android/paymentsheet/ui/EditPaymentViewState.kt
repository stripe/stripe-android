package com.stripe.android.paymentsheet.ui

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.elements.SingleChoiceDropdownItem

internal data class EditPaymentViewState(
    val last4: String,
    val canUpdate: Boolean,
    val selectedBrand: CardBrandChoice,
    val availableBrands: List<CardBrandChoice>
) {
    data class CardBrandChoice(
        val brand: CardBrand
    ) : SingleChoiceDropdownItem {
        override val icon: Int
            get() = brand.icon

        override val label: ResolvableString
            get() = resolvableString(brand.displayName)
    }
}
