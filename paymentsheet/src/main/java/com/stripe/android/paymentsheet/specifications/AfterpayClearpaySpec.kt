package com.stripe.android.paymentsheet.specifications

import com.stripe.android.paymentsheet.R

internal val afterpayClearpayParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "afterpay_clearpay",
    "billing_details" to billingParams
)

internal val afterpayClearpayHeader = FormItemSpec.AfterpayClearpayTextSpec(
    IdentifierSpec.Generic("afterpay_clearpay_header")
)
internal val afterpayClearpayNameSection = FormItemSpec.SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SectionFieldSpec.NAME
)
internal val afterpayClearpayEmailSection =
    FormItemSpec.SectionSpec(IdentifierSpec.Generic("email_section"), SectionFieldSpec.Email)

internal val afterpayClearpayBillingSection = FormItemSpec.SectionSpec(
    IdentifierSpec.Generic("address_section"),
    SectionFieldSpec.AddressSpec(IdentifierSpec.Generic("address")),
    R.string.billing_details
)

internal val afterpayClearpay = FormSpec(
    LayoutSpec(
        listOf(
            afterpayClearpayHeader,
            afterpayClearpayNameSection,
            afterpayClearpayEmailSection,
            afterpayClearpayBillingSection
        )
    ),
    afterpayClearpayParamKey
)
