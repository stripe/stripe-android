package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class SectionMultiFieldElement(
    override val identifier: IdentifierSpec
) : SectionFieldElement
