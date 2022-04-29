package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BankDropdownSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.SupportedBankType

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val IdealForm = LayoutSpec.create(
    NameSpec(),
    BankDropdownSpec(
        IdentifierSpec.Generic("ideal[bank]"),
        R.string.ideal_bank,
        SupportedBankType.Ideal
    ),
)
