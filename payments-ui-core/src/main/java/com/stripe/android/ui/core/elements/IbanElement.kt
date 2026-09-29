package com.stripe.android.ui.core.elements

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.SectionSingleFieldElement
import com.stripe.android.uicore.elements.TextFieldController

internal data class IbanElement(
    override val identifier: FormFieldId,
    override val controller: TextFieldController
) : SectionSingleFieldElement(identifier) {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null
}
