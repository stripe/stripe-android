package com.stripe.android.paymentsheet.forms

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.AddressSpec
import com.stripe.android.paymentsheet.elements.EmailSpec
import com.stripe.android.paymentsheet.elements.IbanSpec
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.MandateTextSpec
import com.stripe.android.paymentsheet.elements.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.SimpleTextSpec
import com.stripe.android.paymentsheet.elements.billingParams

internal val sepaDebitParams: MutableMap<String, Any?> = mutableMapOf(
    "iban" to null
)

internal val sepaDebitParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "sepa_debit",
    "billing_details" to billingParams,
    "sepa_debit" to sepaDebitParams
)

internal val sepaDebitNameSection = SectionSpec(
    IdentifierSpec.Generic("name _ection"),
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
    R.string.stripe_paymentsheet_sepa_mandate,
    Color.Gray
)
internal val sepaBillingSection = SectionSpec(
    IdentifierSpec.Generic("billing_section"),
    AddressSpec(IdentifierSpec.Generic("address")),
    R.string.billing_details
)

internal val sepaDebitForm = LayoutSpec.create(
    sepaDebitNameSection,
    sepaDebitEmailSection,
    sepaDebitIbanSection,
    sepaBillingSection,
    SaveForFutureUseSpec(emptyList()),
    sepaDebitMandate,
)
