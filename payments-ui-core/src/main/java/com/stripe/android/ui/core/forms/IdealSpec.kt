package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BankDropdownSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.SupportedBankType

internal val idealNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)
internal val idealEmailSection = SectionSpec(IdentifierSpec.Email, EmailSpec)
internal val idealBankSection = SectionSpec(
    IdentifierSpec.Generic("bank_section"),
    BankDropdownSpec(
        IdentifierSpec.Generic("ideal[bank]"),
        R.string.ideal_bank,
        SupportedBankType.Ideal
    )
)
internal val idealMandate = MandateTextSpec(
    IdentifierSpec.Generic("mandate"),
    R.string.sepa_mandate
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val IdealForm = LayoutSpec.create(
    idealNameSection,
    idealEmailSection,
    idealBankSection,
    SaveForFutureUseSpec(listOf(idealEmailSection, idealMandate)),
    idealMandate,
)
