package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.AffirmTextSpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.IdentifierSpec

internal val affirmHeader = AffirmTextSpec(
    IdentifierSpec.Generic("affirm_header")
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val AffirmForm: List<FormItemSpec> = listOf(
    affirmHeader
)
