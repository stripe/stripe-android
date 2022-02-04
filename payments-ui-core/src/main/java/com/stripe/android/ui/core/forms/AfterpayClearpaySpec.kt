package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AfterpayClearpayTextSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.billingParams

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val AfterpayClearpayParamKey: MutableMap<String, Any?> = mutableMapOf(
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val AfterpayClearpayForm = LayoutSpec.create(
    afterpayClearpayHeader,
    afterpayClearpayNameSection,
    afterpayClearpayEmailSection,
    afterpayClearpayBillingSection
)
