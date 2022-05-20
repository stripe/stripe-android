package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.ui.core.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@SerialName("name")
data class NameSpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Name
) : FormItemSpec(), RequiredItemSpec {
    fun transform(initialValues: Map<IdentifierSpec, String?>) = createSectionElement(
        SimpleTextElement(
            this.api_path,
            SimpleTextFieldController(
                SimpleTextFieldConfig(
                    label = R.string.address_label_name,
                    capitalization = KeyboardCapitalization.Words,
                    keyboard = KeyboardType.Text
                ),
                initialValue = initialValues[this.api_path]
            ),
        )
    )
}
