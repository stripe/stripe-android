package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color

/**
 * This is for elements that do not receive user input
 */
internal data class MandateTextSpec(
    override val identifier: IdentifierSpec,
    @StringRes val stringResId: Int,
    val color: Color
) : FormItemSpec(), RequiredItemSpec
