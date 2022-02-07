package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.StaticTextSpec
import com.stripe.android.ui.core.elements.billingParams

internal val sofortParams: MutableMap<String, Any?> = mutableMapOf(
    "country" to null,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val SofortParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "sofort",
    "billing_details" to billingParams,
    "sofort" to sofortParams
)

internal val sofortNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)
internal val sofortEmailSection = SectionSpec(IdentifierSpec.Generic("email_section"), EmailSpec)
internal val sofortCountrySection =
    SectionSpec(
        IdentifierSpec.Generic("country_section"),
        CountrySpec(setOf("AT", "BE", "DE", "ES", "IT", "NL"))
    )
internal val sofortMandate = StaticTextSpec(
    IdentifierSpec.Generic("mandate"),
    R.string.sepa_mandate,
    R.color.mandate_text_color
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val SofortForm = LayoutSpec.create(
    sofortNameSection,
    sofortEmailSection,
    sofortCountrySection,
    SaveForFutureUseSpec(listOf(sofortNameSection, sofortEmailSection, sofortMandate)),
    sofortMandate,
)
