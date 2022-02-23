package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.ui.core.R
import kotlinx.parcelize.Parcelize

@Parcelize
internal object AccountNumberSpec : SectionFieldSpec(IdentifierSpec.Generic("account_number")) {
    fun transform(): SectionFieldElement =
        AccountNumberElement(
            this.identifier,
            TextFieldController(
                SimpleTextFieldConfig(
                label = R.string.address_label_name,
                keyboard = KeyboardType.Number
            )
            )
        )
}
