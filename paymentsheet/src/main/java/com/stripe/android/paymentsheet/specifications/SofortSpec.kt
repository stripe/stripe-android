package com.stripe.android.paymentsheet.specifications

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R

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
            FormItemSpec.SectionSpec(
                IdentifierSpec("name"),
                FormItemSpec.SectionSpec.SectionFieldSpec.Name
            ),
            FormItemSpec.SectionSpec(
                IdentifierSpec("email"),
                FormItemSpec.SectionSpec.SectionFieldSpec.Email
            ),
            FormItemSpec.SectionSpec(
                IdentifierSpec("country"),
                FormItemSpec.SectionSpec.SectionFieldSpec.Country
            ),
            FormItemSpec.StaticTextSpec(
                IdentifierSpec("mandate"),
                R.string.sofort_mandate,
                Color.Gray
            )
        )
    ),
    sofortParamKey,
)

