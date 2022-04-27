package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec

internal val sofortCountrySection =
    SectionSpec(
        IdentifierSpec.Generic("country_section"),
        CountrySpec(setOf("AT", "BE", "DE", "ES", "IT", "NL"))
    )
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val SofortForm = LayoutSpec.create(
    sofortCountrySection,
)
