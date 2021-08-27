package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.R

internal val afterpayClearpayParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "afterpay_clearpay",
    "billing_details" to billingParams
)

internal val afterpayClearpayHeader = AfterpayClearpayTextSpec(
    IdentifierSpec.Generic("afterpay_clearpay_header")
)
internal val afterpayClearpayNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)
internal val afterpayClearpayEmailSection =
    SectionSpec(IdentifierSpec.Generic("email_section"), EmailSpec)

internal val afterpayClearpayBillingSection = SectionSpec(
    IdentifierSpec.Generic("address_section"),
    AddressSpec(IdentifierSpec.Generic("address")),
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
