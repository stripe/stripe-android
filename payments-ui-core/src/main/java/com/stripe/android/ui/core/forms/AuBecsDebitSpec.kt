package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AuBankAccountNumberSpec
import com.stripe.android.ui.core.elements.AuBecsDebitMandateTextSpec
import com.stripe.android.ui.core.elements.BsbSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val AuBecsDebitForm = LayoutSpec.create(
    EmailSpec(),
    BsbSpec(),
    AuBankAccountNumberSpec(),
    SimpleTextSpec(
        IdentifierSpec.Name,
        label = R.string.au_becs_account_name,
        capitalization = KeyboardCapitalization.Words,
        keyboardType = KeyboardType.Text
    ),
    AuBecsDebitMandateTextSpec()
)
