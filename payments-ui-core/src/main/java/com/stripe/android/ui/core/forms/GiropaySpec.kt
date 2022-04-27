package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec

internal val giropayNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    NameSpec
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val GiropayForm = LayoutSpec.create(giropayNameSection)
