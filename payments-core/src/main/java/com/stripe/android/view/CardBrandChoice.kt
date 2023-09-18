package com.stripe.android.view

import com.stripe.android.uicore.elements.DropdownChoice

internal data class CardBrandChoice(
    override val label: String,
    override val icon: Int?
) : DropdownChoice
