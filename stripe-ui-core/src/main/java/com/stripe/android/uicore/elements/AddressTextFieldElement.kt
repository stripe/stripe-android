package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressTextFieldElement(
    override val identifier: IdentifierSpec,
    label: ResolvableString,
    optional: Boolean,
    onNavigation: (() -> Unit)? = null
) : SectionSingleFieldElement(identifier) {
    override val allowsUserInteraction: Boolean = true
    override val mandateText: ResolvableString? = null

    override val controller: AddressTextFieldController =
        AddressTextFieldController(
            label = label,
            optional = optional,
            onNavigation = onNavigation
        )
}
