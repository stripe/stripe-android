package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class PhoneNumberElement(
    override val identifier: IdentifierSpec,
    override val controller: PhoneNumberController
) : SectionSingleFieldElement(identifier)
