package com.stripe.android.paymentsheet.specifications

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R

internal val bancontactParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "bancontact",
    "billing_details" to billingParams
)

val bancontact = FormSpec(
    LayoutSpec(
        listOf(
            FormElementSpec.SectionSpec(FormElementSpec.SectionSpec.SectionFieldSpec.Name),
            FormElementSpec.SectionSpec(FormElementSpec.SectionSpec.SectionFieldSpec.Email),
            FormElementSpec.StaticTextSpec(R.string.sofort_mandate, Color.Gray)
        )
    ),
    bancontactParamKey,
)