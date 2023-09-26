package com.stripe.android.view

import com.stripe.android.uicore.elements.SingleChoiceDropdownItem

internal data class CardBrandChoice(
    override val label: String,
    override val icon: Int?
) : SingleChoiceDropdownItem
