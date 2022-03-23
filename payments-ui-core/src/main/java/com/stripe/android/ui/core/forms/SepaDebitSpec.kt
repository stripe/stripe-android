package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IbanSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.billingParams

internal val sepaDebitParams: MutableMap<String, Any?> = mutableMapOf(
    "iban" to null
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val SepaDebitParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "sepa_debit",
    "billing_details" to billingParams,
    "sepa_debit" to sepaDebitParams
)

internal val sepaDebitNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)
internal val sepaDebitEmailSection = SectionSpec(
    IdentifierSpec.Generic("email_section"),
    EmailSpec
)
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
    AddressSpec(IdentifierSpec.Generic("address")),
    R.string.billing_details
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val SepaDebitForm = LayoutSpec.create(
    sepaDebitNameSection,
    sepaDebitEmailSection,
    sepaDebitIbanSection,
    sepaBillingSection,
    SaveForFutureUseSpec(emptyList()),
    sepaDebitMandate,
)
