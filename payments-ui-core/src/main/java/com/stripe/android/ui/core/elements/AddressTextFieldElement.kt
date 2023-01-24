package com.stripe.android.ui.core.elements

import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.TextFieldConfig

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
