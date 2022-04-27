package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BankDropdownSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.SupportedBankType

internal val idealNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)
internal val idealBankSection = SectionSpec(
    IdentifierSpec.Generic("bank_section"),
    BankDropdownSpec(
        IdentifierSpec.Generic("ideal[bank]"),
        R.string.ideal_bank,
        SupportedBankType.Ideal
    )
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val IdealForm = LayoutSpec.create(
    idealNameSection,
    idealBankSection,
)
