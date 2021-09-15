package com.stripe.android.paymentsheet.forms

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.FormSpec
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.SimpleTextSpec

internal val parameters = SectionSpec(
    IdentifierSpec.Generic("generic_section"),
    SimpleTextSpec(
        IdentifierSpec.PreFilledParameterMap,
        label = R.string.playground_generic,
        capitalization = KeyboardCapitalization.Characters,
        keyboardType = KeyboardType.Text
    )
)

internal val generic = FormSpec(
    LayoutSpec(
        listOf(
            parameters,
            SaveForFutureUseSpec(emptyList())
        )
    ),
    mutableMapOf(),
)
