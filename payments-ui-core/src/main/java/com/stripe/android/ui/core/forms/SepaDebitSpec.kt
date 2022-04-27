package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IbanSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.supportedBillingCountries

internal val sepaDebitIbanSection = SectionSpec(
    IdentifierSpec.Generic("iban_section"),
    IbanSpec
)
internal val sepaDebitMandate = MandateTextSpec(
    IdentifierSpec.Generic("mandate"),
    R.string.sepa_mandate
)
internal val sepaBillingSection = SectionSpec(
    IdentifierSpec.Generic("billing_section"),
    AddressSpec(
        IdentifierSpec.Generic("address"),
        countryCodes = supportedBillingCountries
    ),
    R.string.billing_details
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val SepaDebitForm = LayoutSpec.create(
    NameSpec(),
    EmailSpec(),
    sepaDebitIbanSection,
    sepaBillingSection,
    sepaDebitMandate,
)
