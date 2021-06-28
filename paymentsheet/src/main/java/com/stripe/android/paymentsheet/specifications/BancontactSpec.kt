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
            FormItemSpec.SectionSpec(
                IdentifierSpec("name"),
                SectionFieldSpec.Name
            ),
            FormItemSpec.SectionSpec(
                IdentifierSpec("email"),
                SectionFieldSpec.Email
            ),
            FormItemSpec.StaticTextSpec(
                IdentifierSpec("mandate"),
                R.string.sofort_mandate,
                Color.Gray
            )
        )
    ),
    bancontactParamKey,
)
