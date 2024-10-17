package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.FullNameConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionSingleFieldElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldController

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class FullNameElement(
        override val identifier: IdentifierSpec = IdentifierSpec.Name,
        val label: Int?,
        val initialValue: String? = "",
        override val controller: TextFieldController = SimpleTextFieldController(
            FullNameConfig(
                label = label,
            ),
            initialValue = initialValue,
        )
    ) : SectionSingleFieldElement(identifier) {
        override val allowsUserInteraction: Boolean = true
        override val mandateText: ResolvableString? = null
    }