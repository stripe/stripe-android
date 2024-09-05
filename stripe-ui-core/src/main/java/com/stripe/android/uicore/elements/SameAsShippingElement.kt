package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SameAsShippingElement(
    override val identifier: IdentifierSpec,
    override val controller: SameAsShippingController
) : SectionSingleFieldElement(identifier) {
    override val shouldRenderOutsideCard: Boolean
        get() = true

    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        rawValuesMap[identifier]?.let {
            controller.onRawValueChange(it)
        }
    }
}
