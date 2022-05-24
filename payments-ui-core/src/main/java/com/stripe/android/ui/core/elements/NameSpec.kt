package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@SerialName("name")
@Parcelize
data class NameSpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Name,
    val label: StringRepository.TranslationId = StringRepository.TranslationId.AddressName
) : FormItemSpec(), RequiredItemSpec {
    fun transform(initialValues: Map<IdentifierSpec, String?>) = createSectionElement(
        SimpleTextElement(
            this.api_path,
            SimpleTextFieldController(
                SimpleTextFieldConfig(
                    label = label.resourceId,
                    capitalization = KeyboardCapitalization.Words,
                    keyboard = KeyboardType.Text
                ),
                initialValue = initialValues[this.api_path]
            ),
        )
    )
}
