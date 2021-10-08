package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.AddressSpec
import com.stripe.android.paymentsheet.elements.AfterpayClearpayTextSpec
import com.stripe.android.paymentsheet.elements.EmailSpec
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.SimpleTextSpec
import com.stripe.android.paymentsheet.elements.billingParams

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

internal val afterpayClearpayForm = LayoutSpec.create(
    afterpayClearpayHeader,
    afterpayClearpayNameSection,
    afterpayClearpayEmailSection,
    afterpayClearpayBillingSection
)
