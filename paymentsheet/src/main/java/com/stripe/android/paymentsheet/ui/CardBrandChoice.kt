package com.stripe.android.paymentsheet.ui

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.elements.SingleChoiceDropdownItem

internal data class CardBrandChoice(
    val brand: CardBrand
) : SingleChoiceDropdownItem {
    override val icon: Int
        get() = brand.icon

    override val label: ResolvableString
        get() = brand.displayName.resolvableString
}
