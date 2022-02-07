package com.stripe.android.paymentsheet.elements

import androidx.annotation.ColorRes
import androidx.annotation.StringRes

/**
 * This is for elements that do not receive user input
 */
internal data class StaticTextSpec(
    override val identifier: IdentifierSpec,
    @StringRes val stringResId: Int,
    @ColorRes val color: Int? = null,
    val fontSizeSp: Int = 10,
    val letterSpacingSp: Double = .7
) : FormItemSpec(), RequiredItemSpec
