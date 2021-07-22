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
    IdentifierSpec("name section"),
    SectionFieldSpec.NAME
)
internal val sepaDebitEmailSection = SectionSpec(IdentifierSpec("email"), Email)
internal val sepaDebitIbanSection = SectionSpec(IdentifierSpec("iban"), Iban)
internal val sepaDebitMandate = MandateTextSpec(
    IdentifierSpec("mandate"),
    R.string.stripe_paymentsheet_sepa_mandate,
    Color.Gray
)

internal val sepaDebit = FormSpec(
    LayoutSpec(
        listOf(
            sepaDebitNameSection,
            sepaDebitEmailSection,
            sepaDebitIbanSection,
            FormItemSpec.SaveForFutureUseSpec(listOf(sepaDebitMandate)),
            sepaDebitMandate
        )
    ),
    sepaDebitParamKey
)
