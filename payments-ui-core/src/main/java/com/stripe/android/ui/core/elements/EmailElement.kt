package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class EmailElement(
    override val identifier: IdentifierSpec = IdentifierSpec.Email,
    val initialValue: String? = "",
    override val controller: TextFieldController = SimpleTextFieldController(
        EmailConfig(),
        initialValue = initialValue
    )
) : SectionSingleFieldElement(identifier)
