package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AfterpayClearpayTextSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.supportedBillingCountries

internal val afterpayClearpayHeader = AfterpayClearpayTextSpec(
    IdentifierSpec.Generic("afterpay_clearpay_header")
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val AfterpayClearpayForm: List<FormItemSpec> = listOf(
    afterpayClearpayHeader,
    NameSpec(),
    EmailSpec(),
    AddressSpec(
        IdentifierSpec.Generic("address"),
        supportedBillingCountries
    )
)
