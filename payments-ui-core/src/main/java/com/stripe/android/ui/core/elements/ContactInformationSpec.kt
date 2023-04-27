package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class ContactInformationSpec(
    @SerialName("collect_name")
    val collectName: Boolean = true,
    @SerialName("collect_email")
    val collectEmail: Boolean = true,
    @SerialName("collect_phone")
    val collectPhone: Boolean = true,
) : FormItemSpec() {
    override val apiPath: IdentifierSpec = IdentifierSpec()

    fun transform(initialValues: Map<IdentifierSpec, String?>): SectionElement? {
        val elements = listOfNotNull(
            SimpleTextElement(
                controller = SimpleTextFieldController(
                    textFieldConfig = SimpleTextFieldConfig(
                        label = R.string.stripe_name_on_card,
                        capitalization = KeyboardCapitalization.Words,
                        keyboard = KeyboardType.Text
                    ),
                    initialValue = initialValues[IdentifierSpec.Name],
                ),
                identifier = IdentifierSpec.Name,
            ).takeIf { collectName },
            EmailElement(
                initialValue = initialValues[IdentifierSpec.Email]
            ).takeIf { collectEmail },
            PhoneNumberElement(
                identifier = IdentifierSpec.Phone,
                controller = PhoneNumberController(initialValues[IdentifierSpec.Phone] ?: "")
            ).takeIf { collectPhone },
        )

        if (elements.isEmpty()) return null

        return createSectionElement(
            label = R.string.stripe_contact_information,
            sectionFieldElements = elements,
        )
    }
}
