package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.LayoutSpec

internal val sofortCountrySection = CountrySpec(
    onlyShowCountryCodes = setOf("AT", "BE", "DE", "ES", "IT", "NL")
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val SofortForm = LayoutSpec.create(
    sofortCountrySection,
)
