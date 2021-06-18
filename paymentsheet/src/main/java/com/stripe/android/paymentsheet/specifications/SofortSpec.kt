package com.stripe.android.paymentsheet.specifications

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.FormElementSpec.SectionSpec
import com.stripe.android.paymentsheet.specifications.FormElementSpec.SectionSpec.SectionFieldSpec.Country
import com.stripe.android.paymentsheet.specifications.FormElementSpec.SectionSpec.SectionFieldSpec.Email
import com.stripe.android.paymentsheet.specifications.FormElementSpec.SectionSpec.SectionFieldSpec.Name

internal val sofortParams: MutableMap<String, Any?> = mutableMapOf(
    "country" to null,
)

internal val sofortParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "sofort",
    "billing_details" to billingParams,
    "sofort" to sofortParams
)

internal val nameSection = SectionSpec(IdentifierSpec("name"), Name)
internal val emailSection = SectionSpec(IdentifierSpec("email"), Email)
internal val countrySection = SectionSpec(IdentifierSpec("country"), Country)
internal val mandate = FormElementSpec.StaticTextSpec(
    IdentifierSpec("mandate"),
    R.string.sofort_mandate,
    Color.Gray
)
val sofort = FormSpec(
    LayoutSpec(
        listOf(
            nameSection,
            emailSection,
            countrySection,
            mandate,
            FormElementSpec.SaveForFutureUseSpec(listOf(nameSection, emailSection, mandate))
        )
    ),
    com.stripe.android.paymentsheet.specification.sofortParamKey,
)

