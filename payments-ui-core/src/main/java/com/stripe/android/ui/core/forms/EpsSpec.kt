package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BankDropdownSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.SupportedBankType

internal val epsNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    NameSpec
)
internal val epsBankSection =
    SectionSpec(
        IdentifierSpec.Generic("bank_section"),
        BankDropdownSpec(
            IdentifierSpec.Generic("eps[bank]"),
            R.string.eps_bank,
            SupportedBankType.Eps
        )
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val EpsForm = LayoutSpec.create(
    epsNameSection,
    epsBankSection
)
