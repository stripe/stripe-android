package com.stripe.android.paymentsheet.specifications

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.FormItemSpec.MandateTextSpec
import com.stripe.android.paymentsheet.specifications.FormItemSpec.SectionSpec
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Email
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Iban

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
    SectionFieldSpec.NAME
)
internal val sepaDebitEmailSection = SectionSpec(
    IdentifierSpec.Generic("email_section"),
    Email
)
internal val sepaDebitIbanSection = SectionSpec(
    IdentifierSpec.Generic("iban_section"),
    Iban
)
internal val sepaDebitMandate = MandateTextSpec(
    IdentifierSpec.Generic("mandate"),
    R.string.stripe_paymentsheet_sepa_mandate,
    Color.Gray
)
internal val sepaBillingSection = SectionSpec(
    IdentifierSpec.Generic("billing_section"),
    SectionFieldSpec.AddressSpec(IdentifierSpec.Generic("address")),
    R.string.billing_details
)
internal val sepaDebit = FormSpec(
    LayoutSpec(
        listOf(
            sepaDebitNameSection,
            sepaDebitEmailSection,
            sepaDebitIbanSection,
            FormItemSpec.SaveForFutureUseSpec(listOf(sepaDebitMandate)),
            sepaDebitMandate,
            sepaBillingSection
        )
    ),
    sepaDebitParamKey
)
