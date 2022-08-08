package com.stripe.android.ui.core.elements

internal class AddressTextFieldElement(
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
