package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionSingleFieldElement
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldController

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class EmailElement(
    override val identifier: IdentifierSpec = IdentifierSpec.Email,
    val initialValue: String? = "",
    override val controller: TextFieldController = SimpleTextFieldController(
        EmailConfig(),
        initialValue = initialValue
    )
) : SectionSingleFieldElement(identifier)
