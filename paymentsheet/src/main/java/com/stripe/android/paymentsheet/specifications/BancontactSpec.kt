package com.stripe.android.paymentsheet.specifications

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.FormItemSpec.SaveForFutureUseSpec

internal val bancontactParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "bancontact",
    "billing_details" to billingParams
)

internal val bancontactNameSection = FormItemSpec.SectionSpec(
    IdentifierSpec("name section"),
    SectionFieldSpec.NAME
)
internal val bancontactEmailSection =
    FormItemSpec.SectionSpec(IdentifierSpec("email"), SectionFieldSpec.Email)
internal val bancontactMandate = FormItemSpec.MandateTextSpec(
    IdentifierSpec("mandate"),
    R.string.stripe_paymentsheet_sepa_mandate,
    Color.Gray
)
internal val bancontact = FormSpec(
    LayoutSpec(
        listOf(
            bancontactNameSection,
            bancontactEmailSection,
            SaveForFutureUseSpec(
                listOf(
                    bancontactEmailSection, bancontactMandate
                )
            ),
            bancontactMandate,
            FormItemSpec.SectionSpec(
                IdentifierSpec("billing_section"),
                SectionFieldSpec.AddressSpec(IdentifierSpec("address_element")),
                R.string.billing_details
            )
        )
    ),
    bancontactParamKey,
)
