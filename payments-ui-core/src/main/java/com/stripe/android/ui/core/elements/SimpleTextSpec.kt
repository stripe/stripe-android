package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class SimpleTextSpec(
    override val api_path: IdentifierSpec,
    @StringRes val label: Int,
    val capitalization: @RawValue KeyboardCapitalization,
    val keyboardType: @RawValue KeyboardType,
    val showOptionalLabel: Boolean = false
) : FormItemSpec(), RequiredItemSpec {
    fun transform(
        initialValues: Map<IdentifierSpec, String?> = mapOf()
    ) = createSectionElement(
        SimpleTextElement(
            this.api_path,
            SimpleTextFieldController(
                SimpleTextFieldConfig(
                    label = this.label,
                    capitalization = this.capitalization,
                    keyboard = this.keyboardType
                ),
                initialValue = initialValues[this.api_path],
                showOptionalLabel = this.showOptionalLabel
            )
        )
    )
}
