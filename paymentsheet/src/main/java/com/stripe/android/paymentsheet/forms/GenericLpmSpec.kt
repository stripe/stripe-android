package com.stripe.android.paymentsheet.forms

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec

internal val parameters = SectionSpec(
    IdentifierSpec.Generic("generic_section"),
    SimpleTextSpec(
        IdentifierSpec.PreFilledParameterMap,
        label = R.string.playground_generic,
        capitalization = KeyboardCapitalization.Characters,
        keyboardType = KeyboardType.Text
    )
)

internal val generic =
    LayoutSpec.create(
        parameters,
        SaveForFutureUseSpec(emptyList())
    )
