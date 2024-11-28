package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.uicore.elements.IdentifierSpec

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LinkCardForm = LayoutSpec(
    listOf(
        CardDetailsSectionSpec(IdentifierSpec.Generic("card_details_section")),
        CardBillingSpec(allowedCountryCodes = CountryUtils.supportedBillingCountries)
    )
)
