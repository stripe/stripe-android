package com.stripe.android.paymentsheet.specifications

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.FormItemSpec.MandateTextSpec
import com.stripe.android.paymentsheet.specifications.FormItemSpec.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.specifications.FormItemSpec.SectionSpec
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Email
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Item

internal val idealParams: MutableMap<String, Any?> = mutableMapOf(
    "bank" to null,
)

internal val idealParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "ideal",
    "billing_details" to billingParams,
    "ideal" to idealParams
)

internal val idealNameSection = SectionSpec(
    IdentifierSpec("name section"),
    SectionFieldSpec.NAME
)
internal val idealEmailSection = SectionSpec(IdentifierSpec("email"), Email)
internal val idealBankSection = SectionSpec(
    IdentifierSpec("bank"),
    SectionFieldSpec.SimpleDropdown(
        IdentifierSpec("bank"),
        R.string.stripe_paymentsheet_ideal_bank,
        listOf(
            Item("ABN AMRO", "abn_amro"),
            Item("ASN Bank", "asn_bank"),
            Item("Bunq", "bunq"),
            Item("Handelsbanken", "handelsbanken"),
            Item("ING", "ing"),
            Item("Knab", "knab"),
            Item("Rabobank", "rabobank"),
            Item("Revolut", "revolut"),
            Item("RegioBank", "regiobank"),
            Item("SNS Bank (De Volksbank)", "sns_bank"),
            Item("Triodos Bank", "triodos_bank"),
            Item("Van Lanschot", "van_lanschot"),
        )
    )
)
internal val idealMandate = MandateTextSpec(
    IdentifierSpec("mandate"),
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
