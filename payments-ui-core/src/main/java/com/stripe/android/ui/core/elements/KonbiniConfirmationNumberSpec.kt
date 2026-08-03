package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
@Parcelize
class KonbiniConfirmationNumberSpec : FormItemSpec() {
    @IgnoredOnParcel
    override val apiPath = IdentifierSpec.KonbiniConfirmationNumber

    fun transform(initialValues: Map<IdentifierSpec, String?>) = createSectionElement(
        SimpleTextElement(
            identifier = apiPath,
            controller = SimpleTextFieldController(
                textFieldConfig = SimpleTextFieldConfig(
                    label = resolvableString(R.string.stripe_konbini_confirmation_number_label),
                    capitalization = KeyboardCapitalization.None,
                    keyboard = KeyboardType.Phone,
                    optional = true,
                ),
                initialValue = initialValues[apiPath],
            ),
        )
    )
}
