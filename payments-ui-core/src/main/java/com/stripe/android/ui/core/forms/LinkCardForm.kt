package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.supportedBillingCountries

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LinkCardForm: List<FormItemSpec> = listOf(
    CardDetailsSectionSpec(IdentifierSpec.Generic("card_details_section")),
    CardBillingSpec(validCountryCodes = supportedBillingCountries)
)
