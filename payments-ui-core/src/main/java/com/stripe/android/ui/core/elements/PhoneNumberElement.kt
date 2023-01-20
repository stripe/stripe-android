package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberController

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class PhoneNumberElement(
    override val identifier: IdentifierSpec,
    override val controller: PhoneNumberController
) : SectionSingleFieldElement(identifier)
