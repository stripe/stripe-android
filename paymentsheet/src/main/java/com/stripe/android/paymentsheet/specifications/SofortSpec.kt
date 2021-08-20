package com.stripe.android.paymentsheet.specifications

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.FormItemSpec.MandateTextSpec
import com.stripe.android.paymentsheet.specifications.FormItemSpec.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.specifications.FormItemSpec.SectionSpec
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Country
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Email

internal val sofortParams: MutableMap<String, Any?> = mutableMapOf(
    "country" to null,
)

internal val sofortParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "sofort",
    "billing_details" to billingParams,
    "sofort" to sofortParams
)

internal val sofortNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SectionFieldSpec.NAME
)
internal val sofortEmailSection = SectionSpec(IdentifierSpec.Generic("email_section"), Email)
internal val sofortCountrySection =
    SectionSpec(
        IdentifierSpec.Generic("country_section"),
        Country(setOf("AT", "BE", "DE", "ES", "IT", "NL"))
    )
internal val sofortMandate = MandateTextSpec(
    IdentifierSpec.Generic("mandate"),
    R.string.stripe_paymentsheet_sepa_mandate,
    Color.Gray
)
internal val sofort = FormSpec(
    LayoutSpec(
        listOf(
            sofortNameSection,
            sofortEmailSection,
            sofortCountrySection,
            SaveForFutureUseSpec(listOf(sofortNameSection, sofortEmailSection, sofortMandate)),
            sofortMandate,
        )
    ),
    sofortParamKey,
)
