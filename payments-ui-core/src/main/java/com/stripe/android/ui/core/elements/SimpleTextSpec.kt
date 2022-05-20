package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Capitalization {
    None,
    Characters,
    Words,
    Sentences
}

@Serializable
enum class KeyboardType {
    Text,
    Ascii,
    Number,
    Phone,
    Uri,
    Email,
    Password,
    NumberPassword,
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@SerialName("text")
data class SimpleTextSpec(
    override val api_path: IdentifierSpec,
    @StringRes val label: Int,
    val capitalization: Capitalization,
    val keyboardType: KeyboardType,
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
                    capitalization = when (this.capitalization) {
                        Capitalization.None -> KeyboardCapitalization.None
                        Capitalization.Characters -> KeyboardCapitalization.Characters
                        Capitalization.Words -> KeyboardCapitalization.Words
                        Capitalization.Sentences -> KeyboardCapitalization.Sentences
                    },
                    keyboard = when (this.keyboardType) {
                        KeyboardType.Text -> androidx.compose.ui.text.input.KeyboardType.Text
                        KeyboardType.Ascii -> androidx.compose.ui.text.input.KeyboardType.Ascii
                        KeyboardType.Number -> androidx.compose.ui.text.input.KeyboardType.Number
                        KeyboardType.Phone -> androidx.compose.ui.text.input.KeyboardType.Phone
                        KeyboardType.Uri -> androidx.compose.ui.text.input.KeyboardType.Uri
                        KeyboardType.Email -> androidx.compose.ui.text.input.KeyboardType.Email
                        KeyboardType.Password -> androidx.compose.ui.text.input.KeyboardType.Password
                        KeyboardType.NumberPassword -> androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    }
                ),
                initialValue = initialValues[this.api_path],
                showOptionalLabel = this.showOptionalLabel
            )
        )
    )
}
