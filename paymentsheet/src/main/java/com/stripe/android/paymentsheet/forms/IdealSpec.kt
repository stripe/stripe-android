package com.stripe.android.paymentsheet.forms

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.BankDropdownSpec
import com.stripe.android.paymentsheet.elements.EmailSpec
import com.stripe.android.paymentsheet.elements.FormSpec
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.MandateTextSpec
import com.stripe.android.paymentsheet.elements.PaymentMethodSpec
import com.stripe.android.paymentsheet.elements.Requirement
import com.stripe.android.paymentsheet.elements.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.SimpleTextSpec
import com.stripe.android.paymentsheet.elements.SupportedBankType
import com.stripe.android.paymentsheet.elements.billingParams

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
    SimpleTextSpec.NAME
)
internal val idealEmailSection = SectionSpec(IdentifierSpec.Email, EmailSpec)
internal val idealBankSection = SectionSpec(
    IdentifierSpec.Generic("bank_section"),
    BankDropdownSpec(
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

private val idealUserSelectedSave = FormSpec(
    LayoutSpec(
        listOf(
            idealNameSection,
            idealEmailSection,
            idealBankSection,
            SaveForFutureUseSpec(listOf(idealEmailSection, idealMandate)),
            idealMandate,
        )
    ),
    // When saved this is a SEPA paymentMethod which requires SEPA requirements
    requirements = setOf(Requirement.UserSelectableSave)
        .plus(sepaDebitUserSelectedSave.requirements)
)
private val idealMerchantRequiredSave = FormSpec(
    LayoutSpec(
        listOf(
            idealNameSection,
            idealEmailSection,
            idealBankSection,
            idealMandate,
        )
    ),
    // When saved this is a SEPA paymentMethod which requires SEPA requirements
    requirements = setOf(Requirement.MerchantSelectedSave)
        .plus(sepaDebitMerchantRequiredSave.requirements)
)

private val idealOneTimeUse = FormSpec(
    LayoutSpec(
        listOf(
            idealNameSection,
            idealBankSection,
        )
    ),
    requirements = setOf(Requirement.OneTimeUse),
)

internal val ideal = PaymentMethodSpec(
    idealParamKey,
    listOf(
        idealUserSelectedSave,
        idealMerchantRequiredSave,
        idealOneTimeUse
    )
)

