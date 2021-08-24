package com.stripe.android.paymentsheet.specifications

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.FormItemSpec.MandateTextSpec
import com.stripe.android.paymentsheet.specifications.FormItemSpec.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.specifications.FormItemSpec.SectionSpec
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Email

internal val idealParams: MutableMap<String, Any?> = mutableMapOf(
    "bank" to null,
)

internal val idealParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "ideal",
    "billing_details" to billingParams,
    "ideal" to idealParams
)

internal val idealNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SectionFieldSpec.NAME
)
internal val idealEmailSection = SectionSpec(IdentifierSpec.Email, Email)
internal val idealBankSection = SectionSpec(
    IdentifierSpec.Generic("bank_section"),
    SectionFieldSpec.BankDropdown(
        IdentifierSpec.Generic("bank"),
        R.string.stripe_paymentsheet_ideal_bank,
        SupportedBankType.Ideal
    )
)
internal val idealMandate = MandateTextSpec(
    IdentifierSpec.Generic("mandate"),
    R.string.stripe_paymentsheet_sepa_mandate,
    Color.Gray
)
internal val ideal = FormSpec(
    LayoutSpec(
        listOf(
            idealNameSection,
            idealEmailSection,
            idealBankSection,
            SaveForFutureUseSpec(listOf(idealEmailSection, idealMandate)),
            idealMandate,
        )
    ),
    idealParamKey,
)
