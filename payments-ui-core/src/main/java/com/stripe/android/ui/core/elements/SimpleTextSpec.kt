package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.ui.core.R
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class SimpleTextSpec(
    override val identifier: IdentifierSpec,
    @StringRes val label: Int,
    val capitalization: KeyboardCapitalization,
    val keyboardType: KeyboardType,
    val showOptionalLabel: Boolean = false
) : SectionFieldSpec(identifier) {

    companion object {
        val NAME = SimpleTextSpec(
            IdentifierSpec.Name,
            label = R.string.address_label_name,
            capitalization = KeyboardCapitalization.Words,
            keyboardType = KeyboardType.Text
        )
    }

    fun transform(
        initialValues: Map<IdentifierSpec, String?> = mapOf()
    ): SectionSingleFieldElement =
        SimpleTextElement(
            this.identifier,
            TextFieldController(
                SimpleTextFieldConfig(
                    label = this.label,
                    capitalization = this.capitalization,
                    keyboard = this.keyboardType
                ),
                initialValue = initialValues[this.identifier],
                showOptionalLabel = this.showOptionalLabel
            )
        )
}
