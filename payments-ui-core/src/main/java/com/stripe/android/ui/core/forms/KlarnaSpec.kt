package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.KlarnaCountrySpec
import com.stripe.android.ui.core.elements.KlarnaHelper
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.StaticTextSpec

internal val klarnaHeader = StaticTextSpec(
    api_path = IdentifierSpec.Generic("klarna_header"),
    stringResId = KlarnaHelper.getKlarnaHeader(),
)

internal val klarnaEmailSection =
    SectionSpec(IdentifierSpec.Generic("email_section"), EmailSpec)

internal val klarnaBillingSection = SectionSpec(
    IdentifierSpec.Generic("country_section"),
    KlarnaCountrySpec()
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val KlarnaForm = LayoutSpec.create(
    klarnaHeader,
    klarnaEmailSection,
    klarnaBillingSection
)
