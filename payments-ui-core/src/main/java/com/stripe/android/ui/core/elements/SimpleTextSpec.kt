package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.input.KeyboardCapitalization
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("EnumEntryName")
@Serializable
enum class Capitalization {
    none,
    characters,
    words,
    sentences
}

@Suppress("EnumEntryName")
@Serializable
enum class KeyboardType {
    text,
    ascii,
    number,
    phone,
    uri,
    email,
    password,
    number_password,
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@SerialName("text")
@Parcelize
internal open class SimpleTextSpec(
    override val api_path: IdentifierSpec,
    val label: StringRepository.TranslationId,
    val capitalization: Capitalization = Capitalization.none,
    val keyboardType: KeyboardType = KeyboardType.ascii,
    val showOptionalLabel: Boolean = false
) : FormItemSpec(), RequiredItemSpec {
    fun transform(
        initialValues: Map<IdentifierSpec, String?> = mapOf()
    ) = createSectionElement(
        SimpleTextElement(
            this.api_path,
            SimpleTextFieldController(
                SimpleTextFieldConfig(
                    label = this.label.resourceId,
                    capitalization = when (this.capitalization) {
                        Capitalization.none -> KeyboardCapitalization.None
                        Capitalization.characters -> KeyboardCapitalization.Characters
                        Capitalization.words -> KeyboardCapitalization.Words
                        Capitalization.sentences -> KeyboardCapitalization.Sentences
                    },
                    keyboard = when (this.keyboardType) {
                        KeyboardType.text -> androidx.compose.ui.text.input.KeyboardType.Text
                        KeyboardType.ascii -> androidx.compose.ui.text.input.KeyboardType.Ascii
                        KeyboardType.number -> androidx.compose.ui.text.input.KeyboardType.Number
                        KeyboardType.phone -> androidx.compose.ui.text.input.KeyboardType.Phone
                        KeyboardType.uri -> androidx.compose.ui.text.input.KeyboardType.Uri
                        KeyboardType.email -> androidx.compose.ui.text.input.KeyboardType.Email
                        KeyboardType.password -> androidx.compose.ui.text.input.KeyboardType.Password
                        KeyboardType.number_password -> androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    }
                ),
                initialValue = initialValues[this.api_path],
                showOptionalLabel = this.showOptionalLabel
            )
        )
    )
}
