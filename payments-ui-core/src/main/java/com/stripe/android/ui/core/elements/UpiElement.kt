package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.InputController
import com.stripe.android.uicore.elements.SectionSingleFieldElement
import com.stripe.android.uicore.elements.SimpleTextFieldController

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class UpiElement(
    override val controller: InputController = SimpleTextFieldController(
        textFieldConfig = UpiConfig()
    )
) : SectionSingleFieldElement(identifier = IdentifierSpec.Vpa) {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    override val identifier: IdentifierSpec = IdentifierSpec.Vpa
}
