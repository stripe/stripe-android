package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.supportedBillingCountries

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LinkCardForm = LayoutSpec(
    listOf(
        CardDetailsSectionSpec(IdentifierSpec.Generic("card_details_section")),
        CardBillingSpec(validCountryCodes = supportedBillingCountries)
    )
)
