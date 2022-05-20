package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.KlarnaCountrySpec
import com.stripe.android.ui.core.elements.KlarnaHeaderStaticTextSpec

internal val klarnaHeader = KlarnaHeaderStaticTextSpec(
    api_path = IdentifierSpec.Generic("klarna_header"),
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val KlarnaForm: List<FormItemSpec> = listOf(
    klarnaHeader,
    EmailSpec(),
    KlarnaCountrySpec()
)
