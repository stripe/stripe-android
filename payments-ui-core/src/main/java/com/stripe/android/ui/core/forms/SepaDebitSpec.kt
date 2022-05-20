package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.IbanSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.supportedBillingCountries

internal val sepaDebitMandate = MandateTextSpec(
    IdentifierSpec.Generic("mandate"),
    R.string.sepa_mandate
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val SepaDebitForm: List<FormItemSpec> = listOf(
    NameSpec(),
    EmailSpec(),
    IbanSpec(),
    AddressSpec(
        IdentifierSpec.Generic("address"),
        countryCodes = supportedBillingCountries
    ),
    sepaDebitMandate,
)
