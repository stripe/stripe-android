package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("EnumEntryName")
@Serializable
enum class Capitalization {
    @SerialName("none")
    None,

    @SerialName("characters")
    Characters,

    @SerialName("words")
    Words,

    @SerialName("sentences")
    Sentences
}

@Suppress("EnumEntryName")
@Serializable
enum class KeyboardType {
    @SerialName("text")
    Text,

    @SerialName("ascii")
    Ascii,

    @SerialName("number")
    Number,

    @SerialName("phone")
    Phone,

    @SerialName("uri")
    Uri,

    @SerialName("email")
    Email,

    @SerialName("password")
    Password,

    @SerialName("number_password")
    NumberPassword,
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class SimpleTextSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec,

    @SerialName("label")
    @StringRes val label: Int,

    @SerialName("capitalization")
    val capitalization: Capitalization = Capitalization.None,

    @SerialName("keyboard_type")
    val keyboardType: KeyboardType = KeyboardType.Ascii,

    @SerialName("show_optional_label")
    val showOptionalLabel: Boolean = false
) : FormItemSpec() {
    fun transform(
        initialValues: Map<IdentifierSpec, String?> = mapOf()
    ) = createSectionElement(
        SimpleTextElement(
            this.apiPath,
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
                initialValue = initialValues[this.apiPath],
                showOptionalLabel = this.showOptionalLabel
            )
        )
    )
}
