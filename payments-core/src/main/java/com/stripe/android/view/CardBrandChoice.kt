package com.stripe.android.view

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.SingleChoiceDropdownItem

internal data class CardBrandChoice(
    override val label: ResolvableString,
    override val icon: Int?,
    override val enabled: Boolean
) : SingleChoiceDropdownItem
