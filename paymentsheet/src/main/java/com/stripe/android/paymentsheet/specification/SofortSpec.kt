package com.stripe.android.paymentsheet.specification

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specification.FormElementSpec.SectionSpec
import com.stripe.android.paymentsheet.specification.FormElementSpec.SectionSpec.SectionFieldSpec.Country
import com.stripe.android.paymentsheet.specification.FormElementSpec.SectionSpec.SectionFieldSpec.Email
import com.stripe.android.paymentsheet.specification.FormElementSpec.SectionSpec.SectionFieldSpec.Name
import com.stripe.android.paymentsheet.specification.FormElementSpec.StaticSpec.TextSpec

internal val sofortParams: MutableMap<String, Any?> = mutableMapOf(
    "country" to null,
)

internal val sofortParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "sofort",
    "billing_details" to billingParams,
    "sofort" to sofortParams
)

val sofort = FormSpec(
    LayoutSpec(
        listOf(
            SectionSpec(IdentifierSpec("name"), Name),
            SectionSpec(IdentifierSpec("email"), Email),
            SectionSpec(IdentifierSpec("country"), Country),
            TextSpec(IdentifierSpec("mandate"), R.string.sofort_mandate, Color.Gray)
        )
    ),
    sofortParamKey,
)

