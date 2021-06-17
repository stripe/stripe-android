package com.stripe.android.paymentsheet.specifications

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.FormElementSpec.SectionSpec
import com.stripe.android.paymentsheet.specifications.FormElementSpec.SectionSpec.SectionFieldSpec.Country
import com.stripe.android.paymentsheet.specifications.FormElementSpec.SectionSpec.SectionFieldSpec.Email
import com.stripe.android.paymentsheet.specifications.FormElementSpec.SectionSpec.SectionFieldSpec.Name
import com.stripe.android.paymentsheet.specifications.FormElementSpec.StaticTextSpec

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
            SectionSpec(Name),
            SectionSpec(Email),
            SectionSpec(Country),
            StaticTextSpec(R.string.sofort_mandate, Color.Gray)
        )
    ),
    sofortParamKey,
)

