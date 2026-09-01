package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@Parcelize
data class NameSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Name,
    @SerialName("translation_id")
    val labelTranslationId: TranslationId = TranslationId.AddressName
) : FormItemSpec() {
    fun transform(initialValues: Map<IdentifierSpec, String?>) = createSectionElement(
        SimpleTextElement(
            identifier = apiPath,
            controller = SimpleTextFieldController(
                textFieldConfig = SimpleTextFieldConfig(
                    label = resolvableString(labelTranslationId.resourceId),
                    capitalization = KeyboardCapitalization.Words,
                    keyboard = KeyboardType.Text,
                    optional = false,
                ),
                initialValue = initialValues[apiPath],
            )
        )
    )
}
