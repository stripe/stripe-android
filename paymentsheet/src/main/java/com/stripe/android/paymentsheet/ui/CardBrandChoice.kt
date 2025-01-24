package com.stripe.android.paymentsheet.ui

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.plus
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.elements.SingleChoiceDropdownItem

internal data class CardBrandChoice(
    val brand: CardBrand,
    override val enabled: Boolean
) : SingleChoiceDropdownItem {

    override val icon: Int
        get() = brand.icon

    override val label: ResolvableString
        get() = if (enabled) {
            brand.displayName.resolvableString
        } else {
            // If it's disabled, append "not available" to the end of the brand display name
            brand.displayName.resolvableString +
                " ".resolvableString +
                resolvableString(R.string.stripe_card_brand_not_available)
        }
}
