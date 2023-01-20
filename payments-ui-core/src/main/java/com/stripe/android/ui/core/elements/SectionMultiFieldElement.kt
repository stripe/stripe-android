package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionFieldElement

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class SectionMultiFieldElement(
    override val identifier: IdentifierSpec
) : SectionFieldElement
