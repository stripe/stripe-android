package com.stripe.android.paymentsheet.ui

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.elements.SingleChoiceDropdownItem

internal data class EditPaymentMethodViewState constructor(
    val status: Status,
    val last4: String,
    val displayName: String,
    val canUpdate: Boolean,
    val selectedBrand: CardBrandChoice,
    val availableBrands: List<CardBrandChoice>,
    val confirmRemoval: Boolean = false,
    val error: ResolvableString? = null,
) {
    enum class Status {
        Idle,
        Updating,
        Removing
    }

    data class CardBrandChoice(
        val brand: CardBrand
    ) : SingleChoiceDropdownItem {
        override val icon: Int
            get() = brand.icon

        override val label: ResolvableString
            get() = resolvableString(brand.displayName)
    }
}
