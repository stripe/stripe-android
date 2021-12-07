package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CountryElement(
    override val identifier: IdentifierSpec,
    override val controller: DropdownFieldController
) : SectionSingleFieldElement(identifier)
