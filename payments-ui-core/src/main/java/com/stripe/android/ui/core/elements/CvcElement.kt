package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionSingleFieldElement

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CvcElement(
    val _identifier: IdentifierSpec,
    override val controller: CvcController
) : SectionSingleFieldElement(_identifier) {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        // Nothing from FormArguments to populate
        rawValuesMap[identifier]?.let {
            controller.onRawValueChange(it)
        }
    }
}
