package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.paymentsheet.R

internal data class SimpleTextSpec(
    override val identifier: IdentifierSpec,
    @StringRes val label: Int,
    val capitalization: KeyboardCapitalization,
    val keyboardType: KeyboardType,
    val showOptionalLabel: Boolean = false
) : SectionFieldSpec(identifier) {

    internal companion object {
        val NAME = SimpleTextSpec(
            IdentifierSpec.Name,
            label = R.string.address_label_name,
            capitalization = KeyboardCapitalization.Words,
            keyboardType = KeyboardType.Text
        )
    }
}
