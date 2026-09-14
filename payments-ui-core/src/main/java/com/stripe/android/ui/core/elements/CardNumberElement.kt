package com.stripe.android.ui.core.elements

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.SectionSingleFieldElement

internal data class CardNumberElement(
    val _identifier: FormFieldId,
    override val controller: CardNumberController
) : SectionSingleFieldElement(_identifier) {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    override fun setRawValue(rawValuesMap: Map<FormFieldId, String?>) {
        // Nothing from FormArguments to populate
    }
}
