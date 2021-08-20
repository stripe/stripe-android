package com.stripe.android.paymentsheet.specifications

import com.stripe.android.paymentsheet.R

internal val p24Params: MutableMap<String, Any?> = mutableMapOf(
    "bank" to null,
)

internal val p24ParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "p24",
    "billing_details" to billingParams,
    "p24" to p24Params
)

internal val p24NameSection = FormItemSpec.SectionSpec(
    IdentifierSpec.Generic("name section"),
    SectionFieldSpec.NAME
)
internal val p24EmailSection = FormItemSpec.SectionSpec(
    IdentifierSpec.Generic("email"),
    SectionFieldSpec.Email
)
internal val p24BankSection =
    FormItemSpec.SectionSpec(
        IdentifierSpec.Generic("bank section"),
        SectionFieldSpec.BankDropdown(
            IdentifierSpec.Generic("bank"),
            R.string.stripe_paymentsheet_p24_bank,
            SupportedBankType.P24
        )
    )

internal val p24 = FormSpec(
    LayoutSpec(
        listOf(
            p24NameSection,
            p24EmailSection,
            p24BankSection
        )
    ),
    p24ParamKey,
)
