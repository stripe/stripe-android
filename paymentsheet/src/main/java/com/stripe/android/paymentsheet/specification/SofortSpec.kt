package com.stripe.android.paymentsheet.specification

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.FormElementSpec.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.specifications.FormElementSpec.SectionSpec
import com.stripe.android.paymentsheet.specifications.FormElementSpec.SectionSpec.SectionFieldSpec.Country
import com.stripe.android.paymentsheet.specifications.FormElementSpec.SectionSpec.SectionFieldSpec.Email
import com.stripe.android.paymentsheet.specifications.FormElementSpec.SectionSpec.SectionFieldSpec.Name
import com.stripe.android.paymentsheet.specifications.FormElementSpec.StaticTextSpec
import com.stripe.android.paymentsheet.specifications.FormSpec
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import com.stripe.android.paymentsheet.specifications.billingParams

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
internal val mandate = StaticTextSpec(IdentifierSpec("mandate"), R.string.sofort_mandate, Color.Gray)
val sofort = FormSpec(
    LayoutSpec(
        listOf(
            nameSection,
            emailSection,
            countrySection,
            mandate,
            SaveForFutureUseSpec(listOf(nameSection, emailSection, mandate))
        )
    ),
    sofortParamKey,
)

