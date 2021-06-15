package com.stripe.android.paymentsheet.forms

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormElementSpec.SectionSpec
import com.stripe.android.paymentsheet.forms.FormElementSpec.SectionSpec.SectionFieldSpec.Country
import com.stripe.android.paymentsheet.forms.FormElementSpec.SectionSpec.SectionFieldSpec.Email
import com.stripe.android.paymentsheet.forms.FormElementSpec.SectionSpec.SectionFieldSpec.Name
import com.stripe.android.paymentsheet.forms.FormElementSpec.StaticSpec.TextSpec

internal val sofortParams: MutableMap<String, Any?> = mutableMapOf(
    "country" to null,
)

internal val sofortParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "sofort",
    "billing_details" to billingParams,
    "sofort" to sofortParams
)

val sofort = FormSpec(
    Layout(
        listOf(
            SectionSpec(Name),
            SectionSpec(Email),
            SectionSpec(Country),
            TextSpec(R.string.sofort_mandate, Color.Gray)
        )
    ),
    sofortParamKey,
)

