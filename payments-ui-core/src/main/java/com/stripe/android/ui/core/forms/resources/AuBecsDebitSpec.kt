package com.stripe.android.ui.core.forms.resources

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AccountNumberSpec
import com.stripe.android.ui.core.elements.BsbSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IbanSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.KlarnaHelper
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.StaticTextSpec

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val AuBecsDebitParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "au_becs_debit"
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

internal val auBecsDebitSection = SectionSpec(
    IdentifierSpec.Generic("account_number"),
    AccountNumberSpec
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val AuBecsDebitForm = LayoutSpec.create(
    auBecsDebitEmailSection,
    auBecsDebitNameSection,
    auBecsBsbNumberSection,
    auBecsDebitSection
)