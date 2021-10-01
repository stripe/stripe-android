package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.BankDropdownSpec
import com.stripe.android.paymentsheet.elements.EmailSpec
import com.stripe.android.paymentsheet.elements.FormRequirement
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.PaymentMethodFormSpec
import com.stripe.android.paymentsheet.elements.SaveMode
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.SimpleTextSpec
import com.stripe.android.paymentsheet.elements.SupportedBankType
import com.stripe.android.paymentsheet.elements.billingParams

internal val p24Params: MutableMap<String, Any?> = mutableMapOf(
    "bank" to null,
)

internal val p24ParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "p24",
    "billing_details" to billingParams,
    "p24" to p24Params
)

internal val p24NameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)
internal val p24EmailSection = SectionSpec(
    IdentifierSpec.Generic("email_section"),
    EmailSpec
)
internal val p24BankSection =
    SectionSpec(
        IdentifierSpec.Generic("bank_section"),
        BankDropdownSpec(
            IdentifierSpec.Generic("bank"),
            R.string.stripe_paymentsheet_p24_bank,
            SupportedBankType.P24
        )
    )

internal val p24 = PaymentMethodFormSpec(
    p24ParamKey,
    mapOf(
        FormRequirement(
            SaveMode.PaymentIntentAndSetupFutureUsageNotSet,
            requirements = emptySet()
        ) to LayoutSpec.create(
            p24NameSection,
            p24EmailSection,
            p24BankSection
        )
    )
)
