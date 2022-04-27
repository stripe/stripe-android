package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.supportedBillingCountries

internal val creditBillingSection = SectionSpec(
    IdentifierSpec.Generic("credit_billing_section"),
    CardBillingSpec(
        countryCodes = supportedBillingCountries
    ),
    R.string.billing_details
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val CardForm = LayoutSpec.create(
    CardDetailsSectionSpec(IdentifierSpec.Generic("credit_details_section")),
    creditBillingSection,
    SaveForFutureUseSpec(emptyList())
)
