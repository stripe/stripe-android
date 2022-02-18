package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.AffirmTextSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec


@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val AffirmParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "affirm"
)

internal val affirmHeader = AffirmTextSpec(
    IdentifierSpec.Generic("affirm_header")
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val AffirmForm = LayoutSpec.create(
    affirmHeader
)
