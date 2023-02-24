package com.stripe.android.ui.core.elements

import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionSingleFieldElement
import com.stripe.android.uicore.elements.TextFieldController

internal data class IbanElement(
    override val identifier: IdentifierSpec,
    override val controller: TextFieldController
) : SectionSingleFieldElement(identifier)
