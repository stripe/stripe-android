package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AuBankAccountNumberSpec
import com.stripe.android.ui.core.elements.AuBecsDebitMandateTextSpec
import com.stripe.android.ui.core.elements.BsbSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.billingParams

internal val AuBecsDebitParams: MutableMap<String, Any?> = mutableMapOf(
    "bsb_number" to null,
    "account_number" to null
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val AuBecsDebitParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "au_becs_debit",
    "au_becs_debit" to AuBecsDebitParams,
    "billing_details" to billingParams
)

internal val auBecsDebitNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)

internal val auBecsDebitEmailSection = SectionSpec(
    IdentifierSpec.Generic("email_section"),
    EmailSpec
)

internal val auBecsBsbNumberSection = SectionSpec(
    IdentifierSpec.Generic("bsb_number_section"),
    BsbSpec
)

internal val auBecsDebitAccountNumberSection = SectionSpec(
    IdentifierSpec.Generic("account_number"),
    AuBankAccountNumberSpec
)

internal val auBecsDebitCustomMandate = AuBecsDebitMandateTextSpec(
    IdentifierSpec.Generic("au_becs_mandate"),
    R.color.mandate_text_color
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val AuBecsDebitForm = LayoutSpec.create(
    auBecsDebitEmailSection,
    auBecsBsbNumberSection,
    auBecsDebitAccountNumberSection,
    auBecsDebitNameSection,
    auBecsDebitCustomMandate
)
