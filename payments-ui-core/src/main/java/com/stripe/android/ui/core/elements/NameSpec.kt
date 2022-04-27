package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.ui.core.R
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class NameSpec(
    override val identifier: IdentifierSpec = IdentifierSpec.Name
) : FormItemSpec(), RequiredItemSpec {
    fun transform(initialValues: Map<IdentifierSpec, String?>): SectionElement {
        val sectionFieldElement = SimpleTextElement(
            this.identifier,
            SimpleTextFieldController(
                SimpleTextFieldConfig(
                    label = R.string.address_label_name,
                    capitalization = KeyboardCapitalization.Words,
                    keyboard = KeyboardType.Text
                ),
                initialValue = initialValues[this.identifier]
            ),
        )
        return SectionElement(
            IdentifierSpec.Generic(identifier.value + "_section"),
            sectionFieldElement,
            SectionController(
                null,
                listOf(sectionFieldElement.sectionFieldErrorController())
            )
        )
    }
}
