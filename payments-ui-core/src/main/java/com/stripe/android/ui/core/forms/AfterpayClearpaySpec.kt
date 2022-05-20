package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AfterpayClearpayTextSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.supportedBillingCountries

internal val afterpayClearpayHeader = AfterpayClearpayTextSpec(
    IdentifierSpec.Generic("afterpay_clearpay_header")
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val AfterpayClearpayForm = LayoutSpec.create(
    afterpayClearpayHeader,
    NameSpec(),
    EmailSpec(),
    AddressSpec(
        IdentifierSpec.Generic("address"),
        supportedBillingCountries
    )
)
