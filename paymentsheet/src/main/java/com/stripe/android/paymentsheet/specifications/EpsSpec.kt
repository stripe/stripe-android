package com.stripe.android.paymentsheet.specifications

import com.stripe.android.paymentsheet.R

internal val epsParams: MutableMap<String, Any?> = mutableMapOf(
    "bank" to null,
)

internal val epsParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "eps",
    "billing_details" to billingParams,
    "eps" to epsParams
)

internal val epsNameSection = FormItemSpec.SectionSpec(
    IdentifierSpec.Name,
    SectionFieldSpec.NAME
)
internal val epsBankSection =
    FormItemSpec.SectionSpec(
        IdentifierSpec.Generic("bank section"),
        SectionFieldSpec.BankDropdown(
            IdentifierSpec.Generic("bank"),
            R.string.stripe_paymentsheet_eps_bank,
            SupportedBankType.Eps
        )
    )

internal val eps = FormSpec(
    LayoutSpec(
        listOf(
            epsNameSection,
            epsBankSection
        )
    ),
    epsParamKey,
)
