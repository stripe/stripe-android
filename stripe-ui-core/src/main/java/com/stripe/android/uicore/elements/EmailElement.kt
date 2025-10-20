package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class EmailElement(
    override val identifier: IdentifierSpec = IdentifierSpec.Email,
    val initialValue: String? = "",
    override val controller: TextFieldController = SimpleTextFieldController(
        EmailConfig(),
        initialValue = initialValue
    )
) : SectionSingleFieldElement(identifier) {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null
}
