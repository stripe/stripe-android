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
    Sentences;

    companion object {
        fun from(str: String) = when (str) {
            "none" -> None
            "characters" -> Characters
            "words" -> Words
            "sentences" -> Sentences
            else -> None
        }
    }
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
    NumberPassword;

    companion object {
        fun from(str: String) = when (str) {
            "text" -> Text
            "ascii" -> Ascii
            "number" -> Number
            "phone" -> Phone
            "uri" -> Uri
            "email" -> Email
            "password" -> Password
            "number_password" -> NumberPassword
            else -> Text
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class SimpleTextSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec,

    @SerialName("label")
    @StringRes
    val label: Int,

    @SerialName("capitalization")
    val capitalization: Capitalization = DEFAULT_CAPITALIZATION,

    @SerialName("keyboard_type")
    val keyboardType: KeyboardType = DEFAULT_KEYBOARD_TYPE,

    @SerialName("show_optional_label")
    val showOptionalLabel: Boolean = DEFAULT_SHOW_OPTIONAL_LABEL
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

    companion object {

        val DEFAULT_CAPITALIZATION = Capitalization.None

        val DEFAULT_KEYBOARD_TYPE = KeyboardType.Ascii

        val DEFAULT_SHOW_OPTIONAL_LABEL = false
    }
}
