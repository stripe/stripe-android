package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressTextFieldElement(
    override val identifier: IdentifierSpec,
    config: TextFieldConfig,
    onNavigation: (() -> Unit)? = null
) : SectionSingleFieldElement(identifier) {

    override val controller: AddressTextFieldController =
        AddressTextFieldController(
            config,
            onNavigation
        )
}
