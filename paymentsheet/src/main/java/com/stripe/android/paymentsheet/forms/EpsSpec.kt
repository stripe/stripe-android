package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.BankDropdownSpec
import com.stripe.android.paymentsheet.elements.FormRequirement
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.PaymentMethodFormSpec
import com.stripe.android.paymentsheet.elements.SaveMode
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.SimpleTextSpec
import com.stripe.android.paymentsheet.elements.SupportedBankType
import com.stripe.android.paymentsheet.elements.billingParams

internal val epsParams: MutableMap<String, Any?> = mutableMapOf(
    "bank" to null,
)

internal val epsParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "eps",
    "billing_details" to billingParams,
    "eps" to epsParams
)

internal val epsNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)
internal val epsBankSection =
    SectionSpec(
        IdentifierSpec.Generic("bank_section"),
        BankDropdownSpec(
            IdentifierSpec.Generic("bank"),
            R.string.stripe_paymentsheet_eps_bank,
            SupportedBankType.Eps
        )
    )

internal val eps = PaymentMethodFormSpec(
    epsParamKey,
    mapOf(
        FormRequirement(
            SaveMode.PaymentIntentAndSetupFutureUsageNotSet,
            requirements = emptySet()
        ) to LayoutSpec.create(
            epsNameSection,
            epsBankSection
        )
    )
)
