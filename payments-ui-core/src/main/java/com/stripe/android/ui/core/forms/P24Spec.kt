package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BankDropdownSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.SupportedBankType
import com.stripe.android.ui.core.elements.billingParams

internal val p24Params: MutableMap<String, Any?> = mutableMapOf(
    "bank" to null,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val P24ParamKey: MutableMap<String, Any?> = mutableMapOf(
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
            R.string.p24_bank,
            SupportedBankType.P24
        )
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val P24Form = LayoutSpec.create(
    p24NameSection,
    p24EmailSection,
    p24BankSection
)
