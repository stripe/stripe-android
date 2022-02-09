package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class SectionMultiFieldElement(
    override val identifier: IdentifierSpec,
) : SectionFieldElement
